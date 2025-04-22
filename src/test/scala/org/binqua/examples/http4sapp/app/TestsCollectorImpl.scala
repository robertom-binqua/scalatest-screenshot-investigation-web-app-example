package org.binqua.examples.http4sapp.app

import cats.implicits.catsSyntaxEitherId
import com.google.common.base.Strings
import io.circe.syntax.EncoderOps
import org.apache.commons.io.FileUtils
import org.binqua.examples.http4sapp.app.ScreenshotMoment.{ON_ENTER_PAGE, ON_EXIT_PAGE}
import org.binqua.examples.http4sapp.util.utils.EitherOps

import java.io.File
import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter
import java.time.{Clock, ZoneId, ZonedDateTime}

trait TestsCollector {

  def add(event: StateEvent): Unit

  def createReport(): Unit

  def addScreenshotOnEnterAt(scrFile: File, pageUrl: String): Unit

  def addScreenshotOnExitAt(scrFile: File, pageUrl: String): Unit

}

object TestsCollectorConfigurationFactory {
  def create(systemPropertyReportDestinationKey: String, fixedClock: Clock): Either[String, TestsCollectorConfiguration] =
    if (Strings.isNullOrEmpty(System.getProperty(systemPropertyReportDestinationKey)))
      s"The system property $systemPropertyReportDestinationKey specifying the root dir of the report missing. I cannot proceed".asLeft
    else {
      val systemProperty = System.getProperty(systemPropertyReportDestinationKey)
      val root = new File(
        new File(System.getProperty("user.dir")).getAbsoluteFile + File.separator + systemProperty + File.separator + formatDateTimeFrom(fixedClock)
      )
      val reportDir = new File(root.getAbsolutePath + File.separator + "report")
      val screenshotsDir = new File(root.getAbsolutePath + File.separator + "screenshots")

      reportDir.mkdirs()
      screenshotsDir.mkdirs()

      TestsCollectorConfiguration.from(reportDir, screenshotsDir)

    }

  private def formatDateTimeFrom(fixedClock: Clock): String =
    ZonedDateTime.ofInstant(fixedClock.instant, ZoneId.of("UTC")).format(DateTimeFormatter.ofPattern("'at_'dd_MMM_yyyy_'at_'HH_mm_ss"))
}

object TestsCollector {

  val testsCollector: TestsCollector = TestsCollectorConfigurationFactory
    .create(systemPropertyReportDestinationKey = "reportDestinationRoot", fixedClock = Clock.systemUTC())
    .map(new TestsCollectorImpl(_))
    .getOrThrow

}

object TestsCollectorConfiguration {
  def from(reportLocationFile: File, screenshotsRoot: File): Either[String, TestsCollectorConfiguration] = {
    if (!reportLocationFile.exists())
      ???
    else if (!screenshotsRoot.exists())
      ???
    else
      new TestsCollectorConfiguration {
        override def reportRootLocation: File = reportLocationFile
        override def jsonReport: File = new File(reportLocationFile.getAbsolutePath + File.separator + "report.json")
        override def screenshotsRootLocation: File = screenshotsRoot
      }.asRight
  }
  def unsafeFrom(reportLocation: File, screenshotLocation: File): TestsCollectorConfiguration = from(reportLocation, screenshotLocation).getOrThrow
}

sealed trait TestsCollectorConfiguration {
  def reportRootLocation: File
  def jsonReport: File
  def screenshotsRootLocation: File
}

class TestsCollectorImpl(testsCollectorConfiguration: TestsCollectorConfiguration) extends TestsCollector {

  var tests: Tests = Tests(Map.empty)

  def add(event: StateEvent): Unit =
    event match {
      case StateEvent.TestStarting(runningScenario, timestamp) =>
        tests = tests.testStarting(runningScenario, timestamp).getOrThrow

      case StateEvent.TestFailed(runningScenario, recordedEvent, throwable, timestamp) =>
        tests = tests.testFailed(runningScenario, recordedEvent, throwable, timestamp).getOrThrow

      case StateEvent.TestSucceeded(runningScenario, recordedEvent, timestamp) =>
        tests = tests.testSucceeded(runningScenario, recordedEvent, timestamp).getOrThrow

      case StateEvent.Note(runningScenario, message, throwable, timestamp) =>
        tests = Tests.addStep(tests, runningScenario, message, throwable, timestamp).getOrThrow
    }

  def createReport(): Unit =
    FileUtils.writeStringToFile(testsCollectorConfiguration.jsonReport, tests.asJson.spaces2, StandardCharsets.UTF_8)

  private def addScreenshot(scrFile: File, pageUrl: String, screenshotMoment: ScreenshotMoment): Unit = { //    tests
    val (newTests, screenshotSuffixLocation): (Tests, File) = tests.runningTest.flatMap(tests.addScreenshot(_, pageUrl, screenshotMoment)).getOrThrow
    tests = newTests
    FileUtils.copyFile(scrFile, new File(testsCollectorConfiguration.screenshotsRootLocation + File.separator + screenshotSuffixLocation))
  }

  override def addScreenshotOnExitAt(scrFile: File, pageUrl: String): Unit = addScreenshot(scrFile, pageUrl, ON_EXIT_PAGE)

  override def addScreenshotOnEnterAt(scrFile: File, pageUrl: String): Unit = addScreenshot(scrFile, pageUrl, ON_ENTER_PAGE)
}
