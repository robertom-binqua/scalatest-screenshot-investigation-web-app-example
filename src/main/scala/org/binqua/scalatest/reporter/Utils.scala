package org.binqua.scalatest.reporter

import cats.implicits.catsSyntaxEitherId
import org.scalatest.events.Ordinal

import scala.util.matching.Regex

object Utils:

  implicit class EitherOps[L, R](either: Either[L, R]) {
    def getOrThrow: R = {
      either match {
        case Right(value) => value
        case Left(error)  => throw new RuntimeException(s"Unexpected Left: $error")
      }
    }
  }

  private val FeatureScenarioPattern: Regex = """Feature:\s(.*)\sScenario:\s(.*)""".r

  def createARunningScenario(ordinal: Ordinal, testName: String, suiteClassName: String): Either[String, RunningScenario] =
    suiteClassName match {
      case FeatureScenarioPattern(feature, scenario) => RunningScenario(ordinal, testName, feature, scenario).asRight
      case _                                         => s"could not find pattern $FeatureScenarioPattern in $suiteClassName".asLeft
    }

  def ordinalToString(prefix: String, ordinal: Ordinal): String = s"${prefix}_${ordinal.toList.mkString("_")}"
