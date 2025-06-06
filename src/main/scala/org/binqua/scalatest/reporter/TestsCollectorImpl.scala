package org.binqua.scalatest.reporter

import cats.effect.IO.catsSyntaxTuple2Parallel
import cats.implicits.{catsSyntaxEitherId, catsSyntaxOptionId, toBifunctorOps}
import io.circe.syntax.EncoderOps
import io.circe.{Json, JsonObject}
import org.apache.commons.io.FileUtils
import org.binqua.scalatest.reporter.util.utils
import org.binqua.scalatest.reporter.util.utils.EitherOps
import org.jsoup.Jsoup

import java.io.File
import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter
import java.time.{Clock, ZoneId, ZonedDateTime}
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.{Condition, ReentrantLock}
import scala.annotation.tailrec

trait TestsCollector {

  def add(event: StateEvent): Unit

  def createReport(): Unit

  def addScreenshot(screenshotDriverData: ScreenshotDriverData): Unit

}

object TestsCollectorConfigurationFactory {
  def create(systemPropertyReportDestinationKey: String, fixedClock: Clock): Either[String, TestsCollectorConfiguration] =
    for {
      sp <- Option(System.getProperty(systemPropertyReportDestinationKey)).toRight(invalidSystemProperty(systemPropertyReportDestinationKey))
      validSP <- Either.cond(sp.trim.nonEmpty, sp, invalidSystemProperty(systemPropertyReportDestinationKey))
      result <- {
        val reportRoot = calculateFullReportRoot(fixedClock, validSP)
        reportRoot.mkdirs()
        TestsCollectorConfiguration.from(reportRoot)
      }
    } yield result

  private def invalidSystemProperty(systemPropertyReportDestinationKey: String): String = {
    s"The system property $systemPropertyReportDestinationKey specifying the root dir of the report missing. I cannot proceed"
  }

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

          override def jsonReportLocation: File = new File(reportRoot.getAbsolutePath + File.separator + "testsReport.json")

          override def screenshotsRootLocation: File = screenshotsRoot

          override def screenshotsLocationPrefix: String = s"$reportDirName/$screenshotsDirName/"
        }
      })
      .leftMap(_.mkString(" - "))
  }

  private def validatedReportDir(reportDir: File): Either[List[String], File] =
    if (!reportDir.exists()) List(s"ReportDir $reportDir should exist but it does not").asLeft
    else reportDir.asRight

  private def validateScreenshotsDir(screenshotsDir: File): Either[List[String], File] =
    if (!screenshotsDir.exists())
      List(s"ScreenshotsDir $screenshotsDir should exist but it does not").asLeft
    else
      screenshotsDir.asRight

  def unsafeFrom(reportRootParent: File): TestsCollectorConfiguration = from(reportRootParent).getOrThrow
}

sealed trait TestsCollectorConfiguration {
  def reportRootLocation: File
  def jsonReportLocation: File
  def screenshotsRootLocation: File
  def screenshotsLocationPrefix: String
}

class TestsCollectorImpl(reportFileUtils: ReportFileUtils) extends TestsCollector {

  private val lock: ReentrantLock = new ReentrantLock()

  private val screenshotTakenWeCanProceedConsumingEvents: Condition = lock.newCondition()
  private val lastEventIsTakeAScreenshot: Condition = lock.newCondition()

  private var keepReadingAllEvents: Boolean = true
  private var lastEvent: Option[StateEvent] = None

  private val testsReport: AtomicReference[TestsReport] = new AtomicReference(TestsReport(Map.empty))

  @tailrec
  private def retryCompareAndSet[A, B](ar: AtomicReference[A], calculateNewValueFromOld: A => (A, B)): B = {
    val oldValue: A = ar.get
    val newValues: (A, B) = calculateNewValueFromOld(oldValue)
    if (!ar.compareAndSet(oldValue, newValues._1)) retryCompareAndSet(ar, calculateNewValueFromOld)
    else newValues._2
  }

