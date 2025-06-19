package org.binqua.scalatest.reporter

import cats.implicits.{catsSyntaxEitherId, catsSyntaxOptionId}
import org.scalatest.events.{InfoProvided, Ordinal, RecordableEvent}

final case class ScreenshotExternalData(pageUrl: String, pageTitle: String, screenshotMoment: ScreenshotMoment)

final case class ScreenshotDriverData(image: Array[Byte], pageSource: String, screenshotExternalData: ScreenshotExternalData)

sealed trait StateEvent

object StateEvent {

  def extractRunningScenarioFrom(stateEvent: Option[StateEvent]): Either[String, RunningScenario] = stateEvent match {
    case Some(value) =>
      value match {
        case Note(runningScenario, message, _, _) =>
          if (message.startsWith("take screenshot now")) runningScenario.asRight else "ops!! you did not use the api properly".asLeft
        case _ =>
          "ops!! you did not use the api properly".asLeft
      }
    case None => "there is not state event".asLeft
  }

  case class EventIgnored(originalEvent: String) extends StateEvent

  case class RunStarting(timestamp: Long) extends StateEvent

  case class RunCompleted(timestamp: Long) extends StateEvent

  case class TestStarting(runningScenario: RunningScenario, timestamp: Long) extends StateEvent

  case class TestFailed(runningScenario: RunningScenario, recordedEvent: RecordedEvents, throwable: Option[Throwable], timestamp: Long) extends StateEvent

  case class TestSucceeded(runningScenario: RunningScenario, recordedEvent: RecordedEvents, timestamp: Long) extends StateEvent

  case class Note(runningScenario: RunningScenario, message: String, throwable: Option[Throwable], timestamp: Long) extends StateEvent

  case class Screenshot(runningScenario: RunningScenario, screenshotDriverData: ScreenshotDriverData, timestamp: Long) extends StateEvent

  object RecordedEvent {
    def from(i: RecordableEvent): RecordedEvent = i match {
      case ip: InfoProvided => RecordedEvent(ip.ordinal, ip.message, ip.throwable, ip.timeStamp)
      case _                => ???
    }
  }
  case class RecordedEvent(ordinal: Ordinal, message: String, throwable: Option[Throwable], timestamp: Long) extends StateEvent

  object RecordedEvents {
    val empty: RecordedEvents = new RecordedEvents(None) {}
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
