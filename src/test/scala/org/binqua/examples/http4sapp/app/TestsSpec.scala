package org.binqua.examples.http4sapp.app

import cats.implicits.catsSyntaxEitherId
import io.circe.syntax.EncoderOps
import munit.FunSuite
import org.binqua.scalatest.reporter.ScreenshotMoment.{ON_ENTER_PAGE, ON_EXIT_PAGE}
import org.binqua.scalatest.reporter.StateEvent.{RecordedEvent, RecordedEvents}
import org.binqua.scalatest.reporter.Utils.EitherOps
import org.binqua.scalatest.reporter._
import org.scalatest.events.Ordinal

class TestsSpec extends FunSuite {

  test("we can start a test and add 2 screenshots to it.") {

    val expScenario1 = ReferenceData.startingScenario

    val expFeature1 =
      Feature(description = "feature desc", ordinal = expScenario1.ordinal, scenarios = Scenarios(scenariosMap = Map(expScenario1.description -> expScenario1)))
    val expTest1 = Test("test desc", Features(featuresMap = Map(expFeature1.description -> expFeature1)), expScenario1.ordinal)
    val expTests1: Tests = Tests(tests = Map(expTest1.name -> expTest1))

    val runningScenario = RunningScenario(expScenario1.ordinal, expTest1.name, feature = expFeature1.description, expScenario1.description)

    val firstActualTests: Either[String, Tests] = Tests.testStarting(Tests(Map.empty), runningScenario, 1L)
    assertEquals(firstActualTests, Right(expTests1))

    val secondActualTests: Either[String, (Tests, Screenshot)] = firstActualTests.flatMap(tests => {
      Tests.addScreenshot(tests, runningScenario, "url1", ON_ENTER_PAGE)
    })

    val expScenario2 = expScenario1.copy(screenshots = List(Screenshot("url1", ON_ENTER_PAGE, expScenario1.ordinal, 1)))
    val expFeature2 = expFeature1.copy(scenarios = Scenarios(scenariosMap = Map(expScenario2.description -> expScenario2)))
    val expTest2 = expTest1.copy(features = Features(featuresMap = Map(expFeature2.description -> expFeature2)))
    val expectedTests2: Tests = Tests(tests = Map(expTest2.name -> expTest2))

    assertEquals(secondActualTests.map(_._1), Right(expectedTests2))

    val actualTest3: Either[String, (Tests, Screenshot)] =
      secondActualTests.flatMap((test: (Tests, Screenshot)) => Tests.addScreenshot(test._1, runningScenario, "url2", ON_EXIT_PAGE))

    val expScenario3 = expScenario1.copy(screenshots =
        List(
          Screenshot("url1", ON_ENTER_PAGE, expScenario1.ordinal, 1),
          Screenshot("url2", ON_EXIT_PAGE, expScenario1.ordinal, 2)
      )
    )
    val expFeature3: Feature = expFeature2.copy(scenarios = Scenarios(scenariosMap = Map(expScenario3.description -> expScenario3)))
    val expTest3 = expTest2.copy(features = Features(featuresMap = Map(expFeature3.description -> expFeature3)))
    val expectedTests3: Tests = Tests(tests = Map(expTest3.name -> expTest3))

    assertEquals(actualTest3.map(_._1), Right(expectedTests3))

    val result: Either[String, RunningScenario] = actualTest3.flatMap((t: (Tests, Screenshot)) => Tests.runningTest(t._1))

    assertEquals(
      obtained = result,
      expected = RunningScenario(expScenario1.ordinal, expTest1.name, expFeature1.description, expScenario1.description).asRight
    )

  }

  test("we cannot add a screenshot after that the test is succeeded") {

    val runningScenario = RunningScenario(new Ordinal(1), "test desc", "feature desc", "scenario desc")

    val actualTests: Either[String, Tests] = Tests
      .testStarting(Tests(Map.empty), runningScenario, timestamp = 1L)
      .flatMap(Tests.testSucceeded(_, runningScenario, RecordedEvents.from(List(RecordedEvent(new Ordinal(122), "m", None, 5L))).getOrThrow, timestamp = 2L))

    assertEquals(actualTests.flatMap(Tests.runningTest), runningScenario.asRight)

    val invalidTests: Either[String, (Tests, Screenshot)] = actualTests.flatMap(Tests.addScreenshot(_, runningScenario, "url", ON_EXIT_PAGE))

    assertEquals(invalidTests, Left("Sorry last scenario does not have testOutcome equal to STARTING but SUCCEEDED"))

  }

