package org.binqua.examples.http4sapp

import org.scalatest.Reporter
import org.scalatest.events._

class ScreenshotReporterImpl extends Reporter {

  var state: State = TheState

  override def apply(event: Event): Unit = {
    event match {
      case runStarting: RunStarting =>

      case TestStarting(_, _, testName, _, feature, scenario, _, _, _, _, _, timestamp) => {
        state.add(StateEvent.TestStarting(testName, feature, scenario, timestamp))
      }
      case TestFailed(_, _, _, testName, _, feature, scenario, _, _, _, _, _, _, _, _, _, _) => {
        state.add(StateEvent.TestFailed(testName, feature, scenario))
      }
      case TestSucceeded(_, _, testName, _, feature, scenario, _, _, _, _, _, _, _, _) => {
        state.add(StateEvent.TestSucceeded(testName, feature, scenario))
      }
      case _ =>
      //        println(event)

    }
  }

}





