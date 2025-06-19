package org.binqua.scalatest.reporter

import cats.effect.{Clock, IO}
import org.binqua.scalatest.reporter.StateEvent.{EventIgnored, RecordedEvent, RecordedEvents}
import org.binqua.scalatest.reporter.effects.{DateTimePrefixRootReportInitializer, ReportBuilder, ReportBuilderImpl, StreamReportJsonBuilder}
import org.binqua.scalatest.reporter.util.utils.EitherOps
import org.scalatest.Reporter
import org.scalatest.events._

class ScreenshotReporterRunner extends Reporter {

  val reporter: Reporter = {
    val rootReportInitializer = new DateTimePrefixRootReportInitializer[IO](Clock[IO])
    val reportJsonBuilder = new StreamReportJsonBuilder[IO]()
    val testsReportBuilder = new TestsReportBuilderImpl()
    val reportBuilder: ReportBuilderImpl[IO] = new ReportBuilderImpl[IO](rootReportInitializer, reportJsonBuilder, testsReportBuilder)

    new ScreenshotReporterImpl(TestsCollector.reporterTestsCollector, reportBuilder)
  }

  override def apply(event: Event): Unit = reporter.apply(event)
}

class ScreenshotReporterImpl(testsCollector: ReporterTestsCollector, reportBuilder: ReportBuilder[IO]) extends Reporter {

  override def apply(event: Event): Unit = {

    toInternalEvent(event) match {

      case EventIgnored(originalEvent) =>
        println(s"originalEvent ignored : $originalEvent")

      case completed: StateEvent.RunCompleted =>
        val stateEvents = testsCollector.add(completed)
        reportBuilder.build(stateEvents).unsafeRunSync()(cats.effect.unsafe.implicits.global)

      case event =>
        testsCollector.add(event)

    }
  }

  private def toInternalEvent(event: Event): StateEvent = {
    event match {
      case runStarting: RunStarting =>
        StateEvent.RunStarting(runStarting.timeStamp)

      case TestStarting(ordinal, _, testName, _, featureAndScenario, _, _, _, _, _, _, timestamp) =>
        Utils
          .createARunningScenario(ordinal, testName, featureAndScenario)
          .map(rs => StateEvent.TestStarting(rs, timestamp))
          .getOrThrow

      case TestFailed(ordinal, _, _, testName, _, featureAndScenario, _, recordedEvents, _, throwable, _, _, _, _, _, _, timestamp) =>
        Utils
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

      case TestSucceeded(ordinal, _, testName, _, featureAndScenario, _, recordedEvents, _, _, _, _, _, _, timestamp) =>
        (for {
          rs <- Utils.createARunningScenario(ordinal, testName, featureAndScenario)
          recordedEvents <- RecordedEvents.from(recordedEvents.map((i: RecordableEvent) => RecordedEvent.from(i)).toList)
        } yield StateEvent.TestSucceeded(
          runningScenario = rs,
          recordedEvent = recordedEvents,
          timestamp = timestamp
        )).getOrThrow

      case NoteProvided(ordinal, message, Some(NameInfo(_, _, Some(suiteClassName), Some(testName))), throwable, _, _, _, _, timestamp) =>
        Utils
          .createARunningScenario(ordinal, suiteClassName, testName)
          .map(rs => StateEvent.Note(rs, message, throwable, timestamp))
          .getOrThrow

      case runCompleted: RunCompleted =>
        StateEvent.RunCompleted(runCompleted.timeStamp)

      case e =>
        EventIgnored(e.toString)

    }
  }
}
