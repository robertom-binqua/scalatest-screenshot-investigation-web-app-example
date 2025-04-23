package org.binqua.examples.http4sapp.app

object ScreenshotUtils {

  val testsCollector: TestsCollector = TestsCollector.testsCollector

  def createScreenshotOnEnter(screenshotDriverData: ScreenshotDriverData): Unit =
    testsCollector.addScreenshotOnEnterAt(screenshotDriverData)

  def createScreenshotOnExit(screenshotDriverData: ScreenshotDriverData): Unit =
    testsCollector.addScreenshotOnExitAt(screenshotDriverData)
}
