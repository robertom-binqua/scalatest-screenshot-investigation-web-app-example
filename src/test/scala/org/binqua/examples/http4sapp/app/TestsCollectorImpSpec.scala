package org.binqua.examples.http4sapp.app

import munit.FunSuite
import org.binqua.examples.http4sapp.app.TestUtil.assertPathExist
import org.binqua.scalatest.reporter.StateEvent.{RecordedEvent, RecordedEvents}
import org.binqua.scalatest.reporter.Utils.EitherOps
import org.binqua.scalatest.reporter._
import org.scalatest.events.Ordinal

import java.nio.file.{Files, Path}

class TestsCollectorImpSpec extends FunSuite {

  test("testsCollector create report dir, screenshots dir and testsReport.js") {
    val reportParentDir: Path = Files.createTempDirectory("tempDir")
    val configuration: TestsCollectorConfiguration = TestsCollectorConfiguration.unsafeFrom(reportParentDir.toFile)

    val testsCollectorImpl = new TestsCollectorImpl(
      new ReportFileUtilsImpl(
        config = configuration
      )
    )

    val runningScenario = RunningScenario(new Ordinal(1), test = "t", feature = "f", scenario = "s")
    testsCollectorImpl.add(StateEvent.TestStarting(runningScenario = runningScenario, timestamp = 1L))
    testsCollectorImpl.addScreenshotOnEnterAt(ReferenceData.screenshotDriverData)
    testsCollectorImpl.add(
      StateEvent.TestSucceeded(
        runningScenario = runningScenario,
        RecordedEvents.from(List(RecordedEvent(new Ordinal(122), "m", None, 5L))).getOrThrow,
        timestamp = 2L
      )
    )

    val reportRoot: Path = Path.of(reportParentDir.toString, "report")
    assertPathExist(reportRoot)

    val screenshots: Path = Path.of(reportRoot.toString, "screenshots")
    assertPathExist(screenshots)

    testsCollectorImpl.createReport()

    assertPathExist(Path.of(reportRoot.toString, "testsReport.js"))

  }

