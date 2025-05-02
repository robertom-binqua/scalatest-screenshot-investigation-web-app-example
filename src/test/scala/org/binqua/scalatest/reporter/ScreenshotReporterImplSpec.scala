package org.binqua.scalatest.reporter

import cats.implicits.catsSyntaxEitherId
import munit.FunSuite
import org.scalatest.events.Ordinal

class ScreenshotReporterImplSpec extends FunSuite {

  test(
    "Given a suiteClassName <Feature: f1: Navigation bar should work Scenario: s1: we can go from home to page3> toFeatureAndScenario returns feature and scenario"
  ) {
    assertEquals(
      Utils.createARunningScenario(new Ordinal(1), "test", "Feature: a a a Scenario: b b b"),
      RunningScenario(new Ordinal(1), "test", "a a a", "b b b").asRight
    )
    assertEquals(
      Utils.createARunningScenario(new Ordinal(1), "test", "Feature: a a a    Scenario: b b b   "),
      RunningScenario(new Ordinal(1), "test", "a a a   ", "b b b   ").asRight
    )
    assertEquals(
      Utils.createARunningScenario(new Ordinal(1), "test", "Feature: f1: Navigation bar should work Scenario: s1: we can go from home to page3"),
      RunningScenario(new Ordinal(1), "test", "f1: Navigation bar should work", "s1: we can go from home to page3").asRight
    )
  }

}
