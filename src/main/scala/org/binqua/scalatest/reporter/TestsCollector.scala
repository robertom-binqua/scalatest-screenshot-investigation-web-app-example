package org.binqua.scalatest.reporter

import org.binqua.scalatest.reporter.util.utils.EitherOps

import java.time.Clock
import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec


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

class TestsCollectorImpl(reportFileUtils: ReportFileUtils) extends TestsCollector {

  val testsReport: AtomicReference[TestsReport] = new AtomicReference(TestsReport(Map.empty))

  @tailrec
  private def retryCompareAndSet[A, B](ar: AtomicReference[A], calculateNewValueFromOld: A => (A, B)): B = {
    val oldValue: A = ar.get
    val newValues: (A, B) = calculateNewValueFromOld(oldValue)
    if (!ar.compareAndSet(oldValue, newValues._1)) retryCompareAndSet(ar, calculateNewValueFromOld)
    else newValues._2
  }

  def add(event: StateEvent): Unit = {
    event match {
      case StateEvent.TestStarting(runningScenario, timestamp) =>
        retryCompareAndSet(
          ar = testsReport,
          calculateNewValueFromOld = (oldTests: TestsReport) => (TestsReport.testStarting(oldTests, runningScenario, timestamp).getOrThrow, ())
        )

      case StateEvent.TestFailed(runningScenario, recordedEvent, throwable, timestamp) =>
        retryCompareAndSet(
          ar = testsReport,
          calculateNewValueFromOld =
            (oldTests: TestsReport) => (TestsReport.testFailed(oldTests, runningScenario, recordedEvent, throwable, timestamp).getOrThrow, ())
        )

      case StateEvent.TestSucceeded(runningScenario, recordedEvent, timestamp) =>
        retryCompareAndSet(
          ar = testsReport,
          calculateNewValueFromOld = (oldTests: TestsReport) => (TestsReport.testSucceeded(oldTests, runningScenario, recordedEvent, timestamp).getOrThrow, ())
        )

      case StateEvent.Note(runningScenario, message, throwable, timestamp) =>
        retryCompareAndSet(
          ar = testsReport,
          calculateNewValueFromOld = (oldTests: TestsReport) => (TestsReport.addStep(oldTests, runningScenario, message, throwable, timestamp).getOrThrow, ())
        )
    }
  }

  def createReport(): Unit = reportFileUtils.writeReport(testsReport.get())

  def addScreenshot(screenshotDriverData: ScreenshotDriverData): Unit = {

    val screenshot: Screenshot =
      retryCompareAndSet(
        ar = testsReport,
        calculateNewValueFromOld = (oldTests: TestsReport) =>
          TestsReport.runningTest(oldTests).flatMap(TestsReport.addScreenshot(oldTests, _, screenshotDriverData.screenshotExternalData)).getOrThrow
      )

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

  }

}

