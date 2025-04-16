package org.binqua.examples.http4sapp

import munit.FunSuite
import org.binqua.examples.http4sapp.ScreenshotMoment.{ON_ENTER_PAGE, ON_EXIT_PAGE}
import org.binqua.examples.http4sapp.TestOutcome.STARTING
import org.scalatest.events.Ordinal

class ScenariosSpec extends FunSuite {

  test("test") {

    val startingScenario = Scenario(new Ordinal(1).next, "desc", 122L, Some(111L), None, STARTING)

    val actual1: Either[String, Scenarios] = Scenarios(List(startingScenario)).withNewScreenshot("url", ON_ENTER_PAGE)

    val expected1: Scenarios = Scenarios(
      List(
        Scenario(new Ordinal(1).next, "desc", 122L, Some(111L), Some(List(Screenshot("url", ON_ENTER_PAGE, startingScenario.ordinal, 1))), STARTING)
      )
    )

    assertEquals(actual1, Right(expected1))

    val actual2: Either[String, Scenarios] = actual1.flatMap(startingScenario => startingScenario.withNewScreenshot("url2", ON_EXIT_PAGE))

    val expected2: Scenarios = Scenarios(
      List(
        Scenario(new Ordinal(1).next, "desc", 122L, Some(111L), Some(List(
          Screenshot("url2", ON_EXIT_PAGE, startingScenario.ordinal, 2),
          Screenshot("url", ON_ENTER_PAGE, startingScenario.ordinal, 1),
        )), STARTING)
      )
    )


    assertEquals(actual2, Right(expected2))

  }
}
