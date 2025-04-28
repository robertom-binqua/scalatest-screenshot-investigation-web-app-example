package org.binqua.scalatest.reporter

import cats.implicits.{catsSyntaxEitherId, catsSyntaxOptionId}
import io.circe.Encoder
import io.circe.syntax.EncoderOps
import org.scalatest.events.Ordinal

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
