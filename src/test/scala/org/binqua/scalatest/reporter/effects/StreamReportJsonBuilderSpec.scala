package org.binqua.scalatest.reporter.effects

import munit.CatsEffectSuite
import org.binqua.scalatest.reporter.StateEvent.{RecordedEvents, RunCompleted, RunStarting, Screenshot, TestStarting, TestSucceeded}
import org.binqua.scalatest.reporter.{ReferenceData, RunningScenario, TestsReport}
import org.scalatest.events.Ordinal

class StreamReportJsonBuilderSpec extends CatsEffectSuite {


  test(""){
    val runningScenario = RunningScenario(new Ordinal(1), "t1", "f1", "s1")
    val t2 = runningScenario.copy(test = "t2")

    val events = List(
      RunStarting(1),
      TestStarting(runningScenario, 2),
      Screenshot(runningScenario, ReferenceData.screenshotDriverData.url1, 2),
      TestSucceeded(runningScenario, RecordedEvents.empty, 3),
      TestStarting(t2, 2),
      Screenshot(t2, ReferenceData.screenshotDriverData.url1, 2),
      TestSucceeded(t2, RecordedEvents.empty, 3),
      RunCompleted(4)
    )


  }

}
