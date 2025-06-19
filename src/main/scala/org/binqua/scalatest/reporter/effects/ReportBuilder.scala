package org.binqua.scalatest.reporter.effects

import cats.effect.Async
import cats.instances.unit
import cats.syntax.all._
import fs2.io.file.{Files, Path}
import org.binqua.scalatest.reporter.util.utils
import org.binqua.scalatest.reporter.{Screenshot, StateEvent, TestsReport, TestsReportBuilder}
import org.jsoup.Jsoup

trait ReportBuilder[F[_]] {
  def build(events: List[StateEvent]): F[Unit]
}

trait ReportJsonBuilder[F[_]] {
  def build(events: TestsReport, reportDestination: TestsCollectorConfiguration): fs2.Stream[F, Unit]
}

class StreamReportJsonBuilder[F[_]: Async] extends ReportJsonBuilder[F] {
  override def build(testsReport: TestsReport, configuration: TestsCollectorConfiguration): fs2.Stream[F, Unit] =
    fs2.Stream
      .emit(TestsReport.asJson(testsReport, configuration).spaces2)
      .covary[F]
      .through(Files[F].writeUtf8(configuration.jsonReportLocation))
      .void
}

class ReportBuilderImpl[F[_]: Async](
    rootReportInitializer: RootReportInitializer[F],
    reportJsonBuilder: ReportJsonBuilder[F],
    testsReportBuilder: TestsReportBuilder
) extends ReportBuilder[F] {

  def build(events: List[StateEvent]): F[Unit] = {
    for {
      validReport <- testsReportBuilder.validateEvents(events) match {
        case Left(error: String) => Async[F].raiseError(new RuntimeException(error))
        case Right(validReport)  => Async[F].pure(validReport)
      }
      configuration <- rootReportInitializer.createRoot()
      _ <- buildTestsReport(validReport, configuration)
    } yield unit
  }

  private def buildTestsReport(events: TestsReport, configuration: TestsCollectorConfiguration): F[Unit] = {
    val result = reportJsonBuilder.build(events, configuration) ++
      filesBuilder(extractFilesInfo(events), configuration) ++
      fullReactProjectBuilder(events)
    result.compile.drain
  }

  private def fullReactProjectBuilder(events: TestsReport): fs2.Stream[F, Unit] = fs2.Stream.unit

  private def filesBuilder(screenshots: List[Screenshot], reportDestination: TestsCollectorConfiguration): fs2.Stream[F, Unit] =
    fs2.Stream
      .emits(screenshots)
      .covary[F]
      .map(screenshot =>
        imageWriter(screenshot, reportDestination) ++
          htmlContentWriter(screenshot, reportDestination) ++
          contentWithoutHtmlWriter(screenshot, reportDestination)
      )
      .parJoinUnbounded

  private def imageWriter(screenshot: Screenshot, configuration: TestsCollectorConfiguration): fs2.Stream[F, Unit] = {
    val absoluteLocation = configuration.screenshotsRootLocation / Path.fromNioPath(screenshot.originalFilename.toPath)
    for {
      _ <- fs2.Stream.eval(Files[F].createDirectories(absoluteLocation.parent.get))
      result <- fs2.Stream.emits(screenshot.screenshotDriverData.image).covary[F].through(Files[F].writeAll(absoluteLocation)).void
    } yield result
  }

  private def htmlContentWriter(screenshot: Screenshot, reportDestination: TestsCollectorConfiguration): fs2.Stream[F, Unit] = {
    val absoluteLocation = reportDestination.screenshotsRootLocation / Path.fromNioPath(screenshot.sourceCodeFilename.toPath)
    for {
      _ <- fs2.Stream.eval(Files[F].createDirectories(absoluteLocation.parent.get))
      result <- fs2.Stream.emit(screenshot.screenshotDriverData.pageSource).covary[F].through(Files[F].writeUtf8(absoluteLocation)).void
    } yield result
  }

  private def contentWithoutHtmlWriter(screenshot: Screenshot, reportDestination: TestsCollectorConfiguration): fs2.Stream[F, Unit] = {
    val absoluteLocation = reportDestination.screenshotsRootLocation / Path.fromNioPath(screenshot.sourceWithNoHtmlFilename.toPath)
    for {
      _ <- fs2.Stream.eval(Files[F].createDirectories(absoluteLocation.parent.get))
      result <- fs2.Stream
        .emit(utils.clean(Jsoup.parse(screenshot.screenshotDriverData.pageSource).wholeText()))
        .covary[F]
        .through(Files[F].writeUtf8(absoluteLocation))
        .void
    } yield result
  }

  private def extractFilesInfo(events: TestsReport): List[Screenshot] =
    for {
      tests <- events.tests.values.toList
      features <- tests.features.featuresMap.values
      scenarios <- features.scenarios.scenariosMap.values
      screenshots <- scenarios.screenshots
    } yield screenshots

}
