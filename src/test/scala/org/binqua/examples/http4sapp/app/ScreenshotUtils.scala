package org.binqua.examples.http4sapp.app

import java.io.File

object ScreenshotUtils {

  val testsCollector: TestsCollector = TestsCollector.testsCollector

  def createScreenshotOnEnter(scrFile: File, pageUrl: String): Unit =
    testsCollector
      .addScreenshotOnEnterAt(scrFile, pageUrl)

  def createScreenshotOnExit(scrFile: File, pageUrl: String): Unit =
    testsCollector
      .addScreenshotOnExitAt(scrFile, pageUrl)
}
