package org.binqua.scalatest.reporter

import org.scalatest.events.Ordinal

case class RunningScenario(ordinal: Ordinal, test: String, feature: String, scenario: String)
