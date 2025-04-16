package org.binqua.examples.http4sapp

import org.scalatest.Reporter
import org.scalatest.events._

class ScreenshotReporterImpl extends Reporter {

  var state: State = TheState

  override def apply(event: Event): Unit = {
    event match {
      case runStarting: RunStarting =>

      case TestStarting(ordinal, _, testName, _, feature, scenario, _, _, _, _, _, timestamp) => {
        println(s"starting ${ordinal.toString()} $timestamp")
        state.add(StateEvent.TestStarting(ordinal,testName, feature, scenario, timestamp))
      }
      case tf@TestFailed(ordinal, _, _, testName, _, feature, scenario, _, _, _, _, _, _, _, _, _, timestamp) => {
        println(s"failed $tf ${ordinal.toString()} $timestamp")
        state.add(StateEvent.TestFailed(testName, feature, scenario, timestamp))
      }
      case TestSucceeded(ordinal, _, testName, _, feature, scenario, _, _, _, _, _, _, _, timestamp) => {
        println(s"TestSucceeded ${ordinal.toString()} $timestamp")
        state.add(StateEvent.TestSucceeded(testName, feature, scenario, timestamp))
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





