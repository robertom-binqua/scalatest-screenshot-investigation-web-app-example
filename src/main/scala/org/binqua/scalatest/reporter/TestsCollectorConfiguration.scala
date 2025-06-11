package org.binqua.scalatest.reporter

import cats.effect.IO.catsSyntaxTuple2Parallel
import cats.implicits.{catsSyntaxEitherId, toBifunctorOps}
import org.binqua.scalatest.reporter.util.utils.EitherOps

import java.io.File
import java.time.format.DateTimeFormatter
import java.time.{Clock, ZoneId, ZonedDateTime}

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

object TestsCollectorConfiguration {

  /*
   * Playing a little bit with cats ... https://typelevel.org/cats/typeclasses/parallel.html
   */

  private val reportDirName = "report"
  private val screenshotsDirName = "screenshots"

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