package org.binqua.examples.http4sapp.app

import cats.implicits.catsSyntaxEitherId
import io.circe.Encoder
import io.circe.syntax.EncoderOps
import org.binqua.examples.http4sapp.app.StateEvent.RecordedEvents
import org.scalatest.events.Ordinal

import java.io.File

case class Tests(tests: Map[String, Test])

object Tests {
  implicit val encoder: Encoder[Tests] = (tests: Tests) => tests.tests.values.asJson

  val empty: Tests = Tests(Map.empty)

  def addScreenshot(
      testsToBeUpdated: Tests,
      runningScenario: RunningScenario,
      pageUrl: String,
      screenshotMoment: ScreenshotMoment
  ): Either[String, (Tests, File)] =
    testsToBeUpdated.tests
      .get(runningScenario.test)
      .toRight("I cannot add screenshots because there are no tests")
      .flatMap((runningTest: Test) =>
        runningTest
          .addScreenshot(runningScenario.ordinal, runningScenario.feature, runningScenario.scenario, pageUrl, screenshotMoment)
          .map((updatedTest: (Test, File)) => {
            val newTests = Tests(tests = testsToBeUpdated.tests.updated(runningScenario.test, updatedTest._1))
            (newTests, updatedTest._2)
          })
      )

  def addStep(
      testsToBeUpdated: Tests,
      runningScenario: RunningScenario,
      message: String,
      throwable: Option[Throwable],
      timestamp: Long
  ): Either[String, Tests] =
    testsToBeUpdated.tests
      .get(runningScenario.test)
      .toRight(s"I was going to update test ${runningScenario.test} to succeeded but test ${runningScenario.test} does not exist")
      .flatMap((test: Test) =>
        Test
          .addStep(test, runningScenario.feature, runningScenario.scenario, runningScenario.ordinal, message, throwable, timestamp)
          .map(testsToBeUpdated.tests.updated(runningScenario.test, _))
          .map(Tests(_))
      )

  def testFailed(
      testsToBeUpdated: Tests,
      runningScenario: RunningScenario,
      recordedEvent: RecordedEvents,
      throwable: Option[Throwable],
      timestamp: Long
  ): Either[String, Tests] =
    testsToBeUpdated.tests
      .get(runningScenario.test)
      .toRight(s"I was going to update test ${runningScenario.test} to failed but test ${runningScenario.test} does not exist")
      .flatMap(
        _.markAsFailed(runningScenario.feature, runningScenario.scenario, recordedEvent, throwable, timestamp)
          .map(testsToBeUpdated.tests.updated(runningScenario.test, _))
          .map(Tests(_))
      )

  def testSucceeded(testsToBeUpdated: Tests, runningScenario: RunningScenario, recordedEvent: RecordedEvents, timestamp: Long): Either[String, Tests] =
    testsToBeUpdated.tests
      .get(runningScenario.test)
      .toRight(s"I was going to update test ${runningScenario.test} to succeeded but test ${runningScenario.test} does not exist")
      .flatMap(
        _.markAsSucceeded(runningScenario.feature, runningScenario.scenario, recordedEvent, timestamp)
          .map(testsToBeUpdated.tests.updated(runningScenario.test, _))
          .map(Tests(_))
      )


  def testStarting(testsToBeUpdated: Tests,runningScenario: RunningScenario, timestamp: Long): Either[String, Tests] =
    testsToBeUpdated
      .tests
      .get(runningScenario.test)
      .fold[Either[String, Tests]](
        ifEmpty = Tests(tests =
          testsToBeUpdated.tests.updated(
            runningScenario.test,
            Test(runningScenario.test, Features.starting(runningScenario.ordinal, runningScenario.feature, runningScenario.scenario, timestamp))
          )
        ).asRight
      )(testAlreadyPresent =>
        testAlreadyPresent
          .withNewFeatureOrScenario(runningScenario.ordinal, runningScenario.feature, runningScenario.scenario, timestamp)
          .map((updatedTest: Test) => Tests(tests = testsToBeUpdated.tests.updated(runningScenario.test, updatedTest)))
      )

  def runningTest(tests: Tests): Either[String, RunningScenario] = {
    val result: Iterable[(Test, Feature, Scenario, Ordinal)] = for {
      test <- tests.tests.values
      feature <- test.features.featuresMap.values
      scenario <- feature.scenarios.scenariosMap.values
    } yield (test, feature, scenario, scenario.ordinal)

    result.toList
      .sortWith((l, r) => l._4 > r._4)
      .headOption
      .toRight("There are not tests here dude")
      .map(result => {
        val (test, feature, scenario, ordinal) = result
        RunningScenario(ordinal, test.name, feature.description, scenario.description)
      })
  }

}
