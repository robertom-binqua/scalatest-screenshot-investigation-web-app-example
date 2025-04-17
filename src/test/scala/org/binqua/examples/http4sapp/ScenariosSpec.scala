package org.binqua.examples.http4sapp

import cats.implicits.catsSyntaxEitherId
import munit.FunSuite
import org.binqua.examples.http4sapp.ScreenshotMoment.{ON_ENTER_PAGE, ON_EXIT_PAGE}
import org.binqua.examples.http4sapp.TestOutcome.{FAILED, STARTING, SUCCEEDED}
import org.scalatest.events.Ordinal

import java.io.File

class ScenariosSpec extends FunSuite {

  test("we can add 2 screenshots to a scenario that is in STARTING state") {

    val scenario: Scenario = Scenario(new Ordinal(1).next, "desc", 122L, Some(111L), None, STARTING)

    val actual1: Either[String, (Scenarios, File)] =
      Scenarios(scenarios = Map("desc" -> scenario)).withNewScreenshot(scenario.ordinal, scenario.description, "url", ON_ENTER_PAGE)

    val expected1: Scenarios = Scenarios(
      Map("desc" -> scenario.copy(screenshots = Some(List(Screenshot("url", ON_ENTER_PAGE, scenario.ordinal, 1)))))
    )

    assertEquals(actual1.map(_._1), Right(expected1))

    val actual2: Either[String, (Scenarios, File)] =
      actual1.flatMap(result =>
        result._1.withNewScreenshot(ordinal = scenario.ordinal, scenarioDescription = scenario.description, pageUrl = "url2", screenshotMoment = ON_EXIT_PAGE)
      )

    val expected2: Scenarios = Scenarios(
      Map(
        "desc" -> scenario.copy(screenshots =
          Some(
            List(
              Screenshot(pageUrl = "url2", screenshotMoment = ON_EXIT_PAGE, ordinal = scenario.ordinal, index = 2),
              Screenshot(pageUrl = "url", screenshotMoment = ON_ENTER_PAGE, ordinal = scenario.ordinal, index = 1)
            )
          )
        )
      )
    )

    assertEquals(actual2.map(_._1), expected2.asRight)

  }
  test("we cannot add screenshots to a scenario that is in FAILED state") {

    val scenario: Scenario = Scenario(new Ordinal(1).next, "desc", 122L, Some(111L), None, FAILED)

    val actual1: Either[String, (Scenarios, File)] =
      Scenarios(scenarios = Map("desc" -> scenario)).withNewScreenshot(scenario.ordinal, scenario.description, "url", ON_ENTER_PAGE)

    assertEquals(actual1.map(_._1), "Sorry last scenario does not have testOutcome equal to STARTING but FAILED".asLeft)

  }
  test("we cannot add screenshots to a scenario that is in SUCCEEDED state") {

    val scenario: Scenario = Scenario(new Ordinal(1).next, "desc", 122L, Some(111L), None, SUCCEEDED)

    val actual1: Either[String, (Scenarios, File)] =
      Scenarios(scenarios = Map("desc" -> scenario)).withNewScreenshot(scenario.ordinal, scenario.description, "url", ON_ENTER_PAGE)

    assertEquals(actual1.map(_._1), "Sorry last scenario does not have testOutcome equal to STARTING but SUCCEEDED".asLeft)

  }

}
