package org.binqua.examples.http4sapp.app

import cats.implicits.{catsSyntaxEitherId, catsSyntaxOptionId}
import io.circe.generic.semiauto.deriveEncoder
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}
import org.scalatest.events.Ordinal

import java.io.File

object Step {
  implicit val featureEncoder: Encoder[Step] = Encoder.instance { step =>
    Json.obj(
      "message" -> Json.fromString(step.message),
      "timestamp" -> Json.fromLong(step.timestamp),
      "ordinal" -> Json.fromString(step.ordinal.toList.mkString("_"))
    )
  }
}

case class Step(ordinal: Ordinal, message: String, throwable: Option[Throwable], timestamp: Long)

object Steps {
  implicit val encoder: Encoder[Steps] = (steps: Steps) => steps.list.asJson

  def merge(events: StateEvent.RecordedEvents, steps: Option[Steps]): Either[String, Option[Steps]] = {
    def toStep: StateEvent.RecordedEvent => Step =
      event => Step(event.ordinal, event.message, event.throwable, event.timestamp)

    val stepsFromEvent: Option[List[Step]] = events.events
      .flatMap((recordedEvents: List[StateEvent.RecordedEvent]) => recordedEvents.map(toStep).some)

    val result: Option[List[Step]] = (stepsFromEvent, steps.map(_.list)) match {
      case (None, None)         => None
      case (Some(steps), None)  => Some(steps)
      case (None, Some(steps))  => Some(steps)
      case (Some(sa), Some(sb)) => Some(sa ++ sb)
    }
    result.map(r => new Steps(r.sortWith((l, r) => l.ordinal < r.ordinal)) {}).asRight
  }

  def fromOtherSteps(
                      otherSteps: Option[Steps],
                      newOrdinal: Ordinal,
                      message: String,
                      throwable: Option[Throwable],
                      timestamp: Long
                    ): Either[String, Option[Steps]] = {
    def newStep: Step = Step(newOrdinal, message, throwable, timestamp)

    otherSteps match {
      case Some(oldSteps: Steps) =>
        if (oldSteps.list.map(_.ordinal).contains(newOrdinal))
          "duplicated ordinal".asLeft
        new Steps(newStep :: oldSteps.list) {}.some.asRight
      case None => new Steps(List(newStep)) {}.some.asRight
    }
  }
}

abstract case class Steps(list: List[Step])

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

  def addAStep(scenario: Scenario, ordinal: Ordinal, message: String, throwable: Option[Throwable], timestamp: Long): Either[String, Scenario] =
    Steps
      .fromOtherSteps(otherSteps = scenario.steps, newOrdinal = ordinal, message = message, throwable = throwable, timestamp = timestamp)
      .map((newSteps: Option[Steps]) => scenario.copy(steps = newSteps))

  def update(scenarioFound: Scenario, events: StateEvent.RecordedEvents, outcome: TestOutcome, timestamp: Long): Either[String, Scenario] = {
    if (scenarioFound.testOutcome != TestOutcome.STARTING)
      s"Scenario ${scenarioFound.description}has to have status equals to starting but it was ${scenarioFound.testOutcome}".asLeft
    else
      Steps
        .merge(events, scenarioFound.steps)
        .map(validSteps => {
          scenarioFound.copy(steps = validSteps, testOutcome = outcome, finishedTimestamp = timestamp.some)
        })
  }
}

case class Scenario(
    ordinal: Ordinal,
    description: String,
    startedTimestamp: Long,
    finishedTimestamp: Option[Long],
    screenshots: Option[List[Screenshot]],
    steps: Option[Steps],
    testOutcome: TestOutcome
) {

  def withNewScreenshot(pageUrl: String, screenshotMoment: ScreenshotMoment): (Scenario, File) = {
    val maybeScreenshots: Option[List[Screenshot]] = screenshots
      .map(s => Screenshot(pageUrl, screenshotMoment, ordinal, s.size + 1) :: s)
      .orElse(Some(List(Screenshot(pageUrl, screenshotMoment, ordinal, 1))))
    (this.copy(screenshots = maybeScreenshots), maybeScreenshots.get.head.toFile)
  }
}
