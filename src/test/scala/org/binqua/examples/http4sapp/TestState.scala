package org.binqua.examples.http4sapp


import org.apache.commons.io.FileUtils
import org.binqua.examples.http4sapp.ScreenshotMoment.{ON_ENTER_PAGE, ON_EXIT_PAGE}
import org.binqua.examples.http4sapp.TestOutcome.{FAILED, STARTING, SUCCESSFUL}
import org.scalatest.events.Ordinal

import java.io.File

trait State {

  def add(event: StateEvent): Unit

  def createReport(): Unit

  def addScreenshotOnEnterAt(pageUrl: String): Either[String, File]

  def addScreenshotOnExitAt(pageUrl: String): Either[String, File]

}

case class Screenshot(pageUrl: String, screenshotMoment: ScreenshotMoment, ordinal: Ordinal, index: Int) {
  def toFile: File = new File(ordinal.toList.mkString("_") + File.separator + s"screenshot_${index}_$screenshotMoment.png")
}

object Scenario {
  def starting(ordinal: Ordinal, scenarioDescription: String, timestamp: Long): Scenario = Scenario(ordinal, scenarioDescription, timestamp, None, None, TestOutcome.STARTING)
}

case class Scenario(ordinal: Ordinal, description: String, startedTimestamp: Long, finishedTimestamp: Option[Long], screenshots: Option[List[Screenshot]], testOutcome: TestOutcome) {

  def withNewScreenshot(pageUrl: String, screenshotMoment: ScreenshotMoment): Scenario =
    this.copy(screenshots =
      screenshots
        .map(s => Screenshot(pageUrl, screenshotMoment, ordinal, s.size + 1) :: s)
        .orElse(Some(List(Screenshot(pageUrl, screenshotMoment, ordinal, 1)))))
}

case class Scenarios(scenarios: List[Scenario]) {
  def testUpdate(scenarioDescription: String, timestamp: Long, newState: TestOutcome): Either[String, Scenarios] =
    scenarios.headOption.map(lastScenario =>
      if (lastScenario.description != scenarioDescription)
        Left(s"Scenario description is ${lastScenario.description} and should be $scenarioDescription")
      else if (lastScenario.testOutcome != TestOutcome.STARTING)
        Left(s"lastScenario testOutcome is ${lastScenario.testOutcome} and should be ${TestOutcome.STARTING}")
      else
        Right(this.copy(scenarios = lastScenario.copy(finishedTimestamp = Some(timestamp), testOutcome = newState) :: scenarios.tail))
    ).getOrElse(Left("no scenario"))

  def testStarting(ordinal: Ordinal, scenarioDescription: String, timestamp: Long): Either[String, Scenarios] =
    scenarios.headOption.map(lastScenario => {
      if (lastScenario.description == scenarioDescription)
        Left("test error bro")
      else
        Right(this.copy(scenarios = Scenario.starting(ordinal, scenarioDescription, timestamp) :: scenarios.tail))
    }).getOrElse(Right(this.copy(scenarios = Scenario.starting(ordinal, scenarioDescription, timestamp) :: Nil)))


  def withNewScreenshot(pageUrl: String, screenshotMoment: ScreenshotMoment): Either[String, Scenarios] =
    scenarios
      .headOption
      .map(lastScenario =>
        if (lastScenario.testOutcome == TestOutcome.STARTING)
          Right(this.copy(scenarios = lastScenario.withNewScreenshot(pageUrl, screenshotMoment) :: scenarios.tail))
        else
          Left("last scenario does not have testOutcome == STARTING")
      ).getOrElse(Left("there are not scenario"))

}

case class Feature(description: String, scenarios: Scenarios) {
  def withNewScenario(ordinal: Ordinal, scenarioDescription: String, timestamp: Long): Either[String, Feature] = scenarios
    .testStarting(ordinal, scenarioDescription, timestamp)
    .map(newScenarios => this.copy(scenarios = newScenarios))

  def withNewScreenshot(pageUrl: String, screenshotMoment: ScreenshotMoment): Either[String, Feature] =
    scenarios.withNewScreenshot(pageUrl, screenshotMoment)
      .map(newScenarios => this.copy(scenarios = newScenarios))

}

