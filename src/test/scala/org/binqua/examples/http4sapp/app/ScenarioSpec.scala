package org.binqua.examples.http4sapp.app

import munit.FunSuite
import org.binqua.examples.http4sapp.app.ScreenshotMoment._
import org.binqua.examples.http4sapp.app.TestOutcome._
import org.scalatest.events.Ordinal

class ScenarioSpec extends FunSuite {

  test("withNewScreenshot should work") {

    val startingScenario = Scenario(
      ordinal = new Ordinal(1),
      description = "desc",
      startedTimestamp = 122L,
      finishedTimestamp = Some(111L),
      screenshots = Nil,
      steps = None,
      testOutcome = STARTING,
      throwable = None
    )

    val actual1: (Scenario, Screenshot) = Scenario.addScreenshot(startingScenario, "url1", ON_EXIT_PAGE)

    assertEquals(actual1._1, startingScenario.copy(screenshots = List(Screenshot("url1", ON_EXIT_PAGE, startingScenario.ordinal, 1))))

    val actual2: (Scenario, Screenshot) = Scenario.addScreenshot(scenario = actual1._1, pageUrl = "url2", screenshotMoment = ON_ENTER_PAGE)

    assertEquals(
      actual2._1,
      startingScenario.copy(screenshots =
        List(
          Screenshot("url1", ON_EXIT_PAGE, startingScenario.ordinal, 1),
          Screenshot("url2", ON_ENTER_PAGE, startingScenario.ordinal, 2)
        )
      )
    )

    val actual3: (Scenario, Screenshot) = Scenario.addScreenshot(actual2._1, "url3", ON_EXIT_PAGE)

    assertEquals(
      actual3._1,
      startingScenario.copy(screenshots =
          List(
            Screenshot("url1", ON_EXIT_PAGE, startingScenario.ordinal, 1),
            Screenshot("url2", ON_ENTER_PAGE, startingScenario.ordinal, 2),
            Screenshot("url3", ON_EXIT_PAGE, startingScenario.ordinal, 3)
        )
      )
    )

  }
}
