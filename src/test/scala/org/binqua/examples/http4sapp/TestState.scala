package org.binqua.examples.http4sapp

import org.apache.commons.io.FileUtils
import org.binqua.examples.http4sapp.ScreenshotMoment.{ON_ENTER_PAGE, ON_EXIT_PAGE}
import org.binqua.examples.http4sapp.TestOutcome._
import org.scalatest.events.Ordinal

import java.io.File

trait State {

  def add(event: StateEvent): Unit

  def createReport(): Unit

  def addScreenshotOnEnterAt(pageUrl: String, runningScenario: RunningScenario): Either[String, File]

  def addScreenshotOnExitAt(pageUrl: String, runningScenario: RunningScenario): Either[String, File]

}

case class Screenshot(pageUrl: String, screenshotMoment: ScreenshotMoment, ordinal: Ordinal, index: Int) {
  def toFile: File = new File(ordinal.toList.mkString("_") + File.separator + s"screenshot_${index}_$screenshotMoment.png")
}

object Scenario {
  def starting(ordinal: Ordinal, scenarioDescription: String, timestamp: Long): Scenario =
    Scenario(ordinal, scenarioDescription, timestamp, None, None, TestOutcome.STARTING)
}

case class Scenario(
    ordinal: Ordinal,
    description: String,
    startedTimestamp: Long,
    finishedTimestamp: Option[Long],
    screenshots: Option[List[Screenshot]],
    testOutcome: TestOutcome
) {

  def withNewScreenshot(pageUrl: String, screenshotMoment: ScreenshotMoment): (Scenario, File) = {
    val maybeScreenshots: Option[List[Screenshot]] = screenshots
      .map(s => Screenshot(pageUrl, screenshotMoment, ordinal, s.size + 1) :: s)
      .orElse(Some(List(Screenshot(pageUrl, screenshotMoment, ordinal, 1))))
    (this.copy(screenshots = maybeScreenshots), maybeScreenshots.get.head.toFile)
  }
}

case class Scenarios(scenarios: Map[String, Scenario]) {
  def testUpdate(scenarioDescription: String, timestamp: Long, newState: TestOutcome): Either[String, Scenarios] =
    scenarios.get(scenarioDescription) match {
      case Some(scenarioFound) =>
        if (scenarioFound.testOutcome != TestOutcome.STARTING)
          Left(s"lastScenario testOutcome is ${scenarioFound.testOutcome} and should be ${TestOutcome.STARTING}")
        else
          Right(this.copy(scenarios = scenarios.updated(scenarioDescription, scenarioFound.copy(finishedTimestamp = Some(timestamp), testOutcome = newState))))
      case None =>
        Left(s"no scenario with description $scenarioDescription")
    }

  def testStarting(ordinal: Ordinal, scenarioDescription: String, timestamp: Long): Either[String, Scenarios] =
    scenarios.get(scenarioDescription) match {
      case Some(scenarioAlreadyPresent) =>
        Left(s"scenarioAlreadyPresent bro $scenarioAlreadyPresent, I cannot start it")
      case None =>
        Right(this.copy(scenarios = this.scenarios.updated(scenarioDescription, Scenario.starting(ordinal, scenarioDescription, timestamp))))
    }

  def withNewScreenshot(ordinal: Ordinal, scenarioDescription: String, pageUrl: String, screenshotMoment: ScreenshotMoment): Either[String, (Scenarios, File)] =
    scenarios
      .get(scenarioDescription)
      .toRight("last scenario does not have testOutcome == STARTING")
      .flatMap(lastScenario =>
        if (lastScenario.testOutcome == TestOutcome.STARTING && lastScenario.ordinal == ordinal) {
          val (updatedScenario, screenshotFile) = lastScenario.withNewScreenshot(pageUrl, screenshotMoment)
          val scenarios = this.copy(scenarios = this.scenarios.updated(scenarioDescription, updatedScenario))
          Right(scenarios, screenshotFile)
        } else
          Left(s"Sorry last scenario does not have testOutcome equal to STARTING but ${lastScenario.testOutcome}")
      )

}

case class Feature(description: String, scenarios: Scenarios) {
  def withNewScenario(ordinal: Ordinal, scenarioDescription: String, timestamp: Long): Either[String, Feature] = scenarios
    .testStarting(ordinal, scenarioDescription, timestamp)
    .map(newScenarios => this.copy(scenarios = newScenarios))

  def withNewScreenshot(ordinal: Ordinal, scenarioDescription: String, pageUrl: String, screenshotMoment: ScreenshotMoment): Either[String, (Feature, File)] =
    scenarios
      .withNewScreenshot(ordinal, scenarioDescription, pageUrl, screenshotMoment)
      .map((newScenarios: (Scenarios, File)) => {
        val (updatedScenarios, screenshot) = newScenarios
        (this.copy(scenarios = updatedScenarios), screenshot)
      })

}

