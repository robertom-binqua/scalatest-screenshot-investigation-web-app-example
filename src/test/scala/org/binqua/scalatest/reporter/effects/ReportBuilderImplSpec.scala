package org.binqua.scalatest.reporter.effects

import cats.effect.IO
import cats.implicits.{catsSyntaxEitherId, toBifunctorOps}
import fs2.io.file.{Files, Path}
import munit.CatsEffectSuite
import org.binqua.scalatest.reporter.StateEvent._
import org.binqua.scalatest.reporter.{ReferenceData, RunningScenario, TestsReportBuilderImpl}
import org.scalatest.events.Ordinal

import java.time.{ZoneId, ZonedDateTime}

class ReportBuilderImplSpec extends CatsEffectSuite {

  private val reportDestinationDirName = "ReportBuilderImplSpec"
  private val reportDestinationDirPath = Path(reportDestinationDirName) / "report-root"
  private val now = ZonedDateTime.of(2021, 2, 18, 13, 1, 2, 0, ZoneId.of("UTC"))

  test("given a valid sequence of events, we can build a full report") {

    val expectedFilesToBeGenerated: List[String] = List(
      "/report-root",
      "/report-root/at_18_Feb_2021_at_13_01_02",
      "/report-root/at_18_Feb_2021_at_13_01_02/report",
      "/report-root/at_18_Feb_2021_at_13_01_02/report/screenshots",
      "/report-root/at_18_Feb_2021_at_13_01_02/report/screenshots/scenario_ordinal_1_0",
      "/report-root/at_18_Feb_2021_at_13_01_02/report/screenshots/scenario_ordinal_1_0/original",
      "/report-root/at_18_Feb_2021_at_13_01_02/report/screenshots/scenario_ordinal_1_0/original/1_ON_EXIT_PAGE.png",
      "/report-root/at_18_Feb_2021_at_13_01_02/report/screenshots/scenario_ordinal_1_0/sources",
      "/report-root/at_18_Feb_2021_at_13_01_02/report/screenshots/scenario_ordinal_1_0/sources/1_ON_EXIT_PAGE.txt",
      "/report-root/at_18_Feb_2021_at_13_01_02/report/screenshots/scenario_ordinal_1_0/withNoHtml",
      "/report-root/at_18_Feb_2021_at_13_01_02/report/screenshots/scenario_ordinal_1_0/withNoHtml/1_ON_EXIT_PAGE.txt",
      "/report-root/at_18_Feb_2021_at_13_01_02/report/screenshots/scenario_ordinal_1_1",
      "/report-root/at_18_Feb_2021_at_13_01_02/report/screenshots/scenario_ordinal_1_1/original",
      "/report-root/at_18_Feb_2021_at_13_01_02/report/screenshots/scenario_ordinal_1_1/original/1_ON_EXIT_PAGE.png",
      "/report-root/at_18_Feb_2021_at_13_01_02/report/screenshots/scenario_ordinal_1_1/sources",
      "/report-root/at_18_Feb_2021_at_13_01_02/report/screenshots/scenario_ordinal_1_1/sources/1_ON_EXIT_PAGE.txt",
      "/report-root/at_18_Feb_2021_at_13_01_02/report/screenshots/scenario_ordinal_1_1/withNoHtml",
      "/report-root/at_18_Feb_2021_at_13_01_02/report/screenshots/scenario_ordinal_1_1/withNoHtml/1_ON_EXIT_PAGE.txt",
      "/report-root/at_18_Feb_2021_at_13_01_02/report/testsReport.json"
    ).sorted

    val runningScenarioTest1 = RunningScenario(ordinal = new Ordinal(1), test = "t1", feature = "f1", scenario = "s1")

    val runningScenarioTest2 = runningScenarioTest1.copy(ordinal = runningScenarioTest1.ordinal.next, test = "t2", scenario = "s2")

    val events = List(
      RunStarting(1),
      TestStarting(runningScenarioTest1, 2),
      Screenshot(runningScenarioTest1, ReferenceData.screenshotDriverData.url1, 2),
      TestSucceeded(runningScenarioTest1, RecordedEvents.empty, 3),
      TestStarting(runningScenarioTest2, 2),
      Screenshot(runningScenarioTest2, ReferenceData.screenshotDriverData.url1, 2),
      TestSucceeded(runningScenarioTest2, RecordedEvents.empty, 3),
      RunCompleted(4)
    )

    def rootReportInitializerMock(reportRoot: Path): RootReportInitializer[IO] = { () => TestsCollectorConfiguration.from[IO](now.toInstant, reportRoot) }

    def createRootPath(suffixDir: Path): IO[Path] =
      for {
        tempDir <- Files.forIO.createTempDirectory
        rootPath = tempDir / suffixDir
        _ <- Files.forIO.createDirectories(rootPath)
        exists <- Files.forIO.exists(rootPath)
        result <- if (exists) IO(rootPath) else IO.raiseError(new RuntimeException(s"$rootPath does not exist"))
      } yield result

    val actualFileGenerated: IO[List[String]] = for {
      rootPath <- createRootPath(reportDestinationDirPath)
      rootReportInitializerMock <- IO(rootReportInitializerMock(rootPath))
      underTest <- IO(new ReportBuilderImpl[IO](rootReportInitializerMock, new StreamReportJsonBuilder[IO](), new TestsReportBuilderImpl))
      _ <- underTest.build(events)
      filesGenerated <- readAllFilesFromDirAndRemoveThePrefix(rootPath)
    } yield filesGenerated

    assertIO(obtained = actualFileGenerated, expectedFilesToBeGenerated)
  }

  test("given 2 tests with duplicated ordinal, we get a nice error") {

    val runningScenarioTest1 = RunningScenario(ordinal = new Ordinal(1), test = "t1", feature = "f1", scenario = "s1")

    val runningScenarioTest2 = runningScenarioTest1.copy(test = "t2", scenario = "s2")

    val events = List(
      RunStarting(1),
      TestStarting(runningScenarioTest1, 2),
      TestSucceeded(runningScenarioTest1, RecordedEvents.empty, 3),
      TestStarting(runningScenarioTest2, 2),
      TestSucceeded(runningScenarioTest2, RecordedEvents.empty, 3),
      RunCompleted(4)
    )

    def notUsedRootReportInitializer: RootReportInitializer[IO] = () => IO.raiseError(new RuntimeException("this code should not be exercised"))

    val actualResult: IO[Either[String, Unit]] = for {
      underTest <- IO(new ReportBuilderImpl[IO](notUsedRootReportInitializer, new StreamReportJsonBuilder[IO](), new TestsReportBuilderImpl))
      result <- underTest
        .build(events)
        .attempt
        .map(_.leftMap(_.toString))
    } yield result

    assertIO(obtained = actualResult, "java.lang.RuntimeException: there are some duplication in test ids list t_1_0,t_1_0".asLeft)
  }

  private def readAllFilesFromDirAndRemoveThePrefix(root: Path): IO[List[String]] = {
    root.parent.get.toString
    Files[IO]
      .walk(root)
      .map(_.toString)
      .map(_.replaceAll(root.parent.get.toString, ""))
      .compile
      .toList
      .map(_.sorted)
  }
}
