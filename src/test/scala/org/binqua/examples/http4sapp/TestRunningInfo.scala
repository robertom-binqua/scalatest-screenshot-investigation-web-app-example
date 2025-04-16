package org.binqua.examples.http4sapp

import org.scalatest.events.Ordinal

case class TestRunningInfo(ordinal: Ordinal, testName: String, feature: String, scenario: String)

object TestRunningInfo {

  val testRunningInfo: ThreadLocal[TestRunningInfo] = new ThreadLocal[TestRunningInfo]

}
