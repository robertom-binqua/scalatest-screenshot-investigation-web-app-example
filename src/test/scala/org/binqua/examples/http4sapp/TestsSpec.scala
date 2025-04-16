package org.binqua.examples.http4sapp

import munit.FunSuite
import org.binqua.examples.http4sapp.ScreenshotMoment.{ON_ENTER_PAGE, ON_EXIT_PAGE}
import org.binqua.examples.http4sapp.TestOutcome.STARTING
import org.scalatest.events.Ordinal

class TestsSpec extends FunSuite {

  test("test") {

    val startingScenario = Scenario(ordinal = new Ordinal(1).next,
      description = "desc",
      startedTimestamp = 1L,
      finishedTimestamp = None,
      screenshots = None, testOutcome = STARTING)

    val actualTests1: Either[String, Tests] = Tests(List.empty).testStarting(startingScenario.ordinal, "test desc", "feature desc", "desc", 1L)

    val expected1: Tests = Tests(
      tests = List(
        Test("test desc", Features(features = List(Feature(description = "feature desc", scenarios = Scenarios(scenarios = List(startingScenario)))))
        )
      )
    )

    assertEquals(actualTests1, Right(expected1))

    val actualTest2: Either[String, Tests] = actualTests1.flatMap(test => test.addScreenshot("url1", ON_ENTER_PAGE))

    val expected2: Tests = Tests(
      tests = List(
        Test("test desc", Features(features = List(Feature(description = "feature desc", scenarios = Scenarios(scenarios = List(startingScenario.copy(screenshots = Some(List(
          Screenshot("url1", ON_ENTER_PAGE, startingScenario.ordinal, 1)
        ))))))))
        )
      )
    )

    assertEquals(actualTest2, Right(expected2))

    val actualTest3: Either[String, Tests] = actualTest2.flatMap(test => test.addScreenshot("url2", ON_EXIT_PAGE))

    val expected3: Tests = Tests(
      tests = List(
        Test("test desc", Features(features = List(Feature(description = "feature desc", scenarios = Scenarios(scenarios = List(startingScenario.copy(screenshots = Some(List(
          Screenshot("url2", ON_EXIT_PAGE, startingScenario.ordinal, 2),
          Screenshot("url1", ON_ENTER_PAGE, startingScenario.ordinal, 1)
        ))))))))
        )
      )
    )

    assertEquals(actualTest3, Right(expected3))

  }
}
