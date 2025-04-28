package org.binqua.examples.http4sapp.app

import cats.implicits.{catsSyntaxEitherId, catsSyntaxOptionId}
import io.circe.generic.semiauto.deriveEncoder
import io.circe.{Encoder, Json}
import org.scalatest.events.Ordinal

case class Scenario(
    ordinal: Ordinal,
    description: String,
    startedTimestamp: Long,
    finishedTimestamp: Option[Long],
    screenshots: List[Screenshot],
    steps: Option[Steps],
    testOutcome: TestOutcome,
    throwable: Option[Throwable]
)

object Scenario {
  implicit val ordinalEncoder: Encoder[Ordinal] = (ordinal: Ordinal) => Json.fromString(ordinal.toList.mkString("_"))
  implicit val encoder: Encoder[Scenario] = deriveEncoder[Scenario].mapJson(_.dropNullValues)
  implicit val throwableEncoder: Encoder[Throwable] = (a: Throwable) => {
    Json.obj(
      "exception-message" -> Json.fromString(a.getMessage)
    )
  }

  def starting(ordinal: Ordinal, name: String, timestamp: Long): Scenario =
    Scenario(
      ordinal = ordinal,
      description = name,
      startedTimestamp = timestamp,
      finishedTimestamp = None,
      screenshots = Nil,
      steps = None,
      testOutcome = TestOutcome.STARTING,
      throwable = None
    )

  def addAStep(scenario: Scenario, ordinal: Ordinal, message: String, throwable: Option[Throwable], timestamp: Long): Either[String, Scenario] =
    Steps
      .fromOtherSteps(otherSteps = scenario.steps, newOrdinal = ordinal, message = message, throwable = throwable, timestamp = timestamp)
      .map((newSteps: Option[Steps]) => scenario.copy(steps = newSteps))

  def update(
      scenarioFound: Scenario,
      events: StateEvent.RecordedEvents,
      outcome: TestOutcome,
      throwable: Option[Throwable],
      timestamp: Long
  ): Either[String, Scenario] = {
    if (scenarioFound.testOutcome != TestOutcome.STARTING)
      s"Scenario ${scenarioFound.description}has to have status equals to starting but it was ${scenarioFound.testOutcome}".asLeft
    else
      Steps
        .merge(events, scenarioFound.steps)
        .map(validSteps => {
          scenarioFound.copy(steps = validSteps, testOutcome = outcome, finishedTimestamp = timestamp.some, throwable = throwable)
        })
  }

  def addScreenshot(scenario: Scenario, pageUrl: String, screenshotMoment: ScreenshotMoment): (Scenario, Screenshot) = {
    def newScreenshot(existingScreenshots: List[Screenshot]): List[Screenshot] =
      List(Screenshot(pageUrl, screenshotMoment, scenario.ordinal, existingScreenshots.size + 1))

    val screenshot = newScreenshot(scenario.screenshots)

    val newScreenshots =  scenario.screenshots ::: screenshot

    (scenario.copy(screenshots = newScreenshots), screenshot.head)
  }
}
