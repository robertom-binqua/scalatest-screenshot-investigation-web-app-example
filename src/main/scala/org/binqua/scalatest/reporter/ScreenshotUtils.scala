package org.binqua.scalatest.reporter

object ScreenshotUtils {

  val testsCollector: TestsCollector = TestsCollector.testsCollector

  def createScreenshotOnEnter(screenshotDriverData: ScreenshotDriverData): Unit =
    testsCollector.addScreenshotOnEnterAt(screenshotDriverData)

  def createScreenshotOnExit(screenshotDriverData: ScreenshotDriverData): Unit =
    testsCollector.addScreenshotOnExitAt(screenshotDriverData)
}
