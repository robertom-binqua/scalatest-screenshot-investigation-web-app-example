package org.binqua.examples.http4sapp.app

import cats.implicits.catsSyntaxEitherId
import io.circe.Encoder
import io.circe.syntax.EncoderOps
import org.binqua.examples.http4sapp.app.StateEvent.RecordedEvents
import org.scalatest.events.Ordinal

import java.io.File

object Scenarios {
  implicit val encoder: Encoder[Scenarios] = Encoder.instance { scenario =>
    scenario.scenariosMap.values.asJson
  }
  def withNewStep(
      destination: Scenarios,
      scenarioDescription: String,
      ordinal: Ordinal,
      message: String,
      throwable: Option[Throwable],
      timestamp: Long
  ): Either[String, Scenarios] =
    for {
      scenarioFound <- destination.scenariosMap
        .get(scenarioDescription)
        .toRight(s"no scenario with description $scenarioDescription")
      scenarioWithNewStep <- Scenario.addAStep(scenarioFound, ordinal, message, throwable, timestamp)
    } yield destination.copy(scenariosMap = destination.scenariosMap.updated(scenarioDescription, scenarioWithNewStep))

}

case class Scenarios(scenariosMap: Map[String, Scenario]) {
  def testUpdate(
      scenarioDescription: String,
      timestamp: Long,
      recordedEvent: RecordedEvents,
      throwable: Option[Throwable],
      newState: TestOutcome
  ): Either[String, Scenarios] =
    scenariosMap
      .get(scenarioDescription)
      .toRight(s"no scenario with description $scenarioDescription")
      .flatMap((scenarioFound: Scenario) =>
        Scenario
          .update(scenarioFound, recordedEvent: RecordedEvents, newState: TestOutcome, throwable, timestamp)
          .map(scenarioUpdated => {
            this.copy(scenariosMap = scenariosMap.updated(scenarioDescription, scenarioUpdated))
          })
      )

  def testStarting(ordinal: Ordinal, scenarioDescription: String, timestamp: Long): Either[String, Scenarios] =
    scenariosMap.get(scenarioDescription) match {
      case Some(scenarioAlreadyPresent) =>
        s"scenarioAlreadyPresent bro $scenarioAlreadyPresent, I cannot start it".asLeft
      case None =>
        this.copy(scenariosMap = this.scenariosMap.updated(scenarioDescription, Scenario.starting(ordinal, scenarioDescription, timestamp))).asRight
    }

  def withNewScreenshot(ordinal: Ordinal, scenarioDescription: String, pageUrl: String, screenshotMoment: ScreenshotMoment): Either[String, (Scenarios, File)] =
    scenariosMap
      .get(scenarioDescription)
      .toRight("last scenario does not have testOutcome == STARTING")
      .flatMap(lastScenario =>
        if (lastScenario.testOutcome == TestOutcome.STARTING && lastScenario.ordinal == ordinal) {
          val (updatedScenario, screenshotFile) = lastScenario.withNewScreenshot(pageUrl, screenshotMoment)
          val scenarios = this.copy(scenariosMap = this.scenariosMap.updated(scenarioDescription, updatedScenario))
          (scenarios, screenshotFile).asRight
        } else
          s"Sorry last scenario does not have testOutcome equal to STARTING but ${lastScenario.testOutcome}".asLeft
      )

}
