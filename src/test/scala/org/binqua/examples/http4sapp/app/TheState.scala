package org.binqua.examples.http4sapp.app

import org.binqua.examples.http4sapp.app.ScreenshotMoment._
import org.binqua.examples.http4sapp.util.utils.EitherOps

import java.io.File

object TheState extends State {

  val root = new File(s"screenshots/")

  var tests: Tests = Tests(Map.empty)

  def add(event: StateEvent): Unit =
    event match {
      case StateEvent.TestStarting(runningScenario, timestamp) =>
        tests = tests.testStarting(runningScenario, timestamp).getOrThrow

      case StateEvent.TestFailed(runningScenario, timestamp) =>
        tests = tests.testFailed(runningScenario, timestamp).getOrThrow

      case StateEvent.TestSucceeded(runningScenario, timestamp) =>
        tests = tests.testSucceeded(runningScenario, timestamp).getOrThrow
    }

  def createReport(): Unit = ???

  override def addScreenshotOnEnterAt(pageUrl: String, runningScenario: RunningScenario): Either[String, (Tests, File)] =
    addScreenshot(runningScenario, pageUrl, ON_ENTER_PAGE)

  override def addScreenshotOnExitAt(pageUrl: String, runningScenario: RunningScenario): Either[String, (Tests, File)] =
    addScreenshot(runningScenario, pageUrl, ON_EXIT_PAGE)

  private def addScreenshot(testRunningInfo: RunningScenario, pageUrl: String, screenshotMoment: ScreenshotMoment): Either[String, (Tests, File)] =
    tests
      .addScreenshot(testRunningInfo, pageUrl, screenshotMoment)
      .map(r => (r._1, new File(root.getAbsolutePath + File.separator + r._2)))

}
