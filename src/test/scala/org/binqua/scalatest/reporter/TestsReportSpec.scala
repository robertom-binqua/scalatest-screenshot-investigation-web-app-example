package org.binqua.scalatest.reporter

import cats.implicits.catsSyntaxEitherId
import io.circe.syntax.EncoderOps
import munit.FunSuite
import org.binqua.scalatest.reporter.StateEvent.{RecordedEvent, RecordedEvents}
import org.binqua.scalatest.reporter.util.utils.EitherOps
import org.scalatest.events.Ordinal

class TestsReportSpec extends FunSuite {

  test("we can start a test and add 2 screenshots to it.") {

    val expScenario1 = ReferenceData.startingScenario
    val expFeature1 =      Feature(description = "feature desc", ordinal = expScenario1.ordinal, scenarios = Scenarios(scenariosMap = Map(expScenario1.description -> expScenario1)))
    val expTest1 = Test("test desc", Features(featuresMap = Map(expFeature1.description -> expFeature1)), expScenario1.ordinal)
    val expTests1: TestsReport = TestsReport(tests = Map(expTest1.name -> expTest1))

    val runningScenario = RunningScenario(expScenario1.ordinal, expTest1.name, feature = expFeature1.description, expScenario1.description)

    val firstActualTests: Either[String, TestsReport] = TestsReport.testStarting(testsToBeUpdated = TestsReport(Map.empty), runningScenario = runningScenario, timestamp = 1L)
    assertEquals(firstActualTests, Right(expTests1))

    val secondActualTests: Either[String, (TestsReport, Screenshot)] = firstActualTests.flatMap(f =
      tests =>
        TestsReport.addScreenshot(testsToBeUpdated = tests, runningScenario = runningScenario, screenshotExternalData = ReferenceData.screenshotDriverData.url1)
    )

    val expScenario2 = expScenario1.copy(screenshots = List(Screenshot(ReferenceData.screenshotDriverData.url1, expScenario1.ordinal, 1)))
    val expFeature2 = expFeature1.copy(scenarios = Scenarios(scenariosMap = Map(expScenario2.description -> expScenario2)))
    val expTest2 = expTest1.copy(features = Features(featuresMap = Map(expFeature2.description -> expFeature2)))
    val expectedTests2: TestsReport = TestsReport(tests = Map(expTest2.name -> expTest2))

    assertEquals(secondActualTests.map(_._1), Right(expectedTests2))

    val actualTest3: Either[String, (TestsReport, Screenshot)] =
      secondActualTests.flatMap((test: (TestsReport, Screenshot)) => TestsReport.addScreenshot(test._1, runningScenario, ReferenceData.screenshotDriverData.url2))

    val expScenario3 = expScenario1.copy(screenshots =
      List(
        Screenshot(ReferenceData.screenshotDriverData.url1, expScenario1.ordinal, 1),
        Screenshot(ReferenceData.screenshotDriverData.url2, expScenario1.ordinal, 2)
      )
    )
    val expFeature3: Feature = expFeature2.copy(scenarios = Scenarios(scenariosMap = Map(expScenario3.description -> expScenario3)))
    val expTest3 = expTest2.copy(features = Features(featuresMap = Map(expFeature3.description -> expFeature3)))
    val expectedTests3: TestsReport = TestsReport(tests = Map(expTest3.name -> expTest3))

    assertEquals(actualTest3.map(_._1), Right(expectedTests3))

    val result: Either[String, RunningScenario] = actualTest3.flatMap((t: (TestsReport, Screenshot)) => TestsReport.runningTest(t._1))

    assertEquals(
      obtained = result,
      expected = RunningScenario(expScenario1.ordinal, expTest1.name, expFeature1.description, expScenario1.description).asRight
    )

  }

