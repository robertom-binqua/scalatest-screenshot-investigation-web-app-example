package org.binqua.scalatest.reporter

import cats.implicits.catsSyntaxEitherId
import munit.FunSuite
import org.binqua.scalatest.reporter.StateEvent.{RecordedEvent, RecordedEvents}
import org.binqua.scalatest.reporter.TestUtil.Assertions
import org.binqua.scalatest.reporter.util.utils.EitherOps
import org.scalatest.events.Ordinal

import java.nio.file.{Files, Path}

class TestsCollectorImpSpec extends FunSuite {

  private val testReportFileLocation = "report/testsReport.json"

  def reportFileUtilsInitializerFromTempDir(reportParentDir: Path): ReportInitializer = () =>
    new ReportFileUtilsImpl(
      config = TestsCollectorConfiguration.unsafeFrom(reportParentDir.toFile)
    ).asRight

  test("testsCollector create report dir, screenshots dir and testsReport.js") {

    val reportParentDir: Path = Files.createTempDirectory("tempDir")

    val testsCollectorImpl = new TestsCollectorImpl(reportFileUtilsInitializerFromTempDir(reportParentDir))

    testsCollectorImpl.add(StateEvent.RunStarting(timestamp = 1L))

    val runningScenario = RunningScenario(new Ordinal(1), test = "t", feature = "f", scenario = "s")
    testsCollectorImpl.add(StateEvent.TestStarting(runningScenario = runningScenario, timestamp = 1L))

    testsCollectorImpl.add(StateEvent.Note(runningScenario = runningScenario, "take screenshot now", None, timestamp = 3L))
    testsCollectorImpl.addScreenshot(ReferenceData.screenshotDriverData.url1)

    testsCollectorImpl.add(
      StateEvent.TestSucceeded(
        runningScenario = runningScenario,
        RecordedEvents.from(List(RecordedEvent(new Ordinal(122), "m", None, 5L))).getOrThrow,
        timestamp = 2L
      )
    )

    val reportRoot: Path = Path.of(reportParentDir.toString, "report")
    Assertions.pathExist(reportRoot)

    val screenshots: Path = Path.of(reportRoot.toString, "screenshots")
    Assertions.pathExist(screenshots)

    testsCollectorImpl.createReport()

    Assertions.pathExist(Path.of(reportRoot.toString, "testsReport.json"))

  }

