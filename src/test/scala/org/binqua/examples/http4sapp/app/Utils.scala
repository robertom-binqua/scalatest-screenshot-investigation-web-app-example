package org.binqua.examples.http4sapp.app

import cats.implicits.catsSyntaxEitherId

import scala.util.matching.Regex

object Utils {

  private val FeatureScenarioPattern: Regex = """Feature:\s(.*)\sScenario:\s(.*)""".r

  def toFeatureAndScenario(suiteClassName: String): Either[String, (String, String)] =
    suiteClassName match {
      case FeatureScenarioPattern(feature, scenario) => (feature, scenario).asRight
      case _                                         => s"could not find pattern $FeatureScenarioPattern in $suiteClassName".asLeft
    }

}