case class Features(features: List[Feature]) {

  def testUpdated(featureDescription: String, scenarioDescription: String, updatedTimestamp: Long, newState: TestOutcome): Either[String, Features] =
    features.headOption.map(featureFound => {
      if (featureFound.description == featureDescription)
        featureFound
          .scenarios
          .testUpdate(scenarioDescription, updatedTimestamp, newState)
          .map((updatedScenario: Scenarios) => featureFound.copy(scenarios = updatedScenario))
          .map(f => this.copy(features = f :: features.tail))
      else
        Left(s"last feature does not have featureDescription equals to $featureDescription")
    }).getOrElse(Left(s"There is no feature defined"))

  def newTestStarting(ordinal: Ordinal, featureDescription: String, scenarioDescription: String, timestamp: Long): Either[String, Features] =
    features.headOption match {
      case Some(lastFeature) =>
        lastFeature.withNewScenario(ordinal, scenarioDescription, timestamp).map(updatedFeature => Features(updatedFeature :: features.tail))
      case None =>
        val newF = Feature(featureDescription, Scenarios(List(Scenario(ordinal, scenarioDescription, timestamp, None, None, TestOutcome.STARTING))))
        Right(Features(newF :: features.tail))
    }

  def addScreenshot(pageUrl: String, screenshotMoment: ScreenshotMoment): Either[String, Features] = features
    .headOption
    .toRight("there are not features. I cannot add a screenshot")
    .flatMap(lastFeature => lastFeature.withNewScreenshot(pageUrl, screenshotMoment).map(updatedFeature => Features(updatedFeature :: features.tail)))

}

object Features {
  def starting(ordinal: Ordinal, featureDescription: String, scenarioDescription: String, timestamp: Long): Features =
    Features(features = List(Feature(featureDescription, Scenarios(List(Scenario(ordinal, scenarioDescription, timestamp, None, None, TestOutcome.STARTING))))))
}

case class CurrentTest(testName: String, featureDescription: String, scenarioDescription: String)

case class Tests(tests: List[Test]) {

  def screenshotSuffix(): Either[String, File] =
    tests
      .headOption
      .toRight("there are not tests. I cannot calculate screenshotSuffix")
      .flatMap(lastTest => lastTest.features.features.headOption.toRight("there are not features. I cannot calculate screenshotSuffix").map((lastTest, _)))
      .flatMap((pair: (Test, Feature)) => {
        val (_, lastScenario) = pair
        lastScenario.scenarios.scenarios.headOption.toRight("there are not scenarios. I cannot calculate screenshotSuffix").flatMap(lastScenario =>
          if (lastScenario.testOutcome != STARTING)
            Left(s"Scenario is ${lastScenario.testOutcome} and should be ${TestOutcome.STARTING} to calculate screenshotSuffix")
          else
            lastScenario
              .screenshots
              .toRight("there are not screenshots. I cannot calculate screenshotSuffix")
              .flatMap((screenshots: List[Screenshot]) => screenshots.headOption.toRight("there are not screenshots. I cannot calculate screenshotSuffix"))
              .map(_.toFile)
        )
      })

  def addScreenshot(pageUrl: String, screenshotMoment: ScreenshotMoment): Either[String, Tests] = tests
    .headOption
    .toRight("I cannot add screenshots because there are no tests")
    .flatMap(lastTest => lastTest
      .addScreenshot(pageUrl, screenshotMoment)
      .map(updatedTest => Tests(tests = updatedTest :: tests.tail))
    )

  def testStarting(ordinal: Ordinal, testName: String, featureDescription: String, scenarioDescription: String, timestamp: Long): Either[String, Tests] =
    tests match {
      case ::(lastTest, previousTests) if (lastTest.name == testName) =>
        lastTest
          .withNewFeatureOrScenario(ordinal, featureDescription, scenarioDescription, timestamp)
          .map(updatedTest => Tests(tests = updatedTest :: previousTests))
      case _ =>
        Right(Tests(tests = Test(testName, Features.starting(ordinal, featureDescription, scenarioDescription, timestamp)) :: tests))
    }

  def testFailed(testName: String, featureDescription: String, scenarioDescription: String, timestamp: Long): Either[String, Tests] =
    tests match {
      case ::(lastTest, next) if (lastTest.name == testName) =>
        lastTest.markAsFailed(featureDescription, scenarioDescription, timestamp).map((testUpdated: Test) => Tests(testUpdated :: next))
      case _ => Left(s"I was going to update to test $testName to failed but test $testName does not exist")
    }

  def testSucceeded(testName: String, featureDescription: String, scenarioDescription: String, timestamp: Long): Either[String, Tests] =
    tests match {
      case ::(lastTest, next) if (lastTest.name == testName) =>
        lastTest.markAsSucceeded(featureDescription, scenarioDescription, timestamp).map((testUpdated: Test) => Tests(testUpdated :: next))
      case _ => Left(s"I was going to update to test $testName to failed but test $testName does not exist")
    }

}