  test("we can add 2 tests with 1 and 2 screenshots each") {

    val reportParentDir: Path = Files.createTempDirectory("tempDir")

    val testsCollectorImpl = new TestsCollectorImpl(reportFileUtilsInitializerFromTempDir(reportParentDir))

    testsCollectorImpl.add(StateEvent.RunStarting(timestamp = 1L))

    val runningScenario = RunningScenario(new Ordinal(1), test = "t", feature = "f", scenario = "s")
    testsCollectorImpl.add(StateEvent.TestStarting(runningScenario = runningScenario, timestamp = 1L))

    testsCollectorImpl.add(StateEvent.Note(runningScenario = runningScenario, "take screenshot now 1", None, timestamp = 3L))
    testsCollectorImpl.addScreenshot(ReferenceData.screenshotDriverData.url1)

    testsCollectorImpl.add(
      StateEvent.TestSucceeded(
        runningScenario = runningScenario,
        RecordedEvents.from(List(RecordedEvent(new Ordinal(122), "m", None, 5L))).getOrThrow,
        timestamp = 2L
      )
    )

    val newRunningScenario = runningScenario.copy(test = "t1", ordinal = runningScenario.ordinal.next)

    testsCollectorImpl.add(StateEvent.TestStarting(runningScenario = newRunningScenario, timestamp = 3L))

    testsCollectorImpl.add(StateEvent.Note(runningScenario = newRunningScenario, "take screenshot now 1", None, timestamp = 6L))
    testsCollectorImpl.addScreenshot(ReferenceData.screenshotDriverData.url1)

    testsCollectorImpl.add(StateEvent.Note(runningScenario = newRunningScenario, "take screenshot now 2", None, timestamp = 7L))
    testsCollectorImpl.addScreenshot(ReferenceData.screenshotDriverData.url2)

    val stringOrEvents = RecordedEvents.from(List(RecordedEvent(new Ordinal(122), "m", None, 5L))).getOrThrow
    testsCollectorImpl.add(StateEvent.TestSucceeded(runningScenario = newRunningScenario, stringOrEvents, timestamp = 4L))

    testsCollectorImpl.createReport()

    val expectedContent =
      """{
        |  "screenshotsLocationPrefix" : "report/screenshots/",
        |  "testsReport" : [
        |    {
        |      "name" : "t",
        |      "id" : "t_1_0",
        |      "features" : [
        |        {
        |          "description" : "f",
        |          "id" : "f_1_0",
        |          "scenarios" : [
        |            {
        |              "id" : "s_1_0",
        |              "description" : "s",
        |              "startedTimestamp" : 1,
        |              "finishedTimestamp" : 2,
        |              "screenshots" : [
        |                {
        |                  "originalLocation" : "scenario_ordinal_1_0/original/1_ON_EXIT_PAGE.png",
        |                  "sourceLocation" : "scenario_ordinal_1_0/sources/1_ON_EXIT_PAGE.txt",
        |                  "sourceWithNoHtmlLocation" : "scenario_ordinal_1_0/withNoHtml/1_ON_EXIT_PAGE.txt",
        |                  "pageUrl" : "url1",
        |                  "index" : 1,
        |                  "pageTitle" : "title 1",
        |                  "screenshotMoment" : "ON_EXIT_PAGE"
        |                }
        |              ],
        |              "steps" : [
        |                {
        |                  "message" : "m",
        |                  "timestamp" : 5,
        |                  "id" : "st_122_0"
        |                }
        |              ],
        |              "testOutcome" : "succeeded"
        |            }
        |          ]
        |        }
        |      ]
        |    },
        |    {
        |      "name" : "t1",
        |      "id" : "t_1_1",
        |      "features" : [
        |        {
        |          "description" : "f",
        |          "id" : "f_1_1",
        |          "scenarios" : [
        |            {
        |              "id" : "s_1_1",
        |              "description" : "s",
        |              "startedTimestamp" : 3,
        |              "finishedTimestamp" : 4,
        |              "screenshots" : [
        |                {
        |                  "originalLocation" : "scenario_ordinal_1_1/original/1_ON_EXIT_PAGE.png",
        |                  "sourceLocation" : "scenario_ordinal_1_1/sources/1_ON_EXIT_PAGE.txt",
        |                  "sourceWithNoHtmlLocation" : "scenario_ordinal_1_1/withNoHtml/1_ON_EXIT_PAGE.txt",
        |                  "pageUrl" : "url1",
        |                  "index" : 1,
        |                  "pageTitle" : "title 1",
        |                  "screenshotMoment" : "ON_EXIT_PAGE"
        |                },
        |                {
        |                  "originalLocation" : "scenario_ordinal_1_1/original/2_ON_ENTER_PAGE.png",
        |                  "sourceLocation" : "scenario_ordinal_1_1/sources/2_ON_ENTER_PAGE.txt",
        |                  "sourceWithNoHtmlLocation" : "scenario_ordinal_1_1/withNoHtml/2_ON_ENTER_PAGE.txt",
        |                  "pageUrl" : "url2",
        |                  "index" : 2,
        |                  "pageTitle" : "title 2",
        |                  "screenshotMoment" : "ON_ENTER_PAGE"
        |                }
        |              ],
        |              "steps" : [
        |                {
        |                  "message" : "m",
        |                  "timestamp" : 5,
        |                  "id" : "st_122_0"
        |                }
        |              ],
        |              "testOutcome" : "succeeded"
        |            }
        |          ]
        |        }
        |      ]
        |    }
        |  ]
        |}""".stripMargin

    val actualReportJsFile = Path.of(reportParentDir.toFile.getAbsolutePath, testReportFileLocation).toFile
    assertEquals(Files.readString(actualReportJsFile.toPath), expectedContent)

    List(
      "scenario_ordinal_1_0/original/1_ON_EXIT_PAGE.png",
      "scenario_ordinal_1_0/sources/1_ON_EXIT_PAGE.txt",
      "scenario_ordinal_1_1/original/2_ON_ENTER_PAGE.png",
      "scenario_ordinal_1_1/sources/2_ON_ENTER_PAGE.txt",
      "scenario_ordinal_1_1/original/1_ON_EXIT_PAGE.png",
      "scenario_ordinal_1_1/sources/1_ON_EXIT_PAGE.txt"
    )
      .map(suffix => s"report/screenshots/$suffix")
      .foreach(f => Assertions.pathExist(reportParentDir.resolve(f)))

  }

