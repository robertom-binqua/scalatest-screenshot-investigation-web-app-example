package org.binqua.scalatest.reporter.effects

import cats.effect.{Async, Clock}
import cats.implicits._
import fs2.io.file.Path

trait RootReportInitializer[F[_]] {

  def createRoot(): F[TestsCollectorConfiguration]

}

class DateTimePrefixRootReportInitializer[F[_]: Async](clock: Clock[F]) extends RootReportInitializer[F] {

  override def createRoot(): F[TestsCollectorConfiguration] = for {
    rootPath <- readDestinationProperty()
    testsCollectorConfiguration <- createReportRoot(clock, rootPath)
  } yield testsCollectorConfiguration

  private def readDestinationProperty(): F[Path] =
    Async[F]
      .pure(Option(System.getProperty("test_report_destination_root")).map(_.trim))
      .map {
        case Some("") | None => "test_report_root"
        case Some(value)     => value
      }
      .map((r: String) => Path(r))

  private def createReportRoot(clock: Clock[F], reportDestinationRoot: Path): F[TestsCollectorConfiguration] = for {
    instant <- clock.realTimeInstant
    configuration <- TestsCollectorConfiguration.from[F](instant, reportDestinationRoot)
  } yield configuration
}
