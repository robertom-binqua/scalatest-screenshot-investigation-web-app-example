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
  def create(systemPropertyReportDestinationKey: String, fixedClock: Clock): Either[String, TestsCollectorConfiguration] = {
    if (Strings.isNullOrEmpty(System.getProperty(systemPropertyReportDestinationKey)))
      "System property <exampleOfSystemPropertyKey> specifying the root dir of the report missing. I cannot proceed".asLeft
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
  def from(reportLocationFile: File, screenshotLocationFile: File): Either[String, TestsCollectorConfiguration] = {
    if (!reportLocationFile.exists())
      ???
    else if (!screenshotLocationFile.exists())
      ???
    else
      new TestsCollectorConfiguration {
        override def reportLocation: File = reportLocationFile
        override def jsonReport: File = new File(reportLocationFile.getAbsolutePath + File.separator + "report.json")
        override def screenshotLocation: File = screenshotLocationFile
      }.asRight
  }
  def unsafeFrom(reportLocation: File, screenshotLocation: File): TestsCollectorConfiguration = from(reportLocation, screenshotLocation).getOrThrow
}
sealed trait TestsCollectorConfiguration {
  def reportLocation: File
  def jsonReport: File
  def screenshotLocation: File
}

class TestsCollectorImpl(testsCollectorConfiguration: TestsCollectorConfiguration) extends TestsCollector {

  var tests: Tests = Tests(Map.empty)

  def add(event: StateEvent): Unit =
    event match {
      case StateEvent.TestStarting(runningScenario, timestamp) =>
        tests = tests.testStarting(runningScenario, timestamp).getOrThrow

      case StateEvent.TestFailed(runningScenario, timestamp) =>
        tests = tests.testFailed(runningScenario, timestamp).getOrThrow

      case StateEvent.TestSucceeded(runningScenario, timestamp) =>
        tests = tests.testSucceeded(runningScenario, timestamp).getOrThrow
    }

  def createReport(): Unit = FileUtils.writeStringToFile(testsCollectorConfiguration.jsonReport, tests.asJson.spaces2, StandardCharsets.UTF_8)

  private def addScreenshot(scrFile: File, pageUrl: String, screenshotMoment: ScreenshotMoment): Unit = { //    tests
    val (newTests, screenshotLocation): (Tests, File) = tests.runningTest.flatMap(tests.addScreenshot(_, pageUrl, screenshotMoment)).getOrThrow
    tests = newTests
    FileUtils.copyFile(scrFile, screenshotLocation)
  }

  override def addScreenshotOnExitAt(scrFile: File, pageUrl: String): Unit = addScreenshot(scrFile, pageUrl, ON_EXIT_PAGE)

  override def addScreenshotOnEnterAt(scrFile: File, pageUrl: String): Unit = addScreenshot(scrFile, pageUrl, ON_ENTER_PAGE)
}