  test("we cannot add a screenshot after that the test is failed") {

    val runningScenario = RunningScenario(new Ordinal(1), "test desc", "feature desc", "scenario desc")

    val actualTests: Either[String, Tests] = Tests
      .testStarting(Tests(Map.empty), runningScenario, timestamp = 1L)
      .flatMap(Tests.testFailed(_, runningScenario, RecordedEvents.from(List(RecordedEvent(new Ordinal(122), "m", None, 5L))).getOrThrow, None, timestamp = 2L))

    assertEquals(actualTests.flatMap(Tests.runningTest), Right(runningScenario))

    val invalidTests: Either[String, (Tests, Screenshot)] = actualTests.flatMap(Tests.addScreenshot(_, runningScenario, "url", ON_EXIT_PAGE))

    assertEquals(invalidTests, Left("Sorry last scenario does not have testOutcome equal to STARTING but FAILED"))

  }

  test("we can have a test suite with 2 different tests: t1->f1->s1 and t2->f2->s2 with screenshots and recorded events") {
    val t1f1s1 = RunningScenario(new Ordinal(1), "t1", "f1", "s1")
    val t2f2s2 = RunningScenario(new Ordinal(2), "t2", "f2", "s2")

    val actualTests: Either[String, Tests] = for {
      tests <- Tests(Map.empty).asRight

      test11 <- Tests.testStarting(tests, runningScenario = t1f1s1, timestamp = 1L)
      test21 <- Tests.addScreenshot(test11, t1f1s1, "ulr11", ON_ENTER_PAGE).map(_._1)
      test31 <- Tests.addScreenshot(test21, t1f1s1, "ulr21", ON_EXIT_PAGE).map(_._1)
      test51 <- Tests.testSucceeded(test31, t1f1s1, RecordedEvents.from(List(RecordedEvent(new Ordinal(122), "given", None, 5L))).getOrThrow, timestamp = 3L)

      test12 <- Tests.testStarting(test51, runningScenario = t2f2s2, timestamp = 1L)
      test22 <- Tests.addScreenshot(test12, t2f2s2, "ulr12", ON_ENTER_PAGE).map(_._1)
      test32 <- Tests.addScreenshot(test22, t2f2s2, "ulr22", ON_EXIT_PAGE).map(_._1)
      test52 <- Tests.testFailed(test32, t2f2s2, RecordedEvents.from(List(RecordedEvent(new Ordinal(122), "and", None, 5L))).getOrThrow, None, timestamp = 3L)
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
        |            "ordinal" : "1_0",
        |            "description" : "s1",
        |            "startedTimestamp" : 1,
        |            "finishedTimestamp" : 3,
        |            "screenshots" : [
        |              {
        |                "originalLocation" : "scenario_ordinal_1_0/original/1_ON_ENTER_PAGE.png",
        |                "resizedLocation" : "scenario_ordinal_1_0/resized/1_ON_ENTER_PAGE.png",
        |                "sourceLocation" : "scenario_ordinal_1_0/sources/1_ON_ENTER_PAGE.txt",
        |                "pageUrl" : "ulr11",
        |                "index" : 1,
        |                "screenshotMoment" : "ON_ENTER_PAGE"
        |              },
        |              {
        |                "originalLocation" : "scenario_ordinal_1_0/original/2_ON_EXIT_PAGE.png",
        |                "resizedLocation" : "scenario_ordinal_1_0/resized/2_ON_EXIT_PAGE.png",
        |                "sourceLocation" : "scenario_ordinal_1_0/sources/2_ON_EXIT_PAGE.txt",
        |                "pageUrl" : "ulr21",
        |                "index" : 2,
        |                "screenshotMoment" : "ON_EXIT_PAGE"
        |              }
        |            ],
        |            "steps" : [
        |              {
        |                "message" : "given",
        |                "timestamp" : 5,
        |                "ordinal" : "122_0"
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
        |            "ordinal" : "2_0",
        |            "description" : "s2",
        |            "startedTimestamp" : 1,
        |            "finishedTimestamp" : 3,
        |            "screenshots" : [
        |              {
        |                "originalLocation" : "scenario_ordinal_2_0/original/1_ON_ENTER_PAGE.png",
        |                "resizedLocation" : "scenario_ordinal_2_0/resized/1_ON_ENTER_PAGE.png",
        |                "sourceLocation" : "scenario_ordinal_2_0/sources/1_ON_ENTER_PAGE.txt",
        |                "pageUrl" : "ulr12",
        |                "index" : 1,
        |                "screenshotMoment" : "ON_ENTER_PAGE"
        |              },
        |              {
        |                "originalLocation" : "scenario_ordinal_2_0/original/2_ON_EXIT_PAGE.png",
        |                "resizedLocation" : "scenario_ordinal_2_0/resized/2_ON_EXIT_PAGE.png",
        |                "sourceLocation" : "scenario_ordinal_2_0/sources/2_ON_EXIT_PAGE.txt",
        |                "pageUrl" : "ulr22",
        |                "index" : 2,
        |                "screenshotMoment" : "ON_EXIT_PAGE"
        |              }
        |            ],
        |            "steps" : [
        |              {
        |                "message" : "and",
        |                "timestamp" : 5,
        |                "ordinal" : "122_0"
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

    val actualTests: Either[String, Tests] = for {
      test <- Tests(Map.empty).asRight

      test11 <- Tests.testStarting(test, runningScenario = t1_f1_s1, timestamp = 1L)
      test21 <- Tests.addScreenshot(test11, t1_f1_s1, "ulr-f1-s1-1", ON_ENTER_PAGE).map(_._1)
      test31 <- Tests.addStep(testsToBeUpdated = test21, runningScenario = t1_f1_s1, message = "m1-f1-s1", throwable = None, timestamp = 1L)
      test41 <- Tests.testSucceeded(test31, t1_f1_s1, RecordedEvents.from(List(RecordedEvent(new Ordinal(122), "given", None, 5L))).getOrThrow, timestamp = 3L)

      test12 <- Tests.testStarting(test41, runningScenario = t1_f2_s1, timestamp = 1L)
      test22 <- Tests.addScreenshot(test12, t1_f2_s1, "ulr-f2-s1-1", ON_ENTER_PAGE).map(_._1)
      test42 <- Tests.addStep(testsToBeUpdated = test22, runningScenario = t1_f2_s1, message = "m1-f2-s1", throwable = None, timestamp = 1L)
      test52 <- Tests.testFailed(test42, t1_f2_s1, RecordedEvents.from(List(RecordedEvent(new Ordinal(122), "and", None, 5L))).getOrThrow, None, timestamp = 3L)
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
        |            "ordinal" : "1_0",
        |            "description" : "s1",
        |            "startedTimestamp" : 1,
        |            "finishedTimestamp" : 3,
        |            "screenshots" : [
        |              {
        |                "originalLocation" : "scenario_ordinal_1_0/original/1_ON_ENTER_PAGE.png",
        |                "resizedLocation" : "scenario_ordinal_1_0/resized/1_ON_ENTER_PAGE.png",
        |                "sourceLocation" : "scenario_ordinal_1_0/sources/1_ON_ENTER_PAGE.txt",
        |                "pageUrl" : "ulr-f1-s1-1",
        |                "index" : 1,
        |                "screenshotMoment" : "ON_ENTER_PAGE"
        |              }
        |            ],
        |            "steps" : [
        |              {
        |                "message" : "m1-f1-s1",
        |                "timestamp" : 1,
        |                "ordinal" : "1_0"
        |              },
        |              {
        |                "message" : "given",
        |                "timestamp" : 5,
        |                "ordinal" : "122_0"
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
        |            "ordinal" : "2_0",
        |            "description" : "s1",
        |            "startedTimestamp" : 1,
        |            "finishedTimestamp" : 3,
        |            "screenshots" : [
        |              {
        |                "originalLocation" : "scenario_ordinal_2_0/original/1_ON_ENTER_PAGE.png",
        |                "resizedLocation" : "scenario_ordinal_2_0/resized/1_ON_ENTER_PAGE.png",
        |                "sourceLocation" : "scenario_ordinal_2_0/sources/1_ON_ENTER_PAGE.txt",
        |                "pageUrl" : "ulr-f2-s1-1",
        |                "index" : 1,
        |                "screenshotMoment" : "ON_ENTER_PAGE"
        |              }
        |            ],
        |            "steps" : [
        |              {
        |                "message" : "m1-f2-s1",
        |                "timestamp" : 1,
        |                "ordinal" : "2_0"
        |              },
        |              {
        |                "message" : "and",
        |                "timestamp" : 5,
        |                "ordinal" : "122_0"
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
