package org.binqua.examples.http4sapp.app

import munit.FunSuite
import org.binqua.examples.http4sapp.ImageResizer
import org.binqua.examples.http4sapp.app.StateEvent.{RecordedEvent, RecordedEvents}
import org.binqua.examples.http4sapp.app.TestUtil.assertPathExist
import org.binqua.examples.http4sapp.util.utils.EitherOps
import org.scalatest.events.Ordinal

import java.io.File
import java.nio.file.{Files, Path}

class TestsCollectorImpSpec extends FunSuite {

  val dummyResizer: ImageResizer = (inputPath: File, outputPath: File, _: Int) => Files.copy(inputPath.toPath, outputPath.toPath)

  test("testsCollector create report dir, screenshots dir and testsReport.js") {
    val reportParentDir: Path = Files.createTempDirectory("tempDir")
    val configuration: TestsCollectorConfiguration = TestsCollectorConfiguration.unsafeFrom(reportParentDir.toFile)

    val testsCollectorImpl = new TestsCollectorImpl(
      new ReportFileUtilsImpl(
        config = configuration,
        imageResizer = dummyResizer
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
        config = configuration,
        imageResizer = dummyResizer
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
                            |                  "originalLocation" : "scenario_ordinal_1_0/original/1_ON_ENTER_PAGE.png",
                            |                  "resizedLocation" : "scenario_ordinal_1_0/resized/1_ON_ENTER_PAGE.png",
                            |                  "sourceLocation" : "scenario_ordinal_1_0/sources/1_ON_ENTER_PAGE.txt",
                            |                  "pageUrl" : "url1",
                            |                  "index" : 1,
                            |                  "screenshotMoment" : "ON_ENTER_PAGE"
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
                            |                  "originalLocation" : "scenario_ordinal_1_1/original/1_ON_ENTER_PAGE.png",
                            |                  "resizedLocation" : "scenario_ordinal_1_1/resized/1_ON_ENTER_PAGE.png",
                            |                  "sourceLocation" : "scenario_ordinal_1_1/sources/1_ON_ENTER_PAGE.txt",
                            |                  "pageUrl" : "url1",
                            |                  "index" : 1,
                            |                  "screenshotMoment" : "ON_ENTER_PAGE"
                            |                },
                            |                {
                            |                  "originalLocation" : "scenario_ordinal_1_1/original/2_ON_EXIT_PAGE.png",
                            |                  "resizedLocation" : "scenario_ordinal_1_1/resized/2_ON_EXIT_PAGE.png",
                            |                  "sourceLocation" : "scenario_ordinal_1_1/sources/2_ON_EXIT_PAGE.txt",
                            |                  "pageUrl" : "url1",
                            |                  "index" : 2,
                            |                  "screenshotMoment" : "ON_EXIT_PAGE"
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
      "scenario_ordinal_1_0/original/1_ON_ENTER_PAGE.png",
      "scenario_ordinal_1_0/resized/1_ON_ENTER_PAGE.png",
      "scenario_ordinal_1_0/sources/1_ON_ENTER_PAGE.txt",
      "scenario_ordinal_1_1/original/2_ON_EXIT_PAGE.png",
      "scenario_ordinal_1_1/resized/2_ON_EXIT_PAGE.png",
      "scenario_ordinal_1_1/sources/2_ON_EXIT_PAGE.txt",
      "scenario_ordinal_1_1/original/1_ON_ENTER_PAGE.png",
      "scenario_ordinal_1_1/resized/1_ON_ENTER_PAGE.png",
      "scenario_ordinal_1_1/sources/1_ON_ENTER_PAGE.txt"
    )
      .map(suffix => s"report/screenshots/$suffix")
      .foreach(f => assertPathExist(reportParentDir.resolve(f)))

  }

  test("we can add 1 test with 2 notes e 2 recorded events ....") {

    val reportParentDir: Path = Files.createTempDirectory("tempDir")
    val configuration: TestsCollectorConfiguration = TestsCollectorConfiguration.unsafeFrom(reportParentDir.toFile)

    val testsCollectorImpl = new TestsCollectorImpl(new ReportFileUtilsImpl(configuration, dummyResizer))

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
                            |                  "originalLocation" : "scenario_ordinal_1_0/original/1_ON_ENTER_PAGE.png",
                            |                  "resizedLocation" : "scenario_ordinal_1_0/resized/1_ON_ENTER_PAGE.png",
                            |                  "sourceLocation" : "scenario_ordinal_1_0/sources/1_ON_ENTER_PAGE.txt",
                            |                  "pageUrl" : "url1",
                            |                  "index" : 1,
                            |                  "screenshotMoment" : "ON_ENTER_PAGE"
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
      "scenario_ordinal_1_0/original/1_ON_ENTER_PAGE.png",
      "scenario_ordinal_1_0/resized/1_ON_ENTER_PAGE.png",
      "scenario_ordinal_1_0/sources/1_ON_ENTER_PAGE.txt"
    ).map(suffix => s"report/screenshots/$suffix")
      .foreach(f => assertPathExist(reportParentDir.resolve(f)))
  }

  test("we can add 1 test with no screenshot, and the report json it will be correct") {

    val reportParentDir: Path = Files.createTempDirectory("tempDir")
    val configuration: TestsCollectorConfiguration = TestsCollectorConfiguration.unsafeFrom(reportParentDir.toFile)

    val testsCollectorImpl = new TestsCollectorImpl(new ReportFileUtilsImpl(configuration, dummyResizer))

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
