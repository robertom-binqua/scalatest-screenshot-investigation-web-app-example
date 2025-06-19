package org.binqua.scalatest.reporter.effects

import cats.effect.kernel.Async
import cats.syntax.all._
import fs2.io.file.{Files, Path}

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId, ZonedDateTime}

object TestsCollectorConfiguration {
  def from[F[_]: Async](instant: Instant, reportRootLocation: Path): F[TestsCollectorConfiguration] = {
    val reportPathName = "report"
    val screenshotsPathName = "screenshots"
    val reportPath = reportRootLocation / formatNicely(instant) / reportPathName
    val screenshotsPath = reportPath / screenshotsPathName
    for {
      _ <- Files[F].createDirectories(screenshotsPath)
    } yield new TestsCollectorConfiguration(reportPath, screenshotsPath) {
      override val screenshotsLocationPrefix: String = s"$reportPathName/$screenshotsPathName"
    }
  }

  private def formatNicely(instant: Instant): String =
    ZonedDateTime
      .ofInstant(instant, ZoneId.of("UTC"))
      .format(DateTimeFormatter.ofPattern("'at_'dd_MMM_yyyy_'at_'HH_mm_ss"))
}

abstract sealed case class TestsCollectorConfiguration(private val reportPath: Path, val screenshotsRootLocation: Path) {
  val jsonReportLocation: Path = reportPath / "testsReport.json"
  val screenshotsLocationPrefix: String
}
