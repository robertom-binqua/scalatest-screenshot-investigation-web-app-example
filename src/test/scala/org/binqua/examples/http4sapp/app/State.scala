package org.binqua.examples.http4sapp.app

import java.io.File

trait State {

  def add(event: StateEvent): Unit

  def createReport(): Unit

  def addScreenshotOnEnterAt(pageUrl: String, runningScenario: RunningScenario): Either[String, (Tests, File)]

  def addScreenshotOnExitAt(pageUrl: String, runningScenario: RunningScenario): Either[String, (Tests, File)]

}