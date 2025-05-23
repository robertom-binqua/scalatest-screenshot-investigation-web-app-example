package org.binqua.scalatest.reporter

import cats.implicits.catsSyntaxEitherId
import io.circe.Encoder
import io.circe.syntax.EncoderOps
import org.binqua.scalatest.reporter.StateEvent.RecordedEvents
import org.scalatest.events.Ordinal

case class TestsReport(tests: Map[String, Test])

object TestsReport {
  implicit val encoder: Encoder[TestsReport] = (testsReport: TestsReport) => testsReport.tests.values.asJson

  val empty: TestsReport = TestsReport(Map.empty)

  def addScreenshot(
                     testsToBeUpdated: TestsReport,
                     runningScenario: RunningScenario,
                     screenshotExternalData: ScreenshotExternalData
  ): Either[String, (TestsReport, Screenshot)] =
    findTestToBeUpdated(testsToBeUpdated, runningScenario, details = "I cannot add a screenshot")
      .flatMap((runningTest: Test) =>
        runningTest
          .addScreenshot(runningScenario.ordinal, runningScenario.feature, runningScenario.scenario, screenshotExternalData)
          .map((updatedTest: (Test, Screenshot)) => {
            val newTests = TestsReport(tests = testsToBeUpdated.tests.updated(runningScenario.test, updatedTest._1))
            (newTests, updatedTest._2)
          })
      )

  def addStep(
               testsToBeUpdated: TestsReport,
               runningScenario: RunningScenario,
               message: String,
               throwable: Option[Throwable],
               timestamp: Long
  ): Either[String, TestsReport] =
    findTestToBeUpdated(testsToBeUpdated, runningScenario, "I cannot add a step.")
      .flatMap((test: Test) =>
        Test
          .addStep(test, runningScenario.feature, runningScenario.scenario, runningScenario.ordinal, message, throwable, timestamp)
          .map(testsToBeUpdated.tests.updated(runningScenario.test, _))
          .map(TestsReport(_))
      )

  def testFailed(
                  testsToBeUpdated: TestsReport,
                  runningScenario: RunningScenario,
                  recordedEvent: RecordedEvents,
                  throwable: Option[Throwable],
                  timestamp: Long
  ): Either[String, TestsReport] =
    findTestToBeUpdated(testsToBeUpdated, runningScenario, details = "I cannot set the test to failed")
      .flatMap(
        _.markAsFailed(runningScenario.feature, runningScenario.scenario, recordedEvent, throwable, timestamp)
          .map(testsToBeUpdated.tests.updated(runningScenario.test, _))
          .map(TestsReport(_))
      )

  def testSucceeded(testsToBeUpdated: TestsReport, runningScenario: RunningScenario, recordedEvent: RecordedEvents, timestamp: Long): Either[String, TestsReport] =
    findTestToBeUpdated(testsToBeUpdated, runningScenario, details = "I cannot set the test to succeeded")
      .flatMap(
        _.markAsSucceeded(runningScenario.feature, runningScenario.scenario, recordedEvent, timestamp)
          .map(testsToBeUpdated.tests.updated(runningScenario.test, _))
          .map(TestsReport(_))
      )

  private def findTestToBeUpdated(tests: TestsReport, runningScenario: RunningScenario, details: String): Either[String, Test] =
    tests.tests
      .get(runningScenario.test)
      .toRight(s"I was going to update test ${runningScenario.test} but test ${runningScenario.test} does not exist.$details")

  def testStarting(testsToBeUpdated: TestsReport, runningScenario: RunningScenario, timestamp: Long): Either[String, TestsReport] =
    testsToBeUpdated.tests
      .get(runningScenario.test)
      .fold[Either[String, TestsReport]](
        ifEmpty = TestsReport(tests =
          testsToBeUpdated.tests.updated(
            runningScenario.test,
            Test(
              name = runningScenario.test,
              features = Features.starting(runningScenario.ordinal, runningScenario.feature, runningScenario.scenario, timestamp),
              ordinal = runningScenario.ordinal
            )
          )
        ).asRight
      )(testAlreadyPresent =>
        testAlreadyPresent
          .withNewFeatureOrScenario(runningScenario.ordinal, runningScenario.feature, runningScenario.scenario, timestamp)
          .map((updatedTest: Test) => TestsReport(tests = testsToBeUpdated.tests.updated(runningScenario.test, updatedTest)))
      )

  def runningTest(tests: TestsReport): Either[String, RunningScenario] = {
    val result: Iterable[(Test, Feature, Scenario, Ordinal)] = for {
      test <- tests.tests.values
      feature <- test.features.featuresMap.values
      scenario <- feature.scenarios.scenariosMap.values
    } yield (test, feature, scenario, scenario.id)

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
