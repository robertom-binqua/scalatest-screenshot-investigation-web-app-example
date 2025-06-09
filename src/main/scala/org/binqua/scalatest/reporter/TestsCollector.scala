package org.binqua.scalatest.reporter

import cats.implicits.catsSyntaxOptionId
import org.binqua.scalatest.reporter.util.utils.EitherOps

import java.time.Clock
import java.util.concurrent.locks.{Condition, ReentrantLock}

trait TestsCollector {

  def add(event: StateEvent): Unit

  def createReport(): Unit

  def addScreenshot(screenshotDriverData: ScreenshotDriverData): Unit

}

object TestsCollector {

  val testsCollector: TestsCollector = TestsCollectorConfigurationFactory
    .create(systemPropertyReportDestinationKey = "reportDestinationRoot", fixedClock = Clock.systemUTC())
    .map(config => new TestsCollectorImpl(new ReportFileUtilsImpl(config)))
    .getOrThrow

}

// ThreadSafe
final class TestsCollectorImpl(reportFileUtils: ReportFileUtils) extends TestsCollector {

  private val lock: ReentrantLock = new ReentrantLock()
  private val screenshotTakenWeCanProceedConsumingEvents: Condition = lock.newCondition()
  private val lastEventIsTakeAScreenshot: Condition = lock.newCondition()

  // guarded by lock
  private var keepReadingAllEvents: Boolean = true
  // guarded by lock
  private var lastEvent: Option[StateEvent] = None
  // guarded by lock
  private var testsReport: TestsReport = TestsReport(Map.empty)

  // blocks-until keepReadingAllEvents = false.
  def add(event: StateEvent): Unit = {
    lock.lock();
    try {
      while (!keepReadingAllEvents)
        screenshotTakenWeCanProceedConsumingEvents.await()

      lastEvent = event.some

      event match {
        case StateEvent.TestStarting(runningScenario, timestamp) =>
          testsReport = TestsReport.testStarting(testsReport, runningScenario, timestamp).getOrThrow

        case StateEvent.TestFailed(runningScenario, recordedEvent, throwable, timestamp) =>
          testsReport = TestsReport.testFailed(testsReport, runningScenario, recordedEvent, throwable, timestamp).getOrThrow

        case StateEvent.TestSucceeded(runningScenario, recordedEvent, timestamp) =>
          testsReport = TestsReport.testSucceeded(testsReport, runningScenario, recordedEvent, timestamp).getOrThrow

        case StateEvent.Note(runningScenario, message, throwable, timestamp) =>
          testsReport = TestsReport.addStep(testsReport, runningScenario, message, throwable, timestamp).getOrThrow
      }
      if (eventIsTakeAScreenshotEvent(lastEvent)) {
        keepReadingAllEvents = false // block the current method on the dispatcher thread when we exit, so add screenshot will be the only thread
        lastEventIsTakeAScreenshot.signalAll() //
      }

    } finally {
      lock.unlock()
    }
  }

  // blocks-until lastEventIsNot -> TakeAScreenshotEvent = StateEvent.Note(_, message, _, _) => message.startsWith("take screenshot now")
  def addScreenshot(screenshotDriverData: ScreenshotDriverData): Unit = {
    lock.lock()
    try {
      while (!eventIsTakeAScreenshotEvent(lastEvent)) // events are still too old. Waiting to reach take a screenshot event
        lastEventIsTakeAScreenshot.await()

      val (newTests, screenshot): (TestsReport, Screenshot) =
        TestsReport.runningTest(testsReport).flatMap(TestsReport.addScreenshot(testsReport, _, screenshotDriverData.screenshotExternalData)).getOrThrow

      testsReport = newTests

      reportFileUtils.saveImage(
        data = screenshotDriverData.image,
        toSuffix = screenshot.originalFilename
      )

      reportFileUtils.writeStringToFile(
        stringToBeWritten = screenshotDriverData.pageSource,
        toSuffix = screenshot.sourceCodeFilename
      )

      reportFileUtils.withNoHtmlElementsToFile(
        originalSourceToBeWritten = screenshotDriverData.pageSource,
        toSuffix = screenshot.sourceWithNoHtmlFilename
      )

      keepReadingAllEvents = true
      lastEvent = None
      screenshotTakenWeCanProceedConsumingEvents.signalAll()
    } finally {
      lock.unlock()
    }

  }

  private def eventIsTakeAScreenshotEvent(event: Option[StateEvent]): Boolean = event match {
    case Some(StateEvent.Note(_, message, _, _)) => message.startsWith("take screenshot now")
    case _                                       => false
  }

  def createReport(): Unit = {
    lock.lock()
    try {
      reportFileUtils.writeReport(testsReport)
    } finally {
      lock.unlock()
    }
  }

}
