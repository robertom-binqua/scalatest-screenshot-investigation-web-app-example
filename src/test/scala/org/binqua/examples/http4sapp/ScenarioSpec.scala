package org.binqua.examples.http4sapp

import munit.FunSuite
import org.binqua.examples.http4sapp.ScreenshotMoment.{ON_ENTER_PAGE, ON_EXIT_PAGE}
import org.binqua.examples.http4sapp.TestOutcome.STARTING
import org.scalatest.events.Ordinal

import java.io.File

class ScenarioSpec extends FunSuite {

  test("withNewScreenshot should work") {

    val startingScenario = Scenario(new Ordinal(1).next, "desc", 122L, Some(111L), None, STARTING)

    val actual1: (Scenario, File) = startingScenario.withNewScreenshot("url1", ON_EXIT_PAGE)

    assertEquals(actual1._1, startingScenario.copy(screenshots = Some(List(Screenshot("url1", ON_EXIT_PAGE, startingScenario.ordinal, 1)))))

    val actual2: (Scenario, File) = actual1._1.withNewScreenshot("url2", ON_ENTER_PAGE)

    assertEquals(actual2._1, startingScenario.copy(screenshots = Some(List(
      Screenshot("url2", ON_ENTER_PAGE, startingScenario.ordinal, 2),
      Screenshot("url1", ON_EXIT_PAGE, startingScenario.ordinal, 1)
    ))))

    val actual3: (Scenario, File) = actual2._1.withNewScreenshot("url3", ON_EXIT_PAGE)

    assertEquals(actual3._1, startingScenario.copy(screenshots = Some(List(
      Screenshot("url3", ON_EXIT_PAGE, startingScenario.ordinal, 3),
      Screenshot("url2", ON_ENTER_PAGE, startingScenario.ordinal, 2),
      Screenshot("url1", ON_EXIT_PAGE, startingScenario.ordinal, 1)
    ))))


  }
}