  test("we cannot add a screenshot after that the test is succeeded") {

    val runningScenario = RunningScenario(new Ordinal(1), "test desc", "feature desc", "scenario desc")

    val actualTests: Either[String, TestsReport] = TestsReport
      .testStarting(TestsReport(Map.empty), runningScenario, timestamp = 1L)
      .flatMap(TestsReport.testSucceeded(_, runningScenario, RecordedEvents.from(List(RecordedEvent(new Ordinal(122), "m", None, 5L))).getOrThrow, timestamp = 2L))

    assertEquals(actualTests.flatMap(TestsReport.runningTest), runningScenario.asRight)

    val invalidTests: Either[String, (TestsReport, Screenshot)] =
      actualTests.flatMap(TestsReport.addScreenshot(_, runningScenario, ReferenceData.screenshotDriverData.url1))

    assertEquals(invalidTests, Left("Sorry last scenario 'scenario desc' does not have testOutcome equal to STARTING but SUCCEEDED"))

  }

  test("we cannot add a screenshot after that the test is failed") {

    val runningScenario = RunningScenario(new Ordinal(1), "test desc", "feature desc", "scenario desc")

    val actualTests: Either[String, TestsReport] = TestsReport
      .testStarting(TestsReport(Map.empty), runningScenario, timestamp = 1L)
      .flatMap(TestsReport.testFailed(_, runningScenario, RecordedEvents.from(List(RecordedEvent(new Ordinal(122), "m", None, 5L))).getOrThrow, None, timestamp = 2L))

    assertEquals(actualTests.flatMap(TestsReport.runningTest), Right(runningScenario))

    val invalidTests: Either[String, (TestsReport, Screenshot)] =
      actualTests.flatMap(TestsReport.addScreenshot(_, runningScenario, ReferenceData.screenshotDriverData.url1))

    assertEquals(invalidTests, Left("Sorry last scenario 'scenario desc' does not have testOutcome equal to STARTING but FAILED"))

  }

