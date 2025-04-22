package org.binqua.examples.http4sapp.app

import org.binqua.examples.http4sapp.app.StateEvent.{RecordedEvent, RecordedEvents}
import org.binqua.examples.http4sapp.util.utils.EitherOps
import org.scalatest.Reporter
import org.scalatest.events._

class ScreenshotReporterRunner extends Reporter {
  val reporter = new ScreenshotReporterImpl(TestsCollector.testsCollector)
  override def apply(event: Event): Unit = reporter.apply(event)
}

class ScreenshotReporterImpl(testsCollector: TestsCollector) extends Reporter {

  override def apply(event: Event): Unit = {
    event match {
      case TestStarting(ordinal, _, testName, _, featureAndScenario, _, _, _, _, _, _, timestamp) =>
        val testStartingEvent = org.binqua.examples.http4sapp.app.Utils
          .createARunningScenario(ordinal, testName, featureAndScenario)
          .map(rs => StateEvent.TestStarting(rs, timestamp))
          .getOrThrow
        testsCollector.add(testStartingEvent)

      case TestFailed(ordinal, _, _, testName, _, featureAndScenario, _, recordedEvents, _, throwable, _, _, _, _, _, _, timestamp) =>
        val failedEvent = org.binqua.examples.http4sapp.app.Utils
          .createARunningScenario(ordinal, testName, featureAndScenario)
          .map(rs =>
            StateEvent.TestFailed(
              runningScenario = rs,
              recordedEvent = RecordedEvents.from(recordedEvents.map((i: RecordableEvent) => RecordedEvent.from(i)).toList).getOrThrow,
              throwable,
              timestamp = timestamp
            )
          )
          .getOrThrow
        testsCollector.add(failedEvent)

      case TestSucceeded(ordinal, _, testName, _, featureAndScenario, _, recordedEvents, _, _, _, _, _, _, timestamp) =>
        val succeededEvent: StateEvent.TestSucceeded = (for {
          rs <- Utils.createARunningScenario(ordinal, testName, featureAndScenario)
          recordedEvents <- RecordedEvents.from(recordedEvents.map((i: RecordableEvent) => RecordedEvent.from(i)).toList)
        } yield StateEvent.TestSucceeded(
          runningScenario = rs,
          recordedEvent = recordedEvents,
          timestamp = timestamp
        )).getOrThrow

        testsCollector.add(succeededEvent)

      case NoteProvided(ordinal, message, Some(NameInfo(_, _, Some(suiteClassName), Some(testName))), throwable, _, _, _, _, timestamp) =>
        val newNoteEvent = org.binqua.examples.http4sapp.app.Utils
          .createARunningScenario(ordinal, suiteClassName, testName)
          .map(rs => StateEvent.Note(rs, message, throwable, timestamp))
          .getOrThrow
        testsCollector.add(newNoteEvent)

      case RunCompleted(ordinal, duration, summary, formatter, location, payload, threadName, timeStamp) =>
        testsCollector.createReport()

      case e =>
        println(s"Ignored event $e}")

    }
  }

}
