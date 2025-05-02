package org.binqua.scalatest.reporter

import org.binqua.scalatest.reporter.TestOutcome.STARTING
import org.scalatest.events.Ordinal

import java.nio.file.Files

object ReferenceData {

  val startingScenario: Scenario =  Scenario(
    ordinal = new Ordinal(1).next,
    description = "desc",
    startedTimestamp = 1L,
    finishedTimestamp = Option.empty,
    screenshots = Nil,
    steps = Option.empty,
    testOutcome = STARTING,
    throwable = None
  )

  def screenshotDriverData: ScreenshotDriverData = ScreenshotDriverData(screenshotImage = aDummyScreenshot, pageSource = "source", pageUrl = "url1")

  private def aDummyScreenshot =
    Files.createTempFile("doesNotMatter", "png").toFile


}
