package org.binqua.scalatest.reporter

import cats.implicits.catsSyntaxOptionId
import org.scalatest.events.{InfoProvided, Ordinal, RecordableEvent}

import java.io.File

final case class ScreenshotExternalData(pageUrl: String, pageTitle: String, screenshotMoment: ScreenshotMoment)
final case class ScreenshotDriverData(image: File, pageSource: String, screenshotExternalData: ScreenshotExternalData)

sealed trait StateEvent

object StateEvent {
  case class TestStarting(runningScenario: RunningScenario, timestamp: Long) extends StateEvent

  case class TestFailed(runningScenario: RunningScenario, recordedEvent: RecordedEvents, throwable: Option[Throwable], timestamp: Long) extends StateEvent

  case class TestSucceeded(runningScenario: RunningScenario, recordedEvent: RecordedEvents, timestamp: Long) extends StateEvent

  case class Note(runningScenario: RunningScenario, message: String, throwable: Option[Throwable], timestamp: Long) extends StateEvent

  object RecordedEvent {
    def from(i: RecordableEvent): RecordedEvent = i match {
      case ip: InfoProvided => RecordedEvent(ip.ordinal, ip.message, ip.throwable, ip.timeStamp)
      case _                => ???
    }
  }
  case class RecordedEvent(ordinal: Ordinal, message: String, throwable: Option[Throwable], timestamp: Long) extends StateEvent

  object RecordedEvents {
    def from(events: List[RecordedEvent]): Either[String, RecordedEvents] = {
      val duplicates = events
        .groupBy(_.ordinal)
        .collect { case (ordinal, evs) if evs.size > 1 => ordinal }
      if (duplicates.nonEmpty)
        Left(s"Duplicate ordinals found: ${duplicates.mkString(", ")}")
      else
        Right(new RecordedEvents(events.sortWith((l, r) => l.ordinal > r.ordinal).some) {})
    }

  }

  sealed abstract case class RecordedEvents(events: Option[List[RecordedEvent]])

}
