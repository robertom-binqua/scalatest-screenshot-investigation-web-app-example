package org.binqua.examples.http4sapp

import org.scalatest.Reporter
import org.scalatest.events._

class ScreenshotReporterImpl extends Reporter {

  var state: State = TheState

  override def apply(event: Event): Unit = {
    event match {
      case TestStarting(ordinal, _, testName, _, feature, scenario, _, _, _, _, _, timestamp) => {
        state.add(StateEvent.TestStarting(RunningScenario(ordinal, testName, feature, scenario), timestamp))
      }
      case tf @ TestFailed(ordinal, _, _, testName, _, feature, scenario, _, _, _, _, _, _, _, _, _, timestamp) => {
        state.add(StateEvent.TestFailed(RunningScenario(ordinal, testName, feature, scenario), timestamp))
      }
      case TestSucceeded(ordinal, _, testName, _, feature, scenario, _, _, _, _, _, _, _, timestamp) => {
        state.add(StateEvent.TestSucceeded(RunningScenario(ordinal, testName, feature, scenario), timestamp))
      }
      case InfoProvided(ordinal, message, nameInfo, _, _, _, _, _, _) =>
        println(s"info $ordinal $message} $nameInfo")

      case NoteProvided(ordinal, message, nameInfo, _, _, _, _, _, _) =>
        println(s"NoteProvided $ordinal $message $nameInfo")
      case e =>
        println(s"info $e}")

    }
  }

}