case class Test(name: String, features: Features) {

  def withNewFeatureOrScenario(ordinal: Ordinal, featureDescription: String, scenarioDescription: String, timestamp: Long): Either[String, Test] =
    features.newTestStarting(ordinal, featureDescription, scenarioDescription, timestamp).map(newFeatures => this.copy(features = newFeatures))

  def markAsFailed(featureDescription: String, scenarioDescription: String, failedTimestamp: Long): Either[String, Test] =
    features.testUpdated(featureDescription, scenarioDescription, failedTimestamp, FAILED).map(newFeatures => this.copy(features = newFeatures))

  def markAsSucceeded(featureDescription: String, scenarioDescription: String, timestamp: Long): Either[String, Test] =
    features.testUpdated(featureDescription, scenarioDescription, timestamp, SUCCESSFUL).map(newFeatures => this.copy(features = newFeatures))

  def addScreenshot(pageUrl: String, screenshotMoment: ScreenshotMoment): Either[String, Test] = features.addScreenshot(pageUrl, screenshotMoment).map(newFeatures => this.copy(features = newFeatures))

}

object TheState extends State {

  val root = new File(s"screenshots/")

  var tests: Tests = Tests(List.empty)

  var screenshots: List[Screenshot] = List.empty

  def add(event: StateEvent): Unit = {
    event match {
      case StateEvent.TestStarting(ordinal, testName, featureDescription, scenarioDescription, timestamp) =>
        tests.testStarting(ordinal, testName, featureDescription, scenarioDescription, timestamp) match {
          case Left(value) => throw new IllegalStateException(value)
          case Right(newTests) =>
            tests = newTests
        }

      case StateEvent.TestFailed(testName, featureDescription, scenarioDescription, timestamp) =>
        tests.testFailed(testName, featureDescription, scenarioDescription, timestamp) match {
          case Left(value) => throw new IllegalStateException(value)
          case Right(newTests) =>
            tests = newTests
        }

      case StateEvent.TestSucceeded(testName, featureDescription, scenarioDescription, timestamp) =>
        tests.testSucceeded(testName, featureDescription, scenarioDescription, timestamp) match {
          case Left(error) => throw new IllegalStateException(error)
          case Right(newTests) =>
            tests = newTests
        }

      case _ =>
    }
  }

  def createReport(): Unit = ???

  override def addScreenshotOnEnterAt(pageUrl: String): Either[String, File] =
    addScreenshot(pageUrl, ON_ENTER_PAGE)

  override def addScreenshotOnExitAt(pageUrl: String): Either[String, File] =
    addScreenshot(pageUrl, ON_EXIT_PAGE)

  private def addScreenshot(pageUrl: String, screenshotMoment: ScreenshotMoment): Either[String, File] =
    tests
      .addScreenshot(pageUrl, screenshotMoment) match {
      case Left(error) => throw new IllegalStateException(error)
      case Right(newTests) =>
        tests = newTests
        tests.screenshotSuffix().map(file => new File(root.getAbsolutePath + File.separator + file))
    }

}

object ScreenshotUtils {
  def createScreenshotOnEnter(scrFile: File, pageUrl: String, state: State): Unit = {
    state.addScreenshotOnEnterAt(pageUrl) match {
      case Left(error) => throw new IllegalStateException(error)
      case Right(destination) => FileUtils.copyFile(scrFile, destination)
    }
  }

  def createScreenshotOnExit(scrFile: File, pageUrl: String, state: State): Unit = {
    state.addScreenshotOnExitAt(pageUrl) match {
      case Left(error) => throw new IllegalStateException(error)
      case Right(destination) => FileUtils.copyFile(scrFile, destination)
    }
  }
}

trait StateEvent

object StateEvent {
  case class TestStarting(ordinal: Ordinal, testName: String, feature: String, scenario: String, timestamp: Long) extends StateEvent

  case class TestFailed(testName: String, feature: String, scenario: String, timestamp: Long) extends StateEvent

  case class TestSucceeded(testName: String, feature: String, scenario: String, timestamp: Long) extends StateEvent
}

sealed trait TestOutcome

object TestOutcome {
  case object SUCCESSFUL extends TestOutcome

  case object FAILED extends TestOutcome

  case object STARTING extends TestOutcome
}

sealed trait ScreenshotMoment

object ScreenshotMoment {
  case object ON_ENTER_PAGE extends ScreenshotMoment

  case object ON_EXIT_PAGE extends ScreenshotMoment
}