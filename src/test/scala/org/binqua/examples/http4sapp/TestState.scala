package org.binqua.examples.http4sapp


import org.apache.commons.io.FileUtils
import org.openqa.selenium.WebDriver

import java.io.File

trait State {

  def add(event: StateEvent): Unit

  def createReport(): Unit

  def addScreenshotOnEnterAt(pageUrl: String): Option[File]

  def addScreenshotOnExitAt(pageUrl: String): Option[File]

}

case class ScenarioNumber(x: Int) {
  def next(): ScenarioNumber = ScenarioNumber(x + 1)

  def asString(): String = s"S$x"
}

case class FeatureNumber(x: Int) {
  def next(): FeatureNumber = FeatureNumber(x + 1)

  def asString(): String = s"F$x"
}

case class TestNumber(x: Int) {
  def next(): TestNumber = TestNumber(x + 1)

  def asString(): String = s"T$x"
}

case class ScreenshotNumber(x: Int) {
  def next(): ScreenshotNumber = ScreenshotNumber(x + 1)

  def asString(): String = s"SS$x"
}

trait Counter[A] {
  def next(): A
}

case object TestCounter extends Counter[TestNumber] {
  var testNumber: TestNumber = TestNumber(0)

  def next(): TestNumber = {
    testNumber = testNumber.next()
    testNumber
  }
}

case object FeatureCounter extends Counter[FeatureNumber] {
  var featureNumber: FeatureNumber = FeatureNumber(0)

  def next(): FeatureNumber = {
    featureNumber = featureNumber.next()
    featureNumber
  }
}

case object ScenarioCounter extends Counter[ScenarioNumber] {
  var scenarioNumber: ScenarioNumber = ScenarioNumber(0)

  def next(): ScenarioNumber = {
    scenarioNumber = scenarioNumber.next()
    scenarioNumber
  }
}

case class ScreenshotCounter() extends Counter[ScreenshotNumber] {
  var screenshotNumber: ScreenshotNumber = ScreenshotNumber(0)

  def next(): ScreenshotNumber = {
    screenshotNumber = screenshotNumber.next()
    screenshotNumber
  }
}

case class TestDetail(testName: String, featureNumber: FeatureNumber, scenarioNumber: ScenarioNumber, scenarioDescription: String) {
  def dirName: String = {
    val takeTestName = testName.reverse.takeWhile(_ != '.').reverse
    s"${takeTestName}_${featureNumber.asString()}_${scenarioNumber.asString()}"
  }
}

case class Screenshot(screenshotNumber: ScreenshotNumber, url: String)

case class Scenario(id: ScenarioNumber, description: String, screenshots: Option[List[Screenshot]])

case class Feature(id: FeatureNumber, description: String, scenarios: List[Scenario])

case class Test(name: String, features: List[Feature]) {
  val internalFeaturesMap: Map[String, Feature] = features.map(f => (f.description, f)).toMap

  def add(featureDescription: String, scenarioDescription: String, screenshots: List[Screenshot]): Option[Test] = {
    for {
      feature <- internalFeaturesMap.get(featureDescription)
      updatedScenario: Iterable[Scenario] <- Some(feature
        .scenarios
        .map(s => (s.description, s))
        .toMap
        .updatedWith(scenarioDescription) {
          case Some(scenario) => Some(scenario.copy(screenshots = Some(screenshots)))
          case None => ???
        }.values)
      updatedFeature <- Some(internalFeaturesMap.updatedWith(featureDescription)({
          case Some(feature) => Some(feature.copy(scenarios = updatedScenario.toList))
          case None => ???
        })
        .values
        .toList)
    } yield this.copy(features = updatedFeature)
  }

  def add(featureDescription: String, scenarioDescription: String): Option[Test] = {
    internalFeaturesMap.get(featureDescription) match {
      case Some(value) => add(value.description, scenarioDescription, List.empty)
      case None => Some(Test(name, List(Feature(FeatureCounter.next(), featureDescription, List(Scenario(ScenarioCounter.next(), scenarioDescription, None))))))
    }
  }

  def getTestDetails: TestDetail = {
    TestDetail(name, features.last.id, features.last.scenarios.last.id, features.last.scenarios.last.description)
  }
}

object TheState extends State {

  val root = new File(s"screenshots/")

  var screenshotCounter: ScreenshotCounter = ScreenshotCounter()

  var testMap: Map[String, Test] = Map.empty
  var screenshots: List[Screenshot] = List.empty
  var currentTest: Option[Test] = None

  def add(event: StateEvent): Unit = {
    event match {
      case StateEvent.TestStarting(testName, featureDescription, scenarioDescription, timestamp) =>
        testMap = testMap.updatedWith(testName) {
          case None => Some(Test(testName, List(Feature(FeatureCounter.next(), featureDescription, List(Scenario(ScenarioCounter.next(), scenarioDescription, None))))))
          case Some(oldValueTest) => oldValueTest.add(featureDescription, scenarioDescription)
        }
        currentTest = testMap.get(testName)

      case StateEvent.TestFailed(testName, featureDescription, scenarioDescription) =>
        testMap = testMap.updatedWith(testName) {
          case None => None
          case Some(test) => test.add(featureDescription, scenarioDescription, screenshots)
        }
        screenshotCounter = ScreenshotCounter()
        currentTest = None

      case StateEvent.TestSucceeded(testName, featureDescription, scenarioDescription) =>
        testMap = testMap.updatedWith(testName) {
          case None => None
          case Some(test) => test.add(featureDescription, scenarioDescription, screenshots)
        }
        screenshotCounter = ScreenshotCounter()
        currentTest = None
      case _ =>
    }
  }

  private def screenshotFileName(root: File, testDetail: TestDetail, screenshotNumber: ScreenshotNumber, onEnter: String): File = {
    new File(root.getAbsolutePath + File.separator + testDetail.dirName + File.separator + screenshotNumber.asString() + s"_$onEnter.png")
  }

  def createReport(): Unit = ???

  override def addScreenshotOnEnterAt(pageUrl: String): Option[File] =
    addScreenshot(pageUrl, "onEnter")

  override def addScreenshotOnExitAt(pageUrl: String): Option[File] =
    addScreenshot(pageUrl, "onExit")

  private def addScreenshot(pageUrl: String, identifier: String): Option[File] =
    currentTest.map { test =>
      screenshots = Screenshot(screenshotCounter.next(), pageUrl) :: screenshots
      screenshotFileName(root, test.getTestDetails, screenshots.head.screenshotNumber, identifier)
    }

}

object ScreenshotUtils {
  def createScreenshotOnEnter(scrFile: File, pageUrl: String, state: State, webDriver: WebDriver): Unit = {
    val file = scrFile
    state.addScreenshotOnEnterAt(pageUrl).foreach(f => {
      FileUtils.copyFile(file, f)
    })
  }

  def createScreenshotOnExit(scrFile: File, pageUrl: String, state: State, webDriver: WebDriver): Unit = {
    val file = scrFile
    state.addScreenshotOnExitAt(pageUrl).foreach(f => {
      FileUtils.copyFile(file, f)
    })
  }
}

trait StateEvent

object StateEvent {
  case class TestStarting(testName: String, feature: String, scenario: String, timestamp: Long) extends StateEvent

  case class TestFailed(testName: String, feature: String, scenario: String) extends StateEvent

  case class TestSucceeded(testName: String, feature: String, scenario: String) extends StateEvent
}
