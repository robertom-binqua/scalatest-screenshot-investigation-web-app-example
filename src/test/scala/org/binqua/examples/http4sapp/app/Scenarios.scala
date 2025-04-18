package org.binqua.examples.http4sapp.app

import cats.implicits.catsSyntaxEitherId
import io.circe.Encoder
import io.circe.syntax.EncoderOps
import org.scalatest.events.Ordinal

import java.io.File

object Scenarios {
  implicit val encoder: Encoder[Scenarios] = Encoder.instance { scenario =>
    scenario.scenarios.values.asJson
  }
}
case class Scenarios(scenarios: Map[String, Scenario]) {
  def testUpdate(scenarioDescription: String, timestamp: Long, newState: TestOutcome): Either[String, Scenarios] =
    scenarios
      .get(scenarioDescription)
      .toRight(s"no scenario with description $scenarioDescription")
      .flatMap(scenarioFound =>
        if (scenarioFound.testOutcome != TestOutcome.STARTING)
          s"lastScenario testOutcome is ${scenarioFound.testOutcome} and should be ${TestOutcome.STARTING}".asLeft
        else
          this.copy(scenarios = scenarios.updated(scenarioDescription, scenarioFound.copy(finishedTimestamp = Some(timestamp), testOutcome = newState))).asRight
      )

  def testStarting(ordinal: Ordinal, scenarioDescription: String, timestamp: Long): Either[String, Scenarios] =
    scenarios.get(scenarioDescription) match {
      case Some(scenarioAlreadyPresent) =>
        s"scenarioAlreadyPresent bro $scenarioAlreadyPresent, I cannot start it".asLeft
      case None =>
        this.copy(scenarios = this.scenarios.updated(scenarioDescription, Scenario.starting(ordinal, scenarioDescription, timestamp))).asRight
    }

  def withNewScreenshot(ordinal: Ordinal, scenarioDescription: String, pageUrl: String, screenshotMoment: ScreenshotMoment): Either[String, (Scenarios, File)] =
    scenarios
      .get(scenarioDescription)
      .toRight("last scenario does not have testOutcome == STARTING")
      .flatMap(lastScenario =>
        if (lastScenario.testOutcome == TestOutcome.STARTING && lastScenario.ordinal == ordinal) {
          val (updatedScenario, screenshotFile) = lastScenario.withNewScreenshot(pageUrl, screenshotMoment)
          val scenarios = this.copy(scenarios = this.scenarios.updated(scenarioDescription, updatedScenario))
          (scenarios, screenshotFile).asRight
        } else
          s"Sorry last scenario does not have testOutcome equal to STARTING but ${lastScenario.testOutcome}".asLeft
      )

}
