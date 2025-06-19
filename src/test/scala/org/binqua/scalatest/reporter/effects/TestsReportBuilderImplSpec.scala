package org.binqua.scalatest.reporter.effects

import cats.effect.IO
import cats.implicits.catsSyntaxEitherId
import fs2.io.file.{Files, Path}
import io.circe.syntax.EncoderOps
import munit.CatsEffectSuite
import org.binqua.scalatest.reporter.StateEvent._
import org.binqua.scalatest.reporter.util.utils.EitherOps
import org.binqua.scalatest.reporter.{ReferenceData, RunningScenario, TestsReport, TestsReportBuilderImpl}
import org.scalatest.events.Ordinal

import java.time.Instant

class TestsReportBuilderImplSpec extends CatsEffectSuite {

  test("given a valid list of events without screenshots, we can build a test report") {

    val runningScenario = RunningScenario(new Ordinal(1), "t1", "f1", "s1")
    val t2 = runningScenario.copy(ordinal = runningScenario.ordinal.next, test = "t2")

    val events = List(
      RunStarting(1),
      TestStarting(runningScenario, 2),
      TestSucceeded(runningScenario, RecordedEvents.empty, 3),
      TestStarting(t2, 2),
      TestSucceeded(t2, RecordedEvents.empty, 3),
      RunCompleted(4)
    )

    val expectedJson = """{
                         |  "screenshotsLocationPrefix" : "report/screenshots",
                         |  "testsReport" : [
                         |    {
                         |      "name" : "t1",
                         |      "id" : "t_1_0",
                         |      "features" : [
                         |        {
                         |          "description" : "f1",
                         |          "id" : "f_1_0",
                         |          "scenarios" : [
                         |            {
                         |              "ordinal" : "s_1_0",
                         |              "description" : "s1",
                         |              "startedTimestamp" : 2,
                         |              "finishedTimestamp" : 3,
                         |              "screenshots" : [
                         |              ],
                         |              "testOutcome" : "succeeded"
                         |            }
                         |          ]
                         |        }
                         |      ]
                         |    },
                         |    {
                         |      "name" : "t2",
                         |      "id" : "t_1_1",
                         |      "features" : [
                         |        {
                         |          "description" : "f1",
                         |          "id" : "f_1_1",
                         |          "scenarios" : [
                         |            {
                         |              "ordinal" : "s_1_1",
                         |              "description" : "s1",
                         |              "startedTimestamp" : 2,
                         |              "finishedTimestamp" : 3,
                         |              "screenshots" : [
                         |              ],
                         |              "testOutcome" : "succeeded"
                         |            }
                         |          ]
                         |        }
                         |      ]
                         |    }
                         |  ]
                         |}""".stripMargin

    val actualTestReport: TestsReport = new TestsReportBuilderImpl().validateEvents(events).getOrThrow

    assertIO(calculateActualJson(actualTestReport), expectedJson)

  }

  test("given a valid list of events with screenshots, we can build a test report") {

    val runningScenario = RunningScenario(new Ordinal(1), "t1", "f1", "s1")
    val t2 = runningScenario.copy(ordinal = runningScenario.ordinal.next, test = "t2")

    val events = List(
      RunStarting(1),
      TestStarting(runningScenario, 2),
      Screenshot(runningScenario, ReferenceData.screenshotDriverData.url1, 2),
      TestSucceeded(runningScenario, RecordedEvents.empty, 3),
      TestStarting(t2, 2),
      Screenshot(t2, ReferenceData.screenshotDriverData.url1, 2),
      TestSucceeded(t2, RecordedEvents.empty, 3),
      RunCompleted(4)
    )

    val expectedJson = """{
                         |  "screenshotsLocationPrefix" : "report/screenshots",
                         |  "testsReport" : [
                         |    {
                         |      "name" : "t1",
                         |      "id" : "t_1_0",
                         |      "features" : [
                         |        {
                         |          "description" : "f1",
                         |          "id" : "f_1_0",
                         |          "scenarios" : [
                         |            {
                         |              "ordinal" : "s_1_0",
                         |              "description" : "s1",
                         |              "startedTimestamp" : 2,
                         |              "finishedTimestamp" : 3,
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
                         |              "testOutcome" : "succeeded"
                         |            }
                         |          ]
                         |        }
                         |      ]
                         |    },
                         |    {
                         |      "name" : "t2",
                         |      "id" : "t_1_1",
                         |      "features" : [
                         |        {
                         |          "description" : "f1",
                         |          "id" : "f_1_1",
                         |          "scenarios" : [
                         |            {
                         |              "ordinal" : "s_1_1",
                         |              "description" : "s1",
                         |              "startedTimestamp" : 2,
                         |              "finishedTimestamp" : 3,
                         |              "screenshots" : [
                         |                {
                         |                  "originalLocation" : "scenario_ordinal_1_1/original/1_ON_EXIT_PAGE.png",
                         |                  "sourceLocation" : "scenario_ordinal_1_1/sources/1_ON_EXIT_PAGE.txt",
                         |                  "sourceWithNoHtmlLocation" : "scenario_ordinal_1_1/withNoHtml/1_ON_EXIT_PAGE.txt",
                         |                  "pageUrl" : "url1",
                         |                  "index" : 1,
                         |                  "pageTitle" : "title 1",
                         |                  "screenshotMoment" : "ON_EXIT_PAGE"
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

    val actualTestReport: TestsReport = new TestsReportBuilderImpl().validateEvents(events).getOrThrow

    assertIO(calculateActualJson(actualTestReport), expectedJson)

  }

  private def calculateActualJson(report: TestsReport): IO[String] =
    Files[IO].tempDirectory
      .use((tempDir: Path) =>
        TestsCollectorConfiguration
          .from[IO](instant = Instant.now(), reportRootLocation = tempDir)
          .map((tc: TestsCollectorConfiguration) => TestsReport.asJson(report, tc).spaces2)
      )

  test("given an list with duplicated test ids, we will get a nice error") {

    val runningScenario = RunningScenario(new Ordinal(1), "t1", "f1", "s1")
    val testWithDuplicatedId = runningScenario.copy(test = "t2")

    val events = List(
      RunStarting(1),
      TestStarting(runningScenario, 2),
      TestSucceeded(runningScenario, RecordedEvents.empty, 3),
      TestStarting(testWithDuplicatedId, 2),
      TestSucceeded(testWithDuplicatedId, RecordedEvents.empty, 3),
      RunCompleted(4)
    )


    assertEquals(new TestsReportBuilderImpl().validateEvents(events), "there are some duplication in test ids list t_1_0,t_1_0".asLeft)

  }

}
