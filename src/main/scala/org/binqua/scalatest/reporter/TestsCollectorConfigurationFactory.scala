package org.binqua.scalatest.reporter

import org.binqua.scalatest.reporter.ScreenshotMoment.{ON_ENTER_PAGE, ON_EXIT_PAGE}
import org.binqua.scalatest.reporter.Utils.EitherOps

import java.io.File
import java.time.format.DateTimeFormatter
import java.time.{Clock, ZoneId, ZonedDateTime}

object TestsCollectorConfigurationFactory:
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
      new File(System.getProperty("user.dir")).getAbsoluteFile.getAbsolutePath + File.separator + validSP + File.separator + formatDateTimeFrom(fixedClock)
    )

  private def formatDateTimeFrom(fixedClock: Clock): String =
    ZonedDateTime.ofInstant(fixedClock.instant, ZoneId.of("UTC")).format(DateTimeFormatter.ofPattern("'at_'dd_MMM_yyyy_'at_'HH_mm_ss"))




