package org.binqua.examples.http4sapp.app

import org.scalatest.Reporter
import org.scalatest.events._

class ScreenshotReporterRunner extends Reporter {
  val reporter = new ScreenshotReporterImpl(TestsCollector.testsCollector)
  override def apply(event: Event): Unit = reporter.apply(event)
}

class ScreenshotReporterImpl(testsCollector: TestsCollector) extends Reporter {

  override def apply(event: Event): Unit = {
    event match {
      case TestStarting(ordinal, _, testName, _, feature, scenario, _, _, _, _, _, timestamp) =>
        testsCollector.add(StateEvent.TestStarting(RunningScenario(ordinal, testName, feature, scenario), timestamp))

      case TestFailed(ordinal, _, _, testName, _, feature, scenario, _, _, _, _, _, _, _, _, _, timestamp) =>
        testsCollector.add(StateEvent.TestFailed(RunningScenario(ordinal, testName, feature, scenario), timestamp))

      case TestSucceeded(ordinal, _, testName, _, feature, scenario, _, _, _, _, _, _, _, timestamp) =>
        testsCollector.add(StateEvent.TestSucceeded(RunningScenario(ordinal, testName, feature, scenario), timestamp))

      case NoteProvided(ordinal, message, nameInfo, _, _, _, _, _, _) =>
        println(s"NoteProvided $ordinal $message $nameInfo")

      case e =>
        println(s"info $e}")

    }
  }

}
