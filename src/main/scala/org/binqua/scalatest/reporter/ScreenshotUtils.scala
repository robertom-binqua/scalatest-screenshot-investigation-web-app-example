package org.binqua.scalatest.reporter

object ScreenshotUtils {

  val testsCollector: TestsCollector = TestsCollector.testsCollector

  def createScreenshot(screenshotDriverData: ScreenshotDriverData): Unit =
    testsCollector.addScreenshot(screenshotDriverData)

}
