package org.binqua.examples.http4sapp

import munit.FunSuite
import org.binqua.examples.http4sapp
import org.binqua.examples.http4sapp.ScreenshotMoment.{ON_ENTER_PAGE, ON_EXIT_PAGE}
import org.binqua.examples.http4sapp.TestOutcome.STARTING
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
    val expTest3: http4sapp.Test = expTest2.copy(features = Features(features = Map(expFeature3.description -> expFeature3)))
    val expectedTests3: Tests = Tests(tests = Map(expTest3.name -> expTest3))

    assertEquals(actualTest3.map(_._1), Right(expectedTests3))

    assertEquals(
      actualTest3
        .map(_._1.runningTest)
        .toOption
        .flatten,
      Some(RunningScenario(expScenario1.ordinal, expTest1.name, expFeature1.description, expScenario1.description))
    )

  }

  test("we cannot add a screenshot after that the test is succeeded") {

    val runningScenario = RunningScenario(new Ordinal(1), "test desc", "feature desc", "scenario desc")

    val actualTests: Either[String, Tests] = Tests(Map.empty)
      .testStarting(runningScenario, timestamp = 1L)
      .flatMap(_.testSucceeded(runningScenario, timestamp = 2L))

    assertEquals(actualTests.map(_.runningTest.get), Right(runningScenario))

    val invalidTests: Either[String, (Tests, File)] = actualTests.flatMap(_.addScreenshot(runningScenario, "url", ON_EXIT_PAGE))

    assertEquals(invalidTests, Left("Sorry last scenario does not have testOutcome equal to STARTING but SUCCEEDED"))

  }

  test("we cannot add a screenshot after that the test is failed") {

    val runningScenario = RunningScenario(new Ordinal(1), "test desc", "feature desc", "scenario desc")

    val actualTests: Either[String, Tests] = Tests(Map.empty)
      .testStarting(runningScenario, timestamp = 1L)
      .flatMap(_.testFailed(runningScenario, timestamp = 2L))

    assertEquals(actualTests.map(_.runningTest.get), Right(runningScenario))

    val invalidTests: Either[String, (Tests, File)] = actualTests.flatMap(_.addScreenshot(runningScenario, "url", ON_EXIT_PAGE))

    assertEquals(invalidTests, Left("Sorry last scenario does not have testOutcome equal to STARTING but FAILED"))

  }

}
