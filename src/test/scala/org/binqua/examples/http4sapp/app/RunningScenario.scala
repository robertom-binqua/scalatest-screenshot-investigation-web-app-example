package org.binqua.examples.http4sapp.app

import org.scalatest.events.Ordinal

case class RunningScenario(ordinal: Ordinal, test: String, feature: String, scenario: String)
