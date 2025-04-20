package org.binqua.examples.http4sapp.app

import io.circe.generic.semiauto.deriveEncoder
import io.circe.{Encoder, Json}
import org.scalatest.events.Ordinal

import java.io.File

object Step {
  implicit val featureEncoder: Encoder[Step] = Encoder.instance { step =>
    Json.obj(
      "message" -> Json.fromString(step.message),
      "timestamp" -> Json.fromLong(step.timestamp),
      "ordinal" -> Json.fromString(step.ordinal.toList.mkString("_")),
    )
  }
}

case class Step(ordinal: Ordinal, message: String, throwable: Option[Throwable], timestamp: Long)

object Scenario {
  implicit val ordinalEncoder: Encoder[Ordinal] = (ordinal: Ordinal) => Json.fromString(ordinal.toList.mkString("_"))
  implicit val encoder: Encoder[Scenario] = deriveEncoder[Scenario].mapJson(_.dropNullValues)

  def starting(ordinal: Ordinal, name: String, timestamp: Long): Scenario =
    Scenario(
      ordinal = ordinal,
      description = name,
      startedTimestamp = timestamp,
      finishedTimestamp = None,
      screenshots = None,
      steps = None,
      testOutcome = TestOutcome.STARTING
    )

  def addStep(scenario: Scenario, ordinal: Ordinal, message: String, throwable: Option[Throwable], timestamp: Long): Scenario = {
    def newStep = Step(ordinal, message, throwable, timestamp)
    scenario.copy(steps = scenario.steps.map(steps => newStep :: steps).orElse(Some(List(newStep))))
  }
}


case class Scenario(
    ordinal: Ordinal,
    description: String,
    startedTimestamp: Long,
    finishedTimestamp: Option[Long],
    screenshots: Option[List[Screenshot]],
    steps: Option[List[Step]],
    testOutcome: TestOutcome
) {

  def withNewScreenshot(pageUrl: String, screenshotMoment: ScreenshotMoment): (Scenario, File) = {
    val maybeScreenshots: Option[List[Screenshot]] = screenshots
      .map(s => Screenshot(pageUrl, screenshotMoment, ordinal, s.size + 1) :: s)
      .orElse(Some(List(Screenshot(pageUrl, screenshotMoment, ordinal, 1))))
    (this.copy(screenshots = maybeScreenshots), maybeScreenshots.get.head.toFile)
  }
}
