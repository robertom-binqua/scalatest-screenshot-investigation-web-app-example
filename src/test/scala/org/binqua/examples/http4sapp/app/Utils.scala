package org.binqua.examples.http4sapp.app

import cats.implicits.catsSyntaxEitherId
import org.scalatest.events.Ordinal

import scala.util.matching.Regex

object Utils {

  private val FeatureScenarioPattern: Regex = """Feature:\s(.*)\sScenario:\s(.*)""".r

  def createARunningScenario(ordinal: Ordinal, testName: String, suiteClassName: String): Either[String, RunningScenario] =
    suiteClassName match {
      case FeatureScenarioPattern(feature, scenario) => RunningScenario(ordinal, testName, feature, scenario).asRight
      case _                                         => s"could not find pattern $FeatureScenarioPattern in $suiteClassName".asLeft
    }

}
