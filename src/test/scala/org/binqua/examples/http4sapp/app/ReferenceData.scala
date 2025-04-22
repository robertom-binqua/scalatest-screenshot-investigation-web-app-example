package org.binqua.examples.http4sapp.app

import org.binqua.examples.http4sapp.app.TestOutcome.STARTING
import org.scalatest.events.Ordinal

object ReferenceData {

  val startingScenario: Scenario =  Scenario(
    ordinal = new Ordinal(1).next,
    description = "desc",
    startedTimestamp = 1L,
    finishedTimestamp = Option.empty,
    screenshots = Option.empty,
    steps = Option.empty,
    testOutcome = STARTING,
    throwable = None
  )

}