  def add(event: StateEvent): Unit = {
    lock.lock();
    try {
      Thread.sleep(2000)
      while (!keepReadingAllEvents) {
        screenshotTakenWeCanProceedConsumingEvents.await()
      }
      lastEvent = event.some
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
            calculateNewValueFromOld =
              (oldTests: TestsReport) => (TestsReport.testSucceeded(oldTests, runningScenario, recordedEvent, timestamp).getOrThrow, ())
          )

        case StateEvent.Note(runningScenario, message, throwable, timestamp) =>
          retryCompareAndSet(
            ar = testsReport,
            calculateNewValueFromOld = (oldTests: TestsReport) => (TestsReport.addStep(oldTests, runningScenario, message, throwable, timestamp).getOrThrow, ())
          )
      }

      if (eventIsTakeAScreenshotEvent(lastEvent)) {
        keepReadingAllEvents = false // block the current method on the dispatcher thread when we exit, so add screenshot will be the only thread
        lastEventIsTakeAScreenshot.signalAll() //
      }

    } finally {
      lock.unlock()
    }
  }

  // blocks-until lastEventIsNot -> TakeAScreenshot
  def addScreenshot(screenshotDriverData: ScreenshotDriverData): Unit = {
    var screenshot: Option[Screenshot] = None
    lock.lock()
    try {
      while (!eventIsTakeAScreenshotEvent(lastEvent)) { // events are still too old. Waiting to reach take a screenshot event
        lastEventIsTakeAScreenshot.await()
      }
      screenshot = retryCompareAndSet(
        ar = testsReport,
        calculateNewValueFromOld = (oldTests: TestsReport) =>
          TestsReport.runningTest(oldTests).flatMap(TestsReport.addScreenshot(oldTests, _, screenshotDriverData.screenshotExternalData)).getOrThrow
      ).some

      reportFileUtils.saveImage(
        data = screenshotDriverData.image,
        toSuffix = screenshot.get.originalFilename
      )

      reportFileUtils.writeStringToFile(
        stringToBeWritten = screenshotDriverData.pageSource,
        toSuffix = screenshot.get.sourceCodeFilename
      )

      reportFileUtils.withNoHtmlElementsToFile(
        originalSourceToBeWritten = screenshotDriverData.pageSource,
        toSuffix = screenshot.get.sourceWithNoHtmlFilename
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

  def createReport(): Unit = reportFileUtils.writeReport(testsReport.get())

}

class ReportFileUtilsImpl(config: TestsCollectorConfiguration) extends ReportFileUtils {
  override def copyFile(from: File, toSuffix: File): Unit =
    FileUtils.copyFile(
      from,
      new File(config.screenshotsRootLocation + File.separator + toSuffix)
    )

  override def saveImage(data: Array[Byte], toSuffix: File): Unit =
    FileUtils.writeByteArrayToFile(new File(config.screenshotsRootLocation + File.separator + toSuffix), data)

  override def writeStringToFile(stringToBeWritten: String, toSuffix: File): Unit =
    FileUtils.writeStringToFile(
      new File(config.screenshotsRootLocation + File.separator + toSuffix),
      stringToBeWritten,
      StandardCharsets.UTF_8
    )

  override def writeReport(tests: TestsReport): Unit = {
    val json: JsonObject = JsonObject(
      "screenshotsLocationPrefix" -> Json.fromString(config.screenshotsLocationPrefix),
      "testsReport" -> tests.asJson
    )
    FileUtils.writeStringToFile(
      config.jsonReportLocation,
      json.toJson.spaces2,
      StandardCharsets.UTF_8
    )
  }

  override def withNoHtmlElementsToFile(originalSourceToBeWritten: String, toSuffix: File): Unit =
    FileUtils.writeStringToFile(
      new File(config.screenshotsRootLocation + File.separator + toSuffix),
      utils.clean(Jsoup.parse(originalSourceToBeWritten).wholeText()),
      StandardCharsets.UTF_8
    )
}

trait ReportFileUtils {

  def saveImage(data: Array[Byte], toSuffix: File): Unit

  def copyFile(from: File, toSuffix: File): Unit

  def writeStringToFile(stringToBeWritten: String, toSuffix: File): Unit

  def withNoHtmlElementsToFile(originalSourceToBeWritten: String, toSuffix: File): Unit

  def writeReport(tests: TestsReport): Unit

}