  test("we can add 1 test with 2 notes e 2 recorded events ....") {

    val reportParentDir: Path = Files.createTempDirectory("tempDir")

    val testsCollectorImpl = new TestsCollectorImpl(reportFileUtilsInitializerFromTempDir(reportParentDir))

    testsCollectorImpl.add(StateEvent.RunStarting(timestamp = 1L))

    val runningScenario = RunningScenario(new Ordinal(1), test = "t", feature = "f", scenario = "s")
    testsCollectorImpl.add(StateEvent.TestStarting(runningScenario = runningScenario, timestamp = 1L))

    testsCollectorImpl.add(StateEvent.Note(runningScenario = runningScenario, "take screenshot now", None, timestamp = 3L))
    testsCollectorImpl.addScreenshot(ReferenceData.screenshotDriverData.url1)

    testsCollectorImpl.add(StateEvent.Note(runningScenario = runningScenario, "this is a note 1", None, timestamp = 2L))
    testsCollectorImpl.add(StateEvent.Note(runningScenario = runningScenario, "this is a note 2", None, timestamp = 3L))

    testsCollectorImpl.add(
      StateEvent.TestSucceeded(
        runningScenario = runningScenario,
        RecordedEvents.from(List(RecordedEvent(new Ordinal(122), "m", None, 5L))).getOrThrow,
        timestamp = 2L
      )
    )

    testsCollectorImpl.createReport()

    val expectedContent = """{
                            |  "screenshotsLocationPrefix" : "report/screenshots/",
                            |  "testsReport" : [
                            |    {
                            |      "name" : "t",
                            |      "id" : "t_1_0",
                            |      "features" : [
                            |        {
                            |          "description" : "f",
                            |          "id" : "f_1_0",
                            |          "scenarios" : [
                            |            {
                            |              "id" : "s_1_0",
                            |              "description" : "s",
                            |              "startedTimestamp" : 1,
                            |              "finishedTimestamp" : 2,
                            |              "screenshots" : [
                            |                {
                            |                  "originalLocation" : "scenario_ordinal_1_0/original/1_ON_EXIT_PAGE.png",
                            |                  "sourceLocation" : "scenario_ordinal_1_0/sources/1_ON_EXIT_PAGE.txt",
                            |                  "sourceWithNoHtmlLocation" : "scenario_ordinal_1_0/withNoHtml/1_ON_EXIT_PAGE.txt",
                            |                  "pageUrl" : "url1",
                            |                  "index" : 1,
                            |                  "pageTitle" : "title 1",
                            |                  "screenshotMoment" : "ON_EXIT_PAGE"
                            |                }
                            |              ],
                            |              "steps" : [
                            |                {
                            |                  "message" : "this is a note 2",
                            |                  "timestamp" : 3,
                            |                  "id" : "st_1_0"
                            |                },
                            |                {
                            |                  "message" : "this is a note 1",
                            |                  "timestamp" : 2,
                            |                  "id" : "st_1_0"
                            |                },
                            |                {
                            |                  "message" : "m",
                            |                  "timestamp" : 5,
                            |                  "id" : "st_122_0"
                            |                }
                            |              ],
                            |              "testOutcome" : "succeeded"
                            |            }
                            |          ]
                            |        }
                            |      ]
                            |    }
                            |  ]
                            |}""".stripMargin

    val actualReportJsFile = Path.of(reportParentDir.toFile.getAbsolutePath, testReportFileLocation).toFile
    assertEquals(Files.readString(actualReportJsFile.toPath), expectedContent)

    List(
      "scenario_ordinal_1_0/original/1_ON_EXIT_PAGE.png",
      "scenario_ordinal_1_0/sources/1_ON_EXIT_PAGE.txt"
    ).map(suffix => s"report/screenshots/$suffix")
      .foreach(f => Assertions.pathExist(reportParentDir.resolve(f)))
  }

  test("we can add 1 test with no screenshot, and the report json it will be correct") {

    val reportParentDir: Path = Files.createTempDirectory("tempDir")

    val testsCollectorImpl = new TestsCollectorImpl(reportFileUtilsInitializerFromTempDir(reportParentDir))

    testsCollectorImpl.add(StateEvent.RunStarting(timestamp = 1L))

    val runningScenario = RunningScenario(new Ordinal(1), test = "t", feature = "f", scenario = "s")
    testsCollectorImpl.add(StateEvent.TestStarting(runningScenario = runningScenario, timestamp = 1L))
    testsCollectorImpl.add(StateEvent.Note(runningScenario = runningScenario, "this is a note 1", None, timestamp = 2L))
    testsCollectorImpl.add(StateEvent.Note(runningScenario = runningScenario, "this is a note 2", None, timestamp = 3L))

    testsCollectorImpl.add(
      StateEvent.TestSucceeded(
        runningScenario = runningScenario,
        RecordedEvents.from(List(RecordedEvent(new Ordinal(122), "m", None, 5L))).getOrThrow,
        timestamp = 2L
      )
    )

    testsCollectorImpl.createReport()

    val expectedContent = """{
                            |  "screenshotsLocationPrefix" : "report/screenshots/",
                            |  "testsReport" : [
                            |    {
                            |      "name" : "t",
                            |      "id" : "t_1_0",
                            |      "features" : [
                            |        {
                            |          "description" : "f",
                            |          "id" : "f_1_0",
                            |          "scenarios" : [
                            |            {
                            |              "id" : "s_1_0",
                            |              "description" : "s",
                            |              "startedTimestamp" : 1,
                            |              "finishedTimestamp" : 2,
                            |              "screenshots" : [
                            |              ],
                            |              "steps" : [
                            |                {
                            |                  "message" : "this is a note 2",
                            |                  "timestamp" : 3,
                            |                  "id" : "st_1_0"
                            |                },
                            |                {
                            |                  "message" : "this is a note 1",
                            |                  "timestamp" : 2,
                            |                  "id" : "st_1_0"
                            |                },
                            |                {
                            |                  "message" : "m",
                            |                  "timestamp" : 5,
                            |                  "id" : "st_122_0"
                            |                }
                            |              ],
                            |              "testOutcome" : "succeeded"
                            |            }
                            |          ]
                            |        }
                            |      ]
                            |    }
                            |  ]
                            |}""".stripMargin

    val actualReportJsFile = Path.of(reportParentDir.toFile.getAbsolutePath, testReportFileLocation).toFile
    assertEquals(Files.readString(actualReportJsFile.toPath), expectedContent)
  }

}
