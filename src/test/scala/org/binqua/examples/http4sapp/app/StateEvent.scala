package org.binqua.examples.http4sapp.app

trait StateEvent

object StateEvent {
  case class TestStarting(runningScenario: RunningScenario, timestamp: Long) extends StateEvent

  case class TestFailed(runningScenario: RunningScenario, timestamp: Long) extends StateEvent

  case class TestSucceeded(runningScenario: RunningScenario, timestamp: Long) extends StateEvent
}
