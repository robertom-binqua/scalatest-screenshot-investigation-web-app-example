package org.binqua.scalatest.reporter

import cats.implicits.catsSyntaxEitherId
import munit.FunSuite
import org.binqua.scalatest.reporter.TestOutcome.{FAILED, STARTING, SUCCEEDED}
import org.scalatest.events.Ordinal

class ScenariosSpec extends FunSuite {

  test("we can add 2 screenshots to a scenario that is in STARTING state") {

    val scenario: Scenario = Scenario(
      ordinal = new Ordinal(1).next,
      description = "desc",
      startedTimestamp = 122L,
      finishedTimestamp = Some(111L),
      screenshots = Nil,
      steps = None,
      testOutcome = STARTING,
      throwable = None
    )

    val Right((actualScenarios1, _)) =
      Scenarios(scenariosMap = Map("desc" -> scenario))
        .withNewScreenshot(scenario.ordinal, scenario.description, ReferenceData.screenshotDriverData.url1)

    val expectedScenarios: Scenarios = Scenarios(
      Map("desc" -> scenario.copy(screenshots = List(Screenshot(ReferenceData.screenshotDriverData.url1, scenario.ordinal, 1))))
    )

    assertEquals(actualScenarios1, expectedScenarios)

    val Right((actualScenarios2, _)) =
      actualScenarios1.withNewScreenshot(ordinal = scenario.ordinal, scenarioDescription = scenario.description, ReferenceData.screenshotDriverData.url2)

    val expected2: Scenarios = Scenarios(
      Map(
        "desc" -> scenario.copy(screenshots =
          List(
            Screenshot(ReferenceData.screenshotDriverData.url1, ordinal = scenario.ordinal, index = 1),
            Screenshot(ReferenceData.screenshotDriverData.url2, ordinal = scenario.ordinal, index = 2)
          )
        )
      )
    )

    assertEquals(actualScenarios2, expected2)

  }

  test("we cannot add screenshots to a scenario that is in FAILED state") {

    val scenario: Scenario = ReferenceData.startingScenario.copy(testOutcome = FAILED)

    val actual1: Either[String, (Scenarios, Screenshot)] =
      Scenarios(scenariosMap = Map("desc" -> scenario)).withNewScreenshot(scenario.ordinal, scenario.description, ReferenceData.screenshotDriverData.url1)

    assertEquals(actual1.map(_._1), "Sorry last scenario 'desc' does not have testOutcome equal to STARTING but FAILED".asLeft)

  }

  test("we cannot add screenshots to a scenario that is in SUCCEEDED state") {

    val scenario: Scenario = ReferenceData.startingScenario.copy(testOutcome = SUCCEEDED)

    val actual1: Either[String, (Scenarios, Screenshot)] =
      Scenarios(scenariosMap = Map("desc" -> scenario)).withNewScreenshot(scenario.ordinal, scenario.description, ReferenceData.screenshotDriverData.url1)

    assertEquals(actual1.map(_._1), "Sorry last scenario 'desc' does not have testOutcome equal to STARTING but SUCCEEDED".asLeft)

  }

}
