package org.binqua.examples.http4sapp.app

import munit.FunSuite
import org.binqua.scalatest.reporter.ScreenshotMoment.{ON_PAGE_ENTER, ON_PAGE_EXIT}
import org.binqua.scalatest.reporter.{Scenario, Screenshot}
import org.binqua.scalatest.reporter.TestOutcome.STARTING
import org.scalatest.events.Ordinal

class ScenarioSpec extends FunSuite {

  test("addScreenshot should work") {

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

    val actual1: (Scenario, Screenshot) = Scenario.addScreenshot(startingScenario, "url1", ON_PAGE_EXIT)

    assertEquals(actual1._1, startingScenario.copy(screenshots = List(Screenshot("url1", ON_PAGE_EXIT, startingScenario.ordinal, 1))))

    val actual2: (Scenario, Screenshot) = Scenario.addScreenshot(scenario = actual1._1, pageUrl = "url2", screenshotMoment = ON_PAGE_ENTER)

    assertEquals(
      actual2._1,
      startingScenario.copy(screenshots =
        List(
          Screenshot("url1", ON_PAGE_EXIT, startingScenario.ordinal, 1),
          Screenshot("url2", ON_PAGE_ENTER, startingScenario.ordinal, 2)
        )
      )
    )

    val actual3: (Scenario, Screenshot) = Scenario.addScreenshot(actual2._1, "url3", ON_PAGE_EXIT)

    assertEquals(
      actual3._1,
      startingScenario.copy(screenshots =
          List(
            Screenshot("url1", ON_PAGE_EXIT, startingScenario.ordinal, 1),
            Screenshot("url2", ON_PAGE_ENTER, startingScenario.ordinal, 2),
            Screenshot("url3", ON_PAGE_EXIT, startingScenario.ordinal, 3)
        )
      )
    )

  }
}
