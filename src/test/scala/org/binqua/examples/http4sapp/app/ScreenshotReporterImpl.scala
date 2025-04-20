package org.binqua.examples.http4sapp.app

import org.binqua.examples.http4sapp.util.utils.EitherOps
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

      case NoteProvided(ordinal, message, Some(NameInfo(_, _, Some(suiteClassName), Some(testName))), throwable , _, _, _, _, timestamp) =>
        org.binqua.examples.http4sapp.app.Utils
          .toFeatureAndScenario(suiteClassName)
          .map(input => {
            val (featureName, scenarioName) = input
            testsCollector.add(StateEvent.Note(RunningScenario(ordinal = ordinal, test = testName, featureName, scenarioName), message, throwable, timestamp))
          })
          .getOrThrow

      case e =>
        println(s"info $e}")

    }
  }

}
