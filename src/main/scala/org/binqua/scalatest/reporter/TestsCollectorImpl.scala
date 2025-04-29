package org.binqua.scalatest.reporter

import org.binqua.scalatest.reporter.ScreenshotMoment.{ON_ENTER_PAGE, ON_EXIT_PAGE}
import org.binqua.scalatest.reporter.Utils.EitherOps

import java.time.Clock

trait TestsCollector:

  def add(event: StateEvent): Unit

  def createReport(): Unit

  def addScreenshotOnEnterAt(screenshotDriverData: ScreenshotDriverData): Unit

  def addScreenshotOnExitAt(screenshotDriverData: ScreenshotDriverData): Unit

class TestsCollectorImpl(reportFileUtils: ReportFileUtils) extends TestsCollector:

  var tests: Tests = Tests(Map.empty)

  def add(event: StateEvent): Unit =
    event match {
      case StateEvent.TestStarting(runningScenario, timestamp) =>
        tests = Tests.testStarting(tests, runningScenario, timestamp).getOrThrow

      case StateEvent.TestFailed(runningScenario, recordedEvent, throwable, timestamp) =>
        tests = Tests.testFailed(tests, runningScenario, recordedEvent, throwable, timestamp).getOrThrow

      case StateEvent.TestSucceeded(runningScenario, recordedEvent, timestamp) =>
        tests = Tests.testSucceeded(tests, runningScenario, recordedEvent, timestamp).getOrThrow

      case StateEvent.Note(runningScenario, message, throwable, timestamp) =>
        tests = Tests.addStep(tests, runningScenario, message, throwable, timestamp).getOrThrow
    }

  def createReport(): Unit =
    reportFileUtils.writeReport(tests)

  private def addScreenshot(screenshotDriverData: ScreenshotDriverData, screenshotMoment: ScreenshotMoment): Unit = {

    val (newTests, screenshot): (Tests, Screenshot) =
      Tests.runningTest(tests).flatMap(Tests.addScreenshot(tests, _, screenshotDriverData.pageUrl, screenshotMoment)).getOrThrow

    tests = newTests

    reportFileUtils.copyFile(
      from = screenshotDriverData.screenshotImage,
      toSuffix = screenshot.originalFileLocation
    )

    reportFileUtils.writeStringToFile(
      stringToBeWritten = screenshotDriverData.pageSource,
      toSuffix = screenshot.sourceCode
    )

  }

  override def addScreenshotOnExitAt(screenshotDriverData: ScreenshotDriverData): Unit = addScreenshot(screenshotDriverData, ON_EXIT_PAGE)

  override def addScreenshotOnEnterAt(screenshotDriverData: ScreenshotDriverData): Unit = addScreenshot(screenshotDriverData, ON_ENTER_PAGE)

object TestsCollector:

  val testsCollector: TestsCollector = TestsCollectorConfigurationFactory
    .create(systemPropertyReportDestinationKey = "reportDestinationRoot", fixedClock = Clock.systemUTC())
    .map(config => new TestsCollectorImpl(new ReportFileUtilsImpl(config)))
    .getOrThrow
