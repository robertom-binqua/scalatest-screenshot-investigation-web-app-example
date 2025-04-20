package org.binqua.examples.http4sapp.app

import munit.FunSuite
import org.scalatest.events.Ordinal

import java.io.File
import java.nio.file.{Files, Path}

class TestsCollectorImpSpec extends FunSuite {

  test("we can add 2 tests with 1 screenshot each") {

    // tempDir/report
    // tempDir/screenshotsRoot
    val tempDir: Path = Files.createTempDirectory("tempDir")

    val reportRoot: File = Files.createDirectory(tempDir.resolve("report")).toFile
    val screenshotsRoot: File = Files.createDirectory(tempDir.resolve("screenshotsRoot")).toFile

    val testsCollectorImpl = new TestsCollectorImpl(TestsCollectorConfiguration.unsafeFrom(reportRoot, screenshotsRoot))

    val runningScenario = RunningScenario(new Ordinal(1), test = "t", feature = "f", scenario = "s")
    testsCollectorImpl.add(StateEvent.TestStarting(runningScenario = runningScenario, timestamp = 1L))
    testsCollectorImpl.addScreenshotOnEnterAt(aDummyScreenshot, "url1")
    testsCollectorImpl.add(StateEvent.TestSucceeded(runningScenario = runningScenario, timestamp = 2L))

    val newRunningScenario = runningScenario.copy(test = "t1", ordinal = runningScenario.ordinal.next)

    testsCollectorImpl.add(StateEvent.TestStarting(runningScenario = newRunningScenario, timestamp = 3L))
    testsCollectorImpl.addScreenshotOnEnterAt(aDummyScreenshot, "url1")
    testsCollectorImpl.addScreenshotOnExitAt(aDummyScreenshot, "url2")
    testsCollectorImpl.add(StateEvent.TestSucceeded(runningScenario = newRunningScenario, timestamp = 4L))

    assertEquals(screenshotsRoot.exists(), true)

    testsCollectorImpl.createReport()

    val actualReportJsonFile = new File(reportRoot.getAbsolutePath + File.separator + "report.json")

    assertEquals(actualReportJsonFile.exists(), true)

    val expectedContent = """[
                            |  {
                            |    "name" : "t",
                            |    "features" : [
                            |      {
                            |        "description" : "f",
                            |        "scenarios" : [
                            |          {
                            |            "ordinal" : "1_0",
                            |            "description" : "s",
                            |            "startedTimestamp" : 1,
                            |            "finishedTimestamp" : 2,
                            |            "screenshots" : [
                            |              {
                            |                "location" : "scenario_ordinal_1_0/screenshot_1_ON_ENTER_PAGE.png",
                            |                "pageUrl" : "url1",
                            |                "index" : 1,
                            |                "screenshotMoment" : "ON_ENTER_PAGE"
                            |              }
                            |            ],
                            |            "testOutcome" : "succeeded"
                            |          }
                            |        ]
                            |      }
                            |    ]
                            |  },
                            |  {
                            |    "name" : "t1",
                            |    "features" : [
                            |      {
                            |        "description" : "f",
                            |        "scenarios" : [
                            |          {
                            |            "ordinal" : "1_1",
                            |            "description" : "s",
                            |            "startedTimestamp" : 3,
                            |            "finishedTimestamp" : 4,
                            |            "screenshots" : [
                            |              {
                            |                "location" : "scenario_ordinal_1_1/screenshot_2_ON_EXIT_PAGE.png",
                            |                "pageUrl" : "url2",
                            |                "index" : 2,
                            |                "screenshotMoment" : "ON_EXIT_PAGE"
                            |              },
                            |              {
                            |                "location" : "scenario_ordinal_1_1/screenshot_1_ON_ENTER_PAGE.png",
                            |                "pageUrl" : "url1",
                            |                "index" : 1,
                            |                "screenshotMoment" : "ON_ENTER_PAGE"
                            |              }
                            |            ],
                            |            "testOutcome" : "succeeded"
                            |          }
                            |        ]
                            |      }
                            |    ]
                            |  }
                            |]""".stripMargin

    assertEquals(Files.readString(actualReportJsonFile.toPath), expectedContent)

    assertEquals(new File(screenshotsRoot.getAbsolutePath + replaceWithFileSeparator("/scenario_ordinal_1_0/screenshot_1_ON_ENTER_PAGE.png")).exists(), true)
    assertEquals(new File(screenshotsRoot.getAbsolutePath + replaceWithFileSeparator("/scenario_ordinal_1_1/screenshot_1_ON_ENTER_PAGE.png")).exists(), true)
    assertEquals(new File(screenshotsRoot.getAbsolutePath + replaceWithFileSeparator("/scenario_ordinal_1_1/screenshot_2_ON_EXIT_PAGE.png")).exists(), true)

  }
  test("we can add 1 test with 2 notes e 2 re....") {

    // tempDir/report
    // tempDir/screenshotsRoot
    val tempDir: Path = Files.createTempDirectory("tempDir")

    val reportRoot: File = Files.createDirectory(tempDir.resolve("report")).toFile
    val screenshotsRoot: File = Files.createDirectory(tempDir.resolve("screenshotsRoot")).toFile

    val testsCollectorImpl = new TestsCollectorImpl(TestsCollectorConfiguration.unsafeFrom(reportRoot, screenshotsRoot))

    val runningScenario = RunningScenario(new Ordinal(1), test = "t", feature = "f", scenario = "s")
    testsCollectorImpl.add(StateEvent.TestStarting(runningScenario = runningScenario, timestamp = 1L))
    testsCollectorImpl.addScreenshotOnEnterAt(aDummyScreenshot, "url1")

    testsCollectorImpl.add(StateEvent.Note(runningScenario = runningScenario, "this is a note 1", None, timestamp = 2L))
    testsCollectorImpl.add(StateEvent.Note(runningScenario = runningScenario, "this is a note 2", None, timestamp = 3L))

    testsCollectorImpl.add(StateEvent.TestSucceeded(runningScenario = runningScenario, timestamp = 2L))

    assertEquals(screenshotsRoot.exists(), true)

    testsCollectorImpl.createReport()

    val actualReportJsonFile = new File(reportRoot.getAbsolutePath + File.separator + "report.json")

    assertEquals(actualReportJsonFile.exists(), true)

    val expectedContent = """[
                            |  {
                            |    "name" : "t",
                            |    "features" : [
                            |      {
                            |        "description" : "f",
                            |        "scenarios" : [
                            |          {
                            |            "ordinal" : "1_0",
                            |            "description" : "s",
                            |            "startedTimestamp" : 1,
                            |            "finishedTimestamp" : 2,
                            |            "screenshots" : [
                            |              {
                            |                "location" : "scenario_ordinal_1_0/screenshot_1_ON_ENTER_PAGE.png",
                            |                "pageUrl" : "url1",
                            |                "index" : 1,
                            |                "screenshotMoment" : "ON_ENTER_PAGE"
                            |              }
                            |            ],
                            |            "steps" : [
                            |              {
                            |                "message" : "this is a note 2",
                            |                "timestamp" : 3,
                            |                "ordinal" : "1_0"
                            |              },
                            |              {
                            |                "message" : "this is a note 1",
                            |                "timestamp" : 2,
                            |                "ordinal" : "1_0"
                            |              }
                            |            ],
                            |            "testOutcome" : "succeeded"
                            |          }
                            |        ]
                            |      }
                            |    ]
                            |  }
                            |]""".stripMargin

    assertEquals(Files.readString(actualReportJsonFile.toPath), expectedContent)

    assertEquals(new File(screenshotsRoot.getAbsolutePath + replaceWithFileSeparator("/scenario_ordinal_1_0/screenshot_1_ON_ENTER_PAGE.png")).exists(), true)

  }

  private def aDummyScreenshot =
    Files.createTempFile("doesNotMatter", "png").toFile

  private def replaceWithFileSeparator(withForwardSlash: String): String = withForwardSlash.replaceAll("/", File.separator)
}
