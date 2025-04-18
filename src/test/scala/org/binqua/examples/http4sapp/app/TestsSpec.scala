package org.binqua.examples.http4sapp.app

import cats.implicits.catsSyntaxEitherId
import io.circe.syntax.EncoderOps
import munit.FunSuite
import org.binqua.examples.http4sapp
import org.binqua.examples.http4sapp.app.ScreenshotMoment._
import org.binqua.examples.http4sapp.app.TestOutcome._
import org.scalatest.events.Ordinal

import java.io.File

class TestsSpec extends FunSuite {

  test("we can start a test and add 2 screenshots to it.") {

    val expScenario1 =
      Scenario(
        ordinal = new Ordinal(1).next,
        description = "desc",
        startedTimestamp = 1L,
        finishedTimestamp = Option.empty,
        screenshots = Option.empty,
        testOutcome = STARTING
      )
    val expFeature1 = Feature(description = "feature desc", scenarios = Scenarios(scenarios = Map(expScenario1.description -> expScenario1)))
    val expTest1 = Test("test desc", Features(features = Map(expFeature1.description -> expFeature1)))
    val expTests1: Tests = Tests(tests = Map(expTest1.name -> expTest1))

    val runningScenario = RunningScenario(expScenario1.ordinal, expTest1.name, feature = expFeature1.description, expScenario1.description)

    val firstActualTests: Either[String, Tests] = Tests(Map.empty).testStarting(runningScenario, 1L)
    assertEquals(firstActualTests, Right(expTests1))

    val secondActualTests: Either[String, (Tests, File)] = firstActualTests.flatMap(test => { test.addScreenshot(runningScenario, "url1", ON_ENTER_PAGE) })

    val expScenario2 = expScenario1.copy(screenshots = Some(List(Screenshot("url1", ON_ENTER_PAGE, expScenario1.ordinal, 1))))
    val expFeature2 = expFeature1.copy(scenarios = Scenarios(scenarios = Map(expScenario2.description -> expScenario2)))
    val expTest2 = expTest1.copy(features = Features(features = Map(expFeature2.description -> expFeature2)))
    val expectedTests2: Tests = Tests(tests = Map(expTest2.name -> expTest2))

    assertEquals(secondActualTests.map(_._1), Right(expectedTests2))

    val actualTest3: Either[String, (Tests, File)] =
      secondActualTests.flatMap((test: (Tests, File)) => test._1.addScreenshot(runningScenario, "url2", ON_EXIT_PAGE))

    val expScenario3 = expScenario1.copy(screenshots =
      Some(
        List(
          Screenshot("url2", ON_EXIT_PAGE, expScenario1.ordinal, 2),
          Screenshot("url1", ON_ENTER_PAGE, expScenario1.ordinal, 1)
        )
      )
    )
    val expFeature3: Feature = expFeature2.copy(scenarios = Scenarios(scenarios = Map(expScenario3.description -> expScenario3)))
    val expTest3: http4sapp.app.Test = expTest2.copy(features = Features(features = Map(expFeature3.description -> expFeature3)))
    val expectedTests3: Tests = Tests(tests = Map(expTest3.name -> expTest3))

    assertEquals(actualTest3.map(_._1), Right(expectedTests3))

    val result: Either[String, RunningScenario] = actualTest3.flatMap(_._1.runningTest)

    assertEquals(
      obtained = result,
      expected = RunningScenario(expScenario1.ordinal, expTest1.name, expFeature1.description, expScenario1.description).asRight
    )

  }

  test("we cannot add a screenshot after that the test is succeeded") {

    val runningScenario = RunningScenario(new Ordinal(1), "test desc", "feature desc", "scenario desc")

    val actualTests: Either[String, Tests] = Tests(Map.empty)
      .testStarting(runningScenario, timestamp = 1L)
      .flatMap(_.testSucceeded(runningScenario, timestamp = 2L))

    assertEquals(actualTests.flatMap(_.runningTest), runningScenario.asRight)

    val invalidTests: Either[String, (Tests, File)] = actualTests.flatMap(_.addScreenshot(runningScenario, "url", ON_EXIT_PAGE))

    assertEquals(invalidTests, Left("Sorry last scenario does not have testOutcome equal to STARTING but SUCCEEDED"))

  }

  test("we cannot add a screenshot after that the test is failed") {

    val runningScenario = RunningScenario(new Ordinal(1), "test desc", "feature desc", "scenario desc")

    val actualTests: Either[String, Tests] = Tests(Map.empty)
      .testStarting(runningScenario, timestamp = 1L)
      .flatMap(_.testFailed(runningScenario, timestamp = 2L))

    assertEquals(actualTests.flatMap(_.runningTest), Right(runningScenario))

    val invalidTests: Either[String, (Tests, File)] = actualTests.flatMap(_.addScreenshot(runningScenario, "url", ON_EXIT_PAGE))

    assertEquals(invalidTests, Left("Sorry last scenario does not have testOutcome equal to STARTING but FAILED"))

  }

  test("a tests with 1 success with 2 screenshots and 1 failure with 2 screenshots can be converted to json") {
    val runningScenario1 = RunningScenario(new Ordinal(1), "t1", "f1", "s1")
    val runningScenario2 = RunningScenario(new Ordinal(2), "t2", "f2", "s2")

    val actualTests: Either[String, Tests] = for {
      test <- Tests(Map.empty).asRight

      test11 <- test.testStarting(runningScenario = runningScenario1, timestamp = 1L)
      test21 <- test11.addScreenshot(runningScenario1, "ulr11", ON_ENTER_PAGE).map(_._1)
      test31 <- test21.addScreenshot(runningScenario1, "ulr21", ON_EXIT_PAGE).map(_._1)
      test41 <- test31.testSucceeded(runningScenario1, timestamp = 3L)

      test12 <- test41.testStarting(runningScenario = runningScenario2, timestamp = 1L)
      test22 <- test12.addScreenshot(runningScenario2, "ulr12", ON_ENTER_PAGE).map(_._1)
      test32 <- test22.addScreenshot(runningScenario2, "ulr22", ON_EXIT_PAGE).map(_._1)
      test42 <- test32.testFailed(runningScenario2, timestamp = 3L)
    } yield test42

    val expectedJson =
      """[
        |  {
        |    "name" : "t1",
        |    "features" : [
        |      {
        |        "description" : "f1",
        |        "scenarios" : [
        |          {
        |            "ordinal" : "1_0",
        |            "description" : "s1",
        |            "startedTimestamp" : 1,
        |            "finishedTimestamp" : 3,
        |            "screenshots" : [
        |              {
        |                "location" : "scenario_ordinal_1_0/screenshot_2_ON_EXIT_PAGE.png"
        |              },
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
        |    "name" : "t2",
        |    "features" : [
        |      {
        |        "description" : "f2",
        |        "scenarios" : [
        |          {
        |            "ordinal" : "2_0",
        |            "description" : "s2",
        |            "startedTimestamp" : 1,
        |            "finishedTimestamp" : 3,
        |            "screenshots" : [
        |              {
        |                "location" : "scenario_ordinal_2_0/screenshot_2_ON_EXIT_PAGE.png"
        |              },
        |              {
        |                "location" : "scenario_ordinal_2_0/screenshot_1_ON_ENTER_PAGE.png"
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

}
