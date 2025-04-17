package org.binqua.examples.http4sapp.app

import org.apache.commons.io.FileUtils
import org.binqua.examples.http4sapp.util.utils.EitherOps

import java.io.File

object ScreenshotUtils {
  def createScreenshotOnEnter(scrFile: File, pageUrl: String, state: State, testRunningInfo: RunningScenario): Unit =
    state
      .addScreenshotOnEnterAt(pageUrl, testRunningInfo)
      .map((r: (Tests, File)) => FileUtils.copyFile(scrFile, r._2))
      .getOrThrow

  def createScreenshotOnExit(scrFile: File, pageUrl: String, state: State, testRunningInfo: RunningScenario): Unit =
    state
      .addScreenshotOnExitAt(pageUrl, testRunningInfo)
      .map((r: (Tests, File)) => FileUtils.copyFile(scrFile, r._2))
      .getOrThrow
}
