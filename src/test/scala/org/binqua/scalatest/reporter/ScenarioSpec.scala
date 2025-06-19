package org.binqua.scalatest.reporter

import munit.FunSuite
import org.binqua.scalatest.reporter.ReferenceData.screenshotDriverData
import org.binqua.scalatest.reporter.TestOutcome.STARTING
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

    val (actualScenario1, _) = Scenario.addScreenshot(scenario = startingScenario, screenshotExternalData = ReferenceData.screenshotDriverData.url1)

    assertEquals(
      actualScenario1,
      startingScenario.copy(screenshots =
        List(Screenshot(screenshotDriverData = ReferenceData.screenshotDriverData.url1, ordinal = startingScenario.ordinal, index = 1))
      )
    )

    val (actualScenario2, _) = Scenario.addScreenshot(scenario = actualScenario1, screenshotExternalData = ReferenceData.screenshotDriverData.url2)

    assertEquals(
      actualScenario2,
      startingScenario.copy(screenshots =
        List(
          Screenshot(ReferenceData.screenshotDriverData.url1, startingScenario.ordinal, 1),
          Screenshot(ReferenceData.screenshotDriverData.url2, startingScenario.ordinal, 2)
        )
      )
    )

    val thirdScreenshotExternalData = ReferenceData.screenshotDriverData.url3
    val (actualScenario3, _) = Scenario.addScreenshot(scenario = actualScenario2, screenshotExternalData = thirdScreenshotExternalData)

    assertEquals(
      actualScenario3,
      startingScenario.copy(screenshots =
        List(
          Screenshot(screenshotDriverData.url1, startingScenario.ordinal, 1),
          Screenshot(screenshotDriverData.url2, startingScenario.ordinal, 2),
          Screenshot(screenshotDriverData.url3, startingScenario.ordinal, 3)
        )
      )
    )

  }
}