  test("we can have a test suite with 2 different tests: t1->f1->s1 and t2->f2->s2 with screenshots and recorded events") {
    val t1f1s1 = RunningScenario(new Ordinal(1), "t1", "f1", "s1")
    val t2f2s2 = RunningScenario(new Ordinal(2), "t2", "f2", "s2")

    val actualTests: Either[String, TestsReport] = for {
      tests <- TestsReport(Map.empty).asRight

      test11 <- TestsReport.testStarting(tests, runningScenario = t1f1s1, timestamp = 1L)
      test21 <- TestsReport.addScreenshot(test11, t1f1s1, ReferenceData.screenshotDriverData.url1).map(_._1)
      test31 <- TestsReport.addScreenshot(test21, t1f1s1, ReferenceData.screenshotDriverData.url2).map(_._1)
      test51 <- TestsReport.testSucceeded(test31, t1f1s1, RecordedEvents.from(List(RecordedEvent(new Ordinal(122), "given", None, 5L))).getOrThrow, timestamp = 3L)

      test12 <- TestsReport.testStarting(test51, runningScenario = t2f2s2, timestamp = 1L)
      test22 <- TestsReport.addScreenshot(test12, t2f2s2, ReferenceData.screenshotDriverData.url3).map(_._1)
      test32 <- TestsReport.addScreenshot(test22, t2f2s2, ReferenceData.screenshotDriverData.url4).map(_._1)
      test52 <- TestsReport.testFailed(test32, t2f2s2, RecordedEvents.from(List(RecordedEvent(new Ordinal(122), "and", None, 5L))).getOrThrow, None, timestamp = 3L)
    } yield test52

    val expectedJson =
      """[
        |  {
        |    "name" : "t1",
        |    "id" : "t_1_0",
        |    "features" : [
        |      {
        |        "description" : "f1",
        |        "id" : "f_1_0",
        |        "scenarios" : [
        |          {
        |            "id" : "s_1_0",
        |            "description" : "s1",
        |            "startedTimestamp" : 1,
        |            "finishedTimestamp" : 3,
        |            "screenshots" : [
        |              {
        |                "originalLocation" : "scenario_ordinal_1_0/original/1_ON_EXIT_PAGE.png",
        |                "sourceLocation" : "scenario_ordinal_1_0/sources/1_ON_EXIT_PAGE.txt",
        |                "sourceWithNoHtmlLocation" : "scenario_ordinal_1_0/withNoHtml/1_ON_EXIT_PAGE.txt",
        |                "pageUrl" : "url1",
        |                "index" : 1,
        |                "pageTitle" : "title 1",
        |                "screenshotMoment" : "ON_EXIT_PAGE"
        |              },
        |              {
        |                "originalLocation" : "scenario_ordinal_1_0/original/2_ON_ENTER_PAGE.png",
        |                "sourceLocation" : "scenario_ordinal_1_0/sources/2_ON_ENTER_PAGE.txt",
        |                "sourceWithNoHtmlLocation" : "scenario_ordinal_1_0/withNoHtml/2_ON_ENTER_PAGE.txt",
        |                "pageUrl" : "url2",
        |                "index" : 2,
        |                "pageTitle" : "title 2",
        |                "screenshotMoment" : "ON_ENTER_PAGE"
        |              }
        |            ],
        |            "steps" : [
        |              {
        |                "message" : "given",
        |                "timestamp" : 5,
        |                "id" : "st_122_0"
        |              }
        |            ],
        |            "testOutcome" : "succeeded"
        |          }
        |        ]
        |      }
        |    ]
        |  },
        |  {
        |    "name" : "t2",
        |    "id" : "t_2_0",
        |    "features" : [
        |      {
        |        "description" : "f2",
        |        "id" : "f_2_0",
        |        "scenarios" : [
        |          {
        |            "id" : "s_2_0",
        |            "description" : "s2",
        |            "startedTimestamp" : 1,
        |            "finishedTimestamp" : 3,
        |            "screenshots" : [
        |              {
        |                "originalLocation" : "scenario_ordinal_2_0/original/1_ON_EXIT_PAGE.png",
        |                "sourceLocation" : "scenario_ordinal_2_0/sources/1_ON_EXIT_PAGE.txt",
        |                "sourceWithNoHtmlLocation" : "scenario_ordinal_2_0/withNoHtml/1_ON_EXIT_PAGE.txt",
        |                "pageUrl" : "url3",
        |                "index" : 1,
        |                "pageTitle" : "title 3",
        |                "screenshotMoment" : "ON_EXIT_PAGE"
        |              },
        |              {
        |                "originalLocation" : "scenario_ordinal_2_0/original/2_ON_ENTER_PAGE.png",
        |                "sourceLocation" : "scenario_ordinal_2_0/sources/2_ON_ENTER_PAGE.txt",
        |                "sourceWithNoHtmlLocation" : "scenario_ordinal_2_0/withNoHtml/2_ON_ENTER_PAGE.txt",
        |                "pageUrl" : "url4",
        |                "index" : 2,
        |                "pageTitle" : "title 4",
        |                "screenshotMoment" : "ON_ENTER_PAGE"
        |              }
        |            ],
        |            "steps" : [
        |              {
        |                "message" : "and",
        |                "timestamp" : 5,
        |                "id" : "st_122_0"
        |              }
        |            ],
        |            "testOutcome" : "failed"
        |          }
        |        ]
        |      }
        |    ]
        |  }
        |]""".stripMargin

    assertEquals(actualTests.map(_.asJson.spaces2), expectedJson.asRight)

  }

