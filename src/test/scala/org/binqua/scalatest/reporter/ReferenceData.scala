package org.binqua.scalatest.reporter

import org.binqua.scalatest.reporter.ScreenshotMoment.{ON_ENTER_PAGE, ON_EXIT_PAGE}
import org.binqua.scalatest.reporter.TestOutcome.STARTING
import org.scalatest.events.Ordinal

import java.io.File
import java.nio.file.Files

object ReferenceData {

  val startingScenario: Scenario = Scenario(
    ordinal = new Ordinal(1).next,
    description = "desc",
    startedTimestamp = 1L,
    finishedTimestamp = Option.empty,
    screenshots = Nil,
    steps = Option.empty,
    testOutcome = STARTING,
    throwable = None
  )

  object screenshotExternalData {
    val url1: ScreenshotExternalData = ScreenshotExternalData(pageUrl = "url1", pageTitle = "title 1", ON_EXIT_PAGE)
    val url2: ScreenshotExternalData = ScreenshotExternalData(pageUrl = "url2", pageTitle = "title 2", ON_ENTER_PAGE)
    val url3: ScreenshotExternalData = ScreenshotExternalData(pageUrl = "url3", pageTitle = "title 3", ON_EXIT_PAGE)
    val url4: ScreenshotExternalData = ScreenshotExternalData(pageUrl = "url4", pageTitle = "title 4", ON_ENTER_PAGE)
  }

  object screenshotDriverData {
    val url1: ScreenshotDriverData = ScreenshotDriverData(image = aDummyScreenshot, pageSource = "source1", screenshotExternalData.url1)
    val url2: ScreenshotDriverData = ScreenshotDriverData(image = aDummyScreenshot, pageSource = "source2", screenshotExternalData.url2)
    val url3: ScreenshotDriverData = ScreenshotDriverData(image = aDummyScreenshot, pageSource = "source3", screenshotExternalData.url3)
    val url4: ScreenshotDriverData = ScreenshotDriverData(image = aDummyScreenshot, pageSource = "source3", screenshotExternalData.url4)
  }

  private def aDummyScreenshot: File = Files.createTempFile("doesNotMatter", "png").toFile

}