case class Features(features: Map[String, Feature]) {

  def testUpdated(featureDescription: String, scenarioDescription: String, updatedTimestamp: Long, newState: TestOutcome): Either[String, Features] =
    features.get(featureDescription) match {
      case Some(featureFound) =>
        featureFound.scenarios
          .testUpdate(scenarioDescription, updatedTimestamp, newState)
          .map((updatedScenario: Scenarios) => featureFound.copy(scenarios = updatedScenario))
          .map(updatedFeature => Features(features = features.updated(featureDescription, updatedFeature)))
      case None => Left(s"last feature does not have featureDescription equals to $featureDescription")
    }

  def newTestStarting(ordinal: Ordinal, featureDescription: String, scenarioDescription: String, timestamp: Long): Either[String, Features] =
    features.get(featureDescription) match {
      case Some(featureFound) =>
        featureFound
          .withNewScenario(ordinal, scenarioDescription, timestamp)
          .map(updatedFeature => Features(features = features.updated(featureDescription, updatedFeature)))
      case None =>
        val newF = Feature(
          featureDescription,
          Scenarios(Map(scenarioDescription -> Scenario(ordinal, scenarioDescription, timestamp, None, None, TestOutcome.STARTING)))
        )
        Right(Features(Map(featureDescription -> newF)))
    }

  def addScreenshot(
      ordinal: Ordinal,
      featureDescription: String,
      scenarioDescription: String,
      pageUrl: String,
      screenshotMoment: ScreenshotMoment
  ): Either[String, (Features, File)] =
    features
      .get(featureDescription)
      .toRight("there are not features. I cannot add a screenshot")
      .flatMap(feature =>
        feature
          .withNewScreenshot(ordinal, scenarioDescription, pageUrl, screenshotMoment)
          .map((result: (Feature, File)) => {
            val (updateFeature, screenshotLocation) = result
            (Features(features.updated(featureDescription, updateFeature)), screenshotLocation)
          })
      )
}

object Features {
  def starting(ordinal: Ordinal, featureDescription: String, scenarioDescription: String, timestamp: Long): Features = {
    val newScenarios = Scenarios(scenarios = Map(scenarioDescription -> Scenario(ordinal, scenarioDescription, timestamp, None, None, TestOutcome.STARTING)))
    val newFeature = Feature(description = featureDescription, scenarios = newScenarios)
    Features(features = Map(featureDescription -> newFeature))
  }
}

object Tests {
  val empty: Tests = Tests(Map.empty)
}
case class Tests(tests: Map[String, Test]) {

  def runningTest: Option[RunningScenario] = {
    val result: Iterable[(Test, Feature, Scenario, Ordinal)] = for {
      test <- tests.values
      feature <- test.features.features.values
      scenario <- feature.scenarios.scenarios.values
    } yield (test, feature, scenario, scenario.ordinal)

    result.toList
      .sortWith((l, r) => l._4 > r._4)
      .headOption
      .map(result => {
        val (test, feature, scenario, ordinal) = result
        RunningScenario(ordinal, test.name, feature.description, scenario.description)
      })
  }

  def addScreenshot(runningScenario: RunningScenario, pageUrl: String, screenshotMoment: ScreenshotMoment): Either[String, (Tests, File)] =
    tests.get(runningScenario.test) match {
      case Some(testFound) =>
        testFound
          .addScreenshot(runningScenario.ordinal, runningScenario.feature, runningScenario.scenario, pageUrl, screenshotMoment)
          .map((updatedTest: (Test, File)) => {
            val newTests = Tests(tests = tests.updated(runningScenario.test, updatedTest._1))
            (newTests, updatedTest._2)
          })
      case None => Left("I cannot add screenshots because there are no tests")
    }

  def testStarting(runningScenario: RunningScenario, timestamp: Long): Either[String, Tests] =
    tests.get(runningScenario.test) match {
      case Some(testAlreadyPresent) =>
        testAlreadyPresent
          .withNewFeatureOrScenario(runningScenario.ordinal, runningScenario.feature, runningScenario.scenario, timestamp)
          .map((updatedTest: Test) => Tests(tests = tests.updated(runningScenario.test, updatedTest)))
      case None =>
        Right(
          Tests(tests =
            tests.updated(
              runningScenario.test,
              Test(runningScenario.test, Features.starting(runningScenario.ordinal, runningScenario.feature, runningScenario.scenario, timestamp))
            )
          )
        )
    }

  def testFailed(runningScenario: RunningScenario, timestamp: Long): Either[String, Tests] =
    tests.get(runningScenario.test) match {
      case Some(testAlreadyPresent) =>
        testAlreadyPresent
          .markAsFailed(runningScenario.feature, runningScenario.scenario, timestamp)
          .map((updatedTest: Test) => Tests(tests = tests.updated(runningScenario.test, updatedTest)))
      case None => Left(s"I was going to update test ${runningScenario.test} to failed but test ${runningScenario.test} does not exist")
    }

  def testSucceeded(runningScenario: RunningScenario, timestamp: Long): Either[String, Tests] =
    tests.get(runningScenario.test) match {
      case Some(testAlreadyPresent) =>
        testAlreadyPresent
          .markAsSucceeded(runningScenario.feature, runningScenario.scenario, timestamp)
          .map((updatedTest: Test) => Tests(tests = tests.updated(runningScenario.test, updatedTest)))
      case None => Left(s"I was going to update test ${runningScenario.test} to succeeded but test ${runningScenario.test} does not exist")
    }

}