  test("we can have a test suite with 1 test t1 with multiple features: t1->f1->s1 and t1->f2->s2 with screenshots and recorded events") {
    val t1_f1_s1 = RunningScenario(new Ordinal(1), "t1", "f1", "s1")
    val t1_f2_s1 = RunningScenario(new Ordinal(2), "t1", "f2", "s1")

    val actualTests: Either[String, TestsReport] = for {
      test <- TestsReport(Map.empty).asRight

      test11 <- TestsReport.testStarting(test, runningScenario = t1_f1_s1, timestamp = 1L)
      test21 <- TestsReport.addScreenshot(test11, t1_f1_s1, ReferenceData.screenshotDriverData.url1).map(_._1)
      test31 <- TestsReport.addStep(testsToBeUpdated = test21, runningScenario = t1_f1_s1, message = "m1-f1-s1", throwable = None, timestamp = 1L)
      test41 <- TestsReport.testSucceeded(test31, t1_f1_s1, RecordedEvents.from(List(RecordedEvent(new Ordinal(122), "given", None, 5L))).getOrThrow, timestamp = 3L)

      test12 <- TestsReport.testStarting(test41, runningScenario = t1_f2_s1, timestamp = 1L)
      test22 <- TestsReport.addScreenshot(test12, t1_f2_s1, ReferenceData.screenshotDriverData.url2).map(_._1)
      test42 <- TestsReport.addStep(testsToBeUpdated = test22, runningScenario = t1_f2_s1, message = "m1-f2-s1", throwable = None, timestamp = 1L)
      test52 <- TestsReport.testFailed(test42, t1_f2_s1, RecordedEvents.from(List(RecordedEvent(new Ordinal(122), "and", None, 5L))).getOrThrow, None, timestamp = 3L)
    } yield test52

    val expectedJson =
      """[
        |  {
        |    "name" : "t1",
        |    "id" : "t_1_0",
        |    "features" : [
        |      {
        |        "description" : "f1",
        |        "id" : "f_1_0",
        |        "scenarios" : [
        |          {
        |            "id" : "s_1_0",
        |            "description" : "s1",
        |            "startedTimestamp" : 1,
        |            "finishedTimestamp" : 3,
        |            "screenshots" : [
        |              {
        |                "originalLocation" : "scenario_ordinal_1_0/original/1_ON_EXIT_PAGE.png",
        |                "sourceLocation" : "scenario_ordinal_1_0/sources/1_ON_EXIT_PAGE.txt",
        |                "sourceWithNoHtmlLocation" : "scenario_ordinal_1_0/withNoHtml/1_ON_EXIT_PAGE.txt",
        |                "pageUrl" : "url1",
        |                "index" : 1,
        |                "pageTitle" : "title 1",
        |                "screenshotMoment" : "ON_EXIT_PAGE"
        |              }
        |            ],
        |            "steps" : [
        |              {
        |                "message" : "m1-f1-s1",
        |                "timestamp" : 1,
        |                "id" : "st_1_0"
        |              },
        |              {
        |                "message" : "given",
        |                "timestamp" : 5,
        |                "id" : "st_122_0"
        |              }
        |            ],
        |            "testOutcome" : "succeeded"
        |          }
        |        ]
        |      },
        |      {
        |        "description" : "f2",
        |        "id" : "f_2_0",
        |        "scenarios" : [
        |          {
        |            "id" : "s_2_0",
        |            "description" : "s1",
        |            "startedTimestamp" : 1,
        |            "finishedTimestamp" : 3,
        |            "screenshots" : [
        |              {
        |                "originalLocation" : "scenario_ordinal_2_0/original/1_ON_ENTER_PAGE.png",
        |                "sourceLocation" : "scenario_ordinal_2_0/sources/1_ON_ENTER_PAGE.txt",
        |                "sourceWithNoHtmlLocation" : "scenario_ordinal_2_0/withNoHtml/1_ON_ENTER_PAGE.txt",
        |                "pageUrl" : "url2",
        |                "index" : 1,
        |                "pageTitle" : "title 2",
        |                "screenshotMoment" : "ON_ENTER_PAGE"
        |              }
        |            ],
        |            "steps" : [
        |              {
        |                "message" : "m1-f2-s1",
        |                "timestamp" : 1,
        |                "id" : "st_2_0"
        |              },
        |              {
        |                "message" : "and",
        |                "timestamp" : 5,
        |                "id" : "st_122_0"
        |              }
        |            ],
        |            "testOutcome" : "failed"
        |          }
        |        ]
        |      }
        |    ]
        |  }
        |]""".stripMargin

    assertEquals(obtained = actualTests.map(_.asJson.spaces2), expected = expectedJson.asRight)

  }

}
