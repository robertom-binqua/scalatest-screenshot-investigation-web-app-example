package org.binqua.examples.http4sapp.app

import munit.FunSuite
import org.scalatest.events.Ordinal

import java.io.File
import java.nio.file.Files

class TestsCollectorImpSpec extends FunSuite {

  test("we can add 2 tests with 1 screenshot each") {

    val runningScenario = RunningScenario(new Ordinal(1), "t", "f", "s")

    val report = Files.createTempDirectory("report").toFile
    val screenshots = Files.createTempDirectory("screenshots").toFile

    val testsCollectorImpl = new TestsCollectorImpl(TestsCollectorConfiguration.unsafeFrom(report, screenshots))

    testsCollectorImpl.add(StateEvent.TestStarting(runningScenario = runningScenario, timestamp = 1L))
    testsCollectorImpl.addScreenshotOnEnterAt(Files.createTempFile("doesNotMatter", "png").toFile, "url1")
    testsCollectorImpl.add(StateEvent.TestSucceeded(runningScenario = runningScenario, timestamp = 2L))

    val newRunningScenario = runningScenario.copy(test = "t1", ordinal = runningScenario.ordinal.next)
    testsCollectorImpl.add(StateEvent.TestStarting(runningScenario = newRunningScenario, timestamp = 3L))
    testsCollectorImpl.addScreenshotOnEnterAt(Files.createTempFile("doesNotMatter", "png").toFile, "url1")
    testsCollectorImpl.add(StateEvent.TestSucceeded(runningScenario = newRunningScenario, timestamp = 4L))

    testsCollectorImpl.createReport()

    val actualReportJsonFile = new File(report.getAbsolutePath + File.separator + "report.json")
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
                            |                "location" : "scenario_ordinal_1_0/screenshot_1_ON_ENTER_PAGE.png"
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
                            |                "location" : "scenario_ordinal_1_1/screenshot_1_ON_ENTER_PAGE.png"
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

  }
}