case class Test(name: String, features: Features) {

  def withNewFeatureOrScenario(ordinal: Ordinal, featureDescription: String, scenarioDescription: String, timestamp: Long): Either[String, Test] =
    features.newTestStarting(ordinal, featureDescription, scenarioDescription, timestamp).map(newFeatures => this.copy(features = newFeatures))

  def markAsFailed(featureDescription: String, scenarioDescription: String, failedTimestamp: Long): Either[String, Test] =
    features.testUpdated(featureDescription, scenarioDescription, failedTimestamp, FAILED).map(newFeatures => this.copy(features = newFeatures))

  def markAsSucceeded(featureDescription: String, scenarioDescription: String, timestamp: Long): Either[String, Test] =
    features.testUpdated(featureDescription, scenarioDescription, timestamp, SUCCEEDED).map(newFeatures => this.copy(features = newFeatures))

  def addScreenshot(
      ordinal: Ordinal,
      featureDescription: String,
      scenarioDescription: String,
      pageUrl: String,
      screenshotMoment: ScreenshotMoment
  ): Either[String, (Test, File)] =
    features
      .addScreenshot(ordinal, featureDescription, scenarioDescription, pageUrl, screenshotMoment)
      .map((result: (Features, File)) => (this.copy(features = result._1), result._2))

}

object TheState extends State {

  val root = new File(s"screenshots/")

  var tests: Tests = Tests(Map.empty)

  var screenshots: List[Screenshot] = List.empty

  def add(event: StateEvent): Unit = {
    event match {
      case StateEvent.TestStarting(runningScenario, timestamp) =>
        tests.testStarting(runningScenario, timestamp) match {
          case Left(value) => throw new IllegalStateException(value)
          case Right(newTests) =>
            tests = newTests
        }

      case StateEvent.TestFailed(runningScenario, timestamp) =>
        tests.testFailed(runningScenario, timestamp) match {
          case Left(value) => throw new IllegalStateException(value)
          case Right(newTests) =>
            tests = newTests
        }

      case StateEvent.TestSucceeded(runningScenario, timestamp) =>
        tests.testSucceeded(runningScenario, timestamp) match {
          case Left(error) => throw new IllegalStateException(error)
          case Right(newTests) =>
            tests = newTests
        }

      case _ =>
    }
  }

  def createReport(): Unit = ???

  override def addScreenshotOnEnterAt(pageUrl: String, runningScenario: RunningScenario): Either[String, File] =
    addScreenshot(runningScenario, pageUrl, ON_ENTER_PAGE)

  override def addScreenshotOnExitAt(pageUrl: String, runningScenario: RunningScenario): Either[String, File] =
    addScreenshot(runningScenario, pageUrl, ON_EXIT_PAGE)

  private def addScreenshot(testRunningInfo: RunningScenario, pageUrl: String, screenshotMoment: ScreenshotMoment): Either[String, File] =
    tests
      .addScreenshot(testRunningInfo, pageUrl, screenshotMoment) match {
      case Left(error) => throw new IllegalStateException(error)
      case Right(newTests) =>
        tests = newTests._1
        Right(new File(root.getAbsolutePath + File.separator + newTests._2))
    }

}

object ScreenshotUtils {
  def createScreenshotOnEnter(scrFile: File, pageUrl: String, state: State, testRunningInfo: RunningScenario): Unit = {
    state.addScreenshotOnEnterAt(pageUrl, testRunningInfo) match {
      case Left(error)        => throw new IllegalStateException(error)
      case Right(destination) => FileUtils.copyFile(scrFile, destination)
    }
  }

  def createScreenshotOnExit(scrFile: File, pageUrl: String, state: State, testRunningInfo: RunningScenario): Unit = {
    state.addScreenshotOnExitAt(pageUrl, testRunningInfo) match {
      case Left(error)        => throw new IllegalStateException(error)
      case Right(destination) => FileUtils.copyFile(scrFile, destination)
    }
  }
}

trait StateEvent

object StateEvent {
  case class TestStarting(runningScenario: RunningScenario, timestamp: Long) extends StateEvent

  case class TestFailed(runningScenario: RunningScenario, timestamp: Long) extends StateEvent

  case class TestSucceeded(runningScenario: RunningScenario, timestamp: Long) extends StateEvent
}

sealed trait TestOutcome

object TestOutcome {
  case object SUCCEEDED extends TestOutcome

  case object FAILED extends TestOutcome

  case object STARTING extends TestOutcome
}

sealed trait ScreenshotMoment

object ScreenshotMoment {
  case object ON_ENTER_PAGE extends ScreenshotMoment

  case object ON_EXIT_PAGE extends ScreenshotMoment
}
