package org.binqua.examples.http4sapp.app

import cats.implicits.catsSyntaxEitherId
import munit.FunSuite

class ScreenshotReporterImplSpec extends FunSuite {

  test(
    "Given a suiteClassName <Feature: f1: Navigation bar should work Scenario: s1: we can go from home to page3> toFeatureAndScenario returns feature and scenario"
  ) {
    assertEquals(Utils.toFeatureAndScenario("Feature: a a a Scenario: b b b"), ("a a a", "b b b").asRight)
    assertEquals(Utils.toFeatureAndScenario("Feature: a a a    Scenario: b b b   "), ("a a a   ", "b b b   ").asRight)
    assertEquals(Utils.toFeatureAndScenario("Feature: f1: Navigation bar should work Scenario: s1: we can go from home to page3"), ("f1: Navigation bar should work", "s1: we can go from home to page3").asRight)
  }

}
