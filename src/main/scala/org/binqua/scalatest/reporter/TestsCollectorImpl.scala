package org.binqua.scalatest.reporter

import cats.effect.IO.catsSyntaxTuple2Parallel
import cats.implicits.{catsSyntaxEitherId, toBifunctorOps}
import io.circe.syntax.EncoderOps
import io.circe.{Json, JsonObject}
import org.apache.commons.io.FileUtils
import org.binqua.scalatest.reporter.ScreenshotMoment.{ON_ENTER_PAGE, ON_EXIT_PAGE}
import org.binqua.scalatest.reporter.util.utils.EitherOps

import java.io.File
import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter
import java.time.{Clock, ZoneId, ZonedDateTime}

trait TestsCollector {

  def add(event: StateEvent): Unit

  def createReport(): Unit

  def addScreenshotOnEnterAt(screenshotDriverData: ScreenshotDriverData): Unit

  def addScreenshotOnExitAt(screenshotDriverData: ScreenshotDriverData): Unit

}

object TestsCollectorConfigurationFactory {
  def create(systemPropertyReportDestinationKey: String, fixedClock: Clock): Either[String, TestsCollectorConfiguration] =
    for {
      sp <- Option(System.getProperty(systemPropertyReportDestinationKey))
        .toRight(s"The system property $systemPropertyReportDestinationKey specifying the root dir of the report missing. I cannot proceed")
      validSP <- Either.cond(
        sp.trim.nonEmpty,
        sp,
        "The system property $systemPropertyReportDestinationKey specifying the root dir of the report missing. I cannot proceed"
      )
      result <- {
        val reportRoot = calculateFullReportRoot(fixedClock, validSP)
        reportRoot.mkdirs()
        TestsCollectorConfiguration.from(reportRoot)
      }
    } yield result

  private def calculateFullReportRoot(fixedClock: Clock, validSP: String): File =
    new File(
      new File(System.getProperty("user.dir")).getAbsoluteFile + File.separator + validSP + File.separator + formatDateTimeFrom(fixedClock)
    )

  private def formatDateTimeFrom(fixedClock: Clock): String =
    ZonedDateTime.ofInstant(fixedClock.instant, ZoneId.of("UTC")).format(DateTimeFormatter.ofPattern("'at_'dd_MMM_yyyy_'at_'HH_mm_ss"))
}

object TestsCollector {

  val testsCollector: TestsCollector = TestsCollectorConfigurationFactory
    .create(systemPropertyReportDestinationKey = "reportDestinationRoot", fixedClock = Clock.systemUTC())
    .map(config => new TestsCollectorImpl(new ReportFileUtilsImpl(config)))
    .getOrThrow

}

object TestsCollectorConfiguration {

  /*
   * Playing a little bit with cats ... https://typelevel.org/cats/typeclasses/parallel.html
   */

  val reportDirName = "report"
  val screenshotsDirName = "screenshots"

  def from(reportDirParent: File): Either[String, TestsCollectorConfiguration] = {

    val reportDir = new File(reportDirParent.getAbsolutePath + File.separator + reportDirName)
    reportDir.mkdir()

    val screenshotsDir = new File(reportDir.getAbsolutePath + File.separator + screenshotsDirName)
    screenshotsDir.mkdir()

    (validatedReportDir(reportDir), validateScreenshotsDir(screenshotsDir))
      .parMapN((reportRoot, screenshotsRoot) => {
        new TestsCollectorConfiguration {
          override def reportRootLocation: File = reportRoot

          override def jsonReportLocation: File = new File(reportRoot.getAbsolutePath + File.separator + "testsReport.js")

          override def screenshotsRootLocation: File = screenshotsRoot

          override def screenshotsLocationPrefix: String = s"$reportDirName/$screenshotsDirName/"
        }
      })
      .leftMap(_.mkString(" - "))
  }

  private def validatedReportDir(reportDir: File): Either[List[String], File] =
    if (!reportDir.exists()) List(s"ReportDir $reportDir should exist but it does not").asLeft
    else reportDir.asRight

  private def validateScreenshotsDir(screenshotsDir: File): Either[List[String], File] = {
    if (!screenshotsDir.exists())
      List(s"ScreenshotsDir $screenshotsDir should exist but it does not").asLeft
    else screenshotsDir.asRight
  }

  def unsafeFrom(reportRootParent: File): TestsCollectorConfiguration = from(reportRootParent).getOrThrow
}

sealed trait TestsCollectorConfiguration {
  def reportRootLocation: File
  def jsonReportLocation: File
  def screenshotsRootLocation: File
  def screenshotsLocationPrefix: String
}

class TestsCollectorImpl(reportFileUtils: ReportFileUtils) extends TestsCollector {

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
}

class ReportFileUtilsImpl(config: TestsCollectorConfiguration) extends ReportFileUtils {
  override def copyFile(from: File, toSuffix: File): Unit =
    FileUtils.copyFile(
      from,
      new File(config.screenshotsRootLocation + File.separator + toSuffix)
    )

  override def writeStringToFile(stringToBeWritten: String, toSuffix: File): Unit =
    FileUtils.writeStringToFile(
      new File(config.screenshotsRootLocation + File.separator + toSuffix),
      stringToBeWritten,
      StandardCharsets.UTF_8
    )

  override def writeReport(tests: Tests): Unit = {
    val json: JsonObject = JsonObject(
      "screenshotsLocationPrefix" -> Json.fromString(config.screenshotsLocationPrefix),
      "testsReport" -> tests.asJson
    )
    FileUtils.writeStringToFile(
      config.jsonReportLocation,
      s"window.testsReport = ${json.toJson.spaces2}",
      StandardCharsets.UTF_8
    )
  }
}

trait ReportFileUtils {

  def copyFile(from: File, toSuffix: File): Unit

  def writeStringToFile(stringToBeWritten: String, toSuffix: File): Unit

  def writeReport(tests: Tests): Unit

}