  test("we can add 2 tests with 1 screenshot each") {

    val reportParentDir: Path = Files.createTempDirectory("tempDir")

    val configuration: TestsCollectorConfiguration = TestsCollectorConfiguration.unsafeFrom(reportParentDir.toFile)

    val testsCollectorImpl = new TestsCollectorImpl(
      new ReportFileUtilsImpl(
        config = configuration
      )
    )

    val runningScenario = RunningScenario(new Ordinal(1), test = "t", feature = "f", scenario = "s")
    testsCollectorImpl.add(StateEvent.TestStarting(runningScenario = runningScenario, timestamp = 1L))

    testsCollectorImpl.addScreenshotOnEnterAt(ReferenceData.screenshotDriverData)

    testsCollectorImpl.add(
      StateEvent.TestSucceeded(
        runningScenario = runningScenario,
        RecordedEvents.from(List(RecordedEvent(new Ordinal(122), "m", None, 5L))).getOrThrow,
        timestamp = 2L
      )
    )

    val newRunningScenario = runningScenario.copy(test = "t1", ordinal = runningScenario.ordinal.next)

    testsCollectorImpl.add(StateEvent.TestStarting(runningScenario = newRunningScenario, timestamp = 3L))
    testsCollectorImpl.addScreenshotOnEnterAt(ReferenceData.screenshotDriverData)
    testsCollectorImpl.addScreenshotOnExitAt(ReferenceData.screenshotDriverData)
    val stringOrEvents = RecordedEvents.from(List(RecordedEvent(new Ordinal(122), "m", None, 5L))).getOrThrow
    testsCollectorImpl.add(StateEvent.TestSucceeded(runningScenario = newRunningScenario, stringOrEvents, timestamp = 4L))

    testsCollectorImpl.createReport()

    val expectedContent = """window.testsReport = {
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
                            |              "ordinal" : "1_0",
                            |              "description" : "s",
                            |              "startedTimestamp" : 1,
                            |              "finishedTimestamp" : 2,
                            |              "screenshots" : [
                            |                {
                            |                  "originalLocation" : "scenario_ordinal_1_0/original/1_ON_PAGE_ENTER.png",
                            |                  "resizedLocation" : "scenario_ordinal_1_0/resized/1_ON_PAGE_ENTER.png",
                            |                  "sourceLocation" : "scenario_ordinal_1_0/sources/1_ON_PAGE_ENTER.txt",
                            |                  "pageUrl" : "url1",
                            |                  "index" : 1,
                            |                  "screenshotMoment" : "ON_PAGE_ENTER"
                            |                }
                            |              ],
                            |              "steps" : [
                            |                {
                            |                  "message" : "m",
                            |                  "timestamp" : 5,
                            |                  "ordinal" : "122_0"
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
                            |              "ordinal" : "1_1",
                            |              "description" : "s",
                            |              "startedTimestamp" : 3,
                            |              "finishedTimestamp" : 4,
                            |              "screenshots" : [
                            |                {
                            |                  "originalLocation" : "scenario_ordinal_1_1/original/1_ON_PAGE_ENTER.png",
                            |                  "resizedLocation" : "scenario_ordinal_1_1/resized/1_ON_PAGE_ENTER.png",
                            |                  "sourceLocation" : "scenario_ordinal_1_1/sources/1_ON_PAGE_ENTER.txt",
                            |                  "pageUrl" : "url1",
                            |                  "index" : 1,
                            |                  "screenshotMoment" : "ON_PAGE_ENTER"
                            |                },
                            |                {
                            |                  "originalLocation" : "scenario_ordinal_1_1/original/2_ON_PAGE_EXIT.png",
                            |                  "resizedLocation" : "scenario_ordinal_1_1/resized/2_ON_PAGE_EXIT.png",
                            |                  "sourceLocation" : "scenario_ordinal_1_1/sources/2_ON_PAGE_EXIT.txt",
                            |                  "pageUrl" : "url1",
                            |                  "index" : 2,
                            |                  "screenshotMoment" : "ON_PAGE_EXIT"
                            |                }
                            |              ],
                            |              "steps" : [
                            |                {
                            |                  "message" : "m",
                            |                  "timestamp" : 5,
                            |                  "ordinal" : "122_0"
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

    val actualReportJsFile = Path.of(reportParentDir.toFile.getAbsolutePath, "report/testsReport.js").toFile
    assertEquals(Files.readString(actualReportJsFile.toPath), expectedContent)

    List(
      "scenario_ordinal_1_0/original/1_ON_PAGE_ENTER.png",
      "scenario_ordinal_1_0/sources/1_ON_PAGE_ENTER.txt",
      "scenario_ordinal_1_1/original/2_ON_PAGE_EXIT.png",
      "scenario_ordinal_1_1/sources/2_ON_PAGE_EXIT.txt",
      "scenario_ordinal_1_1/original/1_ON_PAGE_ENTER.png",
      "scenario_ordinal_1_1/sources/1_ON_PAGE_ENTER.txt"
    )
      .map(suffix => s"report/screenshots/$suffix")
      .foreach(f => assertPathExist(reportParentDir.resolve(f)))

  }

  test("we can add 1 test with 2 notes e 2 recorded events ....") {

    val reportParentDir: Path = Files.createTempDirectory("tempDir")
    val configuration: TestsCollectorConfiguration = TestsCollectorConfiguration.unsafeFrom(reportParentDir.toFile)

    val testsCollectorImpl = new TestsCollectorImpl(new ReportFileUtilsImpl(configuration))

    val runningScenario = RunningScenario(new Ordinal(1), test = "t", feature = "f", scenario = "s")
    testsCollectorImpl.add(StateEvent.TestStarting(runningScenario = runningScenario, timestamp = 1L))
    testsCollectorImpl.addScreenshotOnEnterAt(ReferenceData.screenshotDriverData)

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

    val expectedContent = """window.testsReport = {
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
                            |              "ordinal" : "1_0",
                            |              "description" : "s",
                            |              "startedTimestamp" : 1,
                            |              "finishedTimestamp" : 2,
                            |              "screenshots" : [
                            |                {
                            |                  "originalLocation" : "scenario_ordinal_1_0/original/1_ON_PAGE_ENTER.png",
                            |                  "resizedLocation" : "scenario_ordinal_1_0/resized/1_ON_PAGE_ENTER.png",
                            |                  "sourceLocation" : "scenario_ordinal_1_0/sources/1_ON_PAGE_ENTER.txt",
                            |                  "pageUrl" : "url1",
                            |                  "index" : 1,
                            |                  "screenshotMoment" : "ON_PAGE_ENTER"
                            |                }
                            |              ],
                            |              "steps" : [
                            |                {
                            |                  "message" : "this is a note 2",
                            |                  "timestamp" : 3,
                            |                  "ordinal" : "1_0"
                            |                },
                            |                {
                            |                  "message" : "this is a note 1",
                            |                  "timestamp" : 2,
                            |                  "ordinal" : "1_0"
                            |                },
                            |                {
                            |                  "message" : "m",
                            |                  "timestamp" : 5,
                            |                  "ordinal" : "122_0"
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

    val actualReportJsFile = Path.of(reportParentDir.toFile.getAbsolutePath, "report/testsReport.js").toFile
    assertEquals(Files.readString(actualReportJsFile.toPath), expectedContent)

    List(
      "scenario_ordinal_1_0/original/1_ON_PAGE_ENTER.png",
      "scenario_ordinal_1_0/sources/1_ON_PAGE_ENTER.txt"
    ).map(suffix => s"report/screenshots/$suffix")
      .foreach(f => assertPathExist(reportParentDir.resolve(f)))
  }

  test("we can add 1 test with no screenshot, and the report json it will be correct") {

    val reportParentDir: Path = Files.createTempDirectory("tempDir")
    val configuration: TestsCollectorConfiguration = TestsCollectorConfiguration.unsafeFrom(reportParentDir.toFile)

    val testsCollectorImpl = new TestsCollectorImpl(new ReportFileUtilsImpl(configuration))

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

    val expectedContent = """window.testsReport = {
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
                            |              "ordinal" : "1_0",
                            |              "description" : "s",
                            |              "startedTimestamp" : 1,
                            |              "finishedTimestamp" : 2,
                            |              "screenshots" : [
                            |              ],
                            |              "steps" : [
                            |                {
                            |                  "message" : "this is a note 2",
                            |                  "timestamp" : 3,
                            |                  "ordinal" : "1_0"
                            |                },
                            |                {
                            |                  "message" : "this is a note 1",
                            |                  "timestamp" : 2,
                            |                  "ordinal" : "1_0"
                            |                },
                            |                {
                            |                  "message" : "m",
                            |                  "timestamp" : 5,
                            |                  "ordinal" : "122_0"
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

    val actualReportJsFile = Path.of(reportParentDir.toFile.getAbsolutePath, "report/testsReport.js").toFile
    assertEquals(Files.readString(actualReportJsFile.toPath), expectedContent)
  }

}
