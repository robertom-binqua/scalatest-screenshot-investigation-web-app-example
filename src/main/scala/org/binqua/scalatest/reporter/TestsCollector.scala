package org.binqua.scalatest.reporter

import cats.implicits.catsSyntaxOptionId
import org.binqua.scalatest.reporter.util.utils.EitherOps
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime

import java.util.concurrent.locks.{Condition, ReentrantLock}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

trait WebDriverTestsCollector {
  def addScreenshot(screenshotDriverData: ScreenshotDriverData): Unit
}

trait ReporterTestsCollector {

  def add(event: StateEvent): Unit

  def createReport(): Unit

}

trait TestsCollector extends WebDriverTestsCollector with ReporterTestsCollector

object TestsCollector {

  private val internalTestsCollector: TestsCollector = new TestsCollectorImpl(new ReportInitializerImpl)

  val webDriverTestsCollector: WebDriverTestsCollector = internalTestsCollector

  val reporterTestsCollector: ReporterTestsCollector = internalTestsCollector

}

// ThreadSafe
final class TestsCollectorImpl(reportFileUtilsInitializer: ReportInitializer) extends TestsCollector {

  private val lock: ReentrantLock = new ReentrantLock()
  private val screenshotTakenWeCanProceedConsumingEvents: Condition = lock.newCondition()
  private val lastEventIsTakeAScreenshot: Condition = lock.newCondition()

  // guarded by lock
  private var keepReadingAllEvents: Boolean = true
  // guarded by lock
  private var lastEvent: Option[StateEvent] = None
  // guarded by lock
  private var testsReport: TestsReport = TestsReport(Map.empty)

  private var maybeReportFileUtils: Option[ReportFileUtils] = None

  // blocks-until keepReadingAllEvents = false, to give time to addScreenshot to read the right test coordinates.
  def add(event: StateEvent): Unit = {
    lock.lock();
    try {
      while (!keepReadingAllEvents)
        screenshotTakenWeCanProceedConsumingEvents.await()

      lastEvent = event.some

      event match {
        case StateEvent.RunStarting(_) =>
          maybeReportFileUtils = reportFileUtilsInitializer.init().getOrThrow.some
          testsReport = TestsReport(Map.empty)
          lastEvent = None
          keepReadingAllEvents = true

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
        lastEventIsTakeAScreenshot.signalAll()
      }

    } finally {
      lock.unlock()
    }
  }

  def createReport(): Unit = {
    lock.lock()
    try {
      maybeReportFileUtils.get.writeReport(testsReport)
    } finally {
      lock.unlock()
    }
  }

  // blocks-until lastEventIsNot -> TakeAScreenshotEvent = StateEvent.Note(_, message, _, _) => message.startsWith("take screenshot now")
  def addScreenshot(screenshotDriverData: ScreenshotDriverData): Unit = {
    var maybeSomeScreenshotData: Option[Screenshot] = None
    lock.lock()
    try {
      while (!eventIsTakeAScreenshotEvent(lastEvent)) // events are still too old: waiting to reach "take a screenshot event"
        lastEventIsTakeAScreenshot.await()

      val (newTestsReport, screenshot): (TestsReport, Screenshot) =
        TestsReport
          .runningTest(testsReport)
          .flatMap(TestsReport.addScreenshot(testsReport, _, screenshotDriverData.screenshotExternalData))
          .getOrThrow

      maybeSomeScreenshotData = screenshot.some

      testsReport = newTestsReport

      keepReadingAllEvents = true
      lastEvent = None
      screenshotTakenWeCanProceedConsumingEvents.signalAll()
    } finally {
      lock.unlock()
    }

    maybeSomeScreenshotData match {
      case Some(screenshot) => saveFiles(screenshot, screenshotDriverData)
      case None             => throw new IllegalStateException(s"Ops! there are not screenshot data while saving data from $screenshotDriverData")
    }

  }

  private def saveFiles(screenshot: Screenshot, screenshotDriverData: ScreenshotDriverData): Unit = {
    val image = Future(
      maybeReportFileUtils.get.saveImage(
        data = screenshotDriverData.image,
        toSuffix = screenshot.originalFilename
      )
    )

    val htmlSource = Future(
      maybeReportFileUtils.get.writeStringToFile(
        stringToBeWritten = screenshotDriverData.pageSource,
        toSuffix = screenshot.sourceCodeFilename
      )
    )

    val sourceContentWithNoHtmlTags = Future(
      maybeReportFileUtils.get.withNoHtmlElementsToFile(
        originalSourceToBeWritten = screenshotDriverData.pageSource,
        toSuffix = screenshot.sourceWithNoHtmlFilename
      )
    )

    Await.result(Future.sequence(List(image, htmlSource, sourceContentWithNoHtmlTags)).map(_ => ()), 10.seconds)

  }

  private def eventIsTakeAScreenshotEvent(event: Option[StateEvent]): Boolean = event match {
    case Some(StateEvent.Note(_, message, _, _)) => message.startsWith("take screenshot now")
    case _                                       => false
  }

}
