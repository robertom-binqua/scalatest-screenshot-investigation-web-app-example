package org.binqua.scalatest.reporter

import cats.implicits._

import scala.annotation.tailrec

trait TestsReportBuilder {
  def validateEvents(events: List[StateEvent]): Either[String, TestsReport]
}

class TestsReportBuilderImpl extends TestsReportBuilder {

  private val testsReport: TestsReport = TestsReport(Map.empty)

  override def validateEvents(events: List[StateEvent]): Either[String, TestsReport] =
    internalValidateEvents(events, testsReport)

  @tailrec
  private def internalValidateEvents(events: List[StateEvent], testsReport: TestsReport): Either[String, TestsReport] = {
    checkForDuplicateIds(testsReport) match {
      case Left(error) => error.asLeft
      case Right(_) =>
        events match {
          case ::(head, next) =>
            val newReport: Either[String, TestsReport] = head match {
              case StateEvent.RunStarting(_) => testsReport.asRight

              case StateEvent.TestStarting(runningScenario, timestamp) =>
                TestsReport.testStarting(testsReport, runningScenario, timestamp)

              case StateEvent.TestFailed(runningScenario, recordedEvent, throwable, timestamp) =>
                TestsReport.testFailed(testsReport, runningScenario, recordedEvent, throwable, timestamp)

              case StateEvent.TestSucceeded(runningScenario, recordedEvent, timestamp) =>
                TestsReport.testSucceeded(testsReport, runningScenario, recordedEvent, timestamp)

              case StateEvent.RunCompleted(timestamp) =>
                testsReport.asRight

              case StateEvent.Note(runningScenario, message, throwable, timestamp) =>
                TestsReport.addStep(testsReport, runningScenario, message, throwable, timestamp)

              case StateEvent.Screenshot(runningScenario, screenshotDriverData, timestamp) =>
                TestsReport.addScreenshot(testsReport, runningScenario, screenshotDriverData).map(_._1)
            }

            newReport match {
              case error @ Left(_)    => error
              case Right(validReport) => internalValidateEvents(next, validReport)
            }
          case Nil => testsReport.asRight
        }
    }

  }

  private def checkForDuplicateIds(testsReport: TestsReport): Either[String, Unit] = {

    def testIdsDuplicationCheck: Either[String, List[String]] = {
      val testIds = testsReport.tests.values.map(_.id).toList
      if (testIds.toSet.size != testIds.size) s"there are some duplication in test ids list ${testIds.mkString(",")}".asLeft else testIds.asRight
    }

    def featureIdsDuplicationCheck: Either[String, List[String]] = {
      val featureIds: List[String] = (for {
        test <- testsReport.tests.values
        feature <- test.features.featuresMap.values
      } yield feature.id).toList

      if (featureIds.toSet.size != featureIds.size) s"there are some duplication in feature ids list ${featureIds.mkString(",")}".asLeft else featureIds.asRight
    }

    def scenarioIdsDuplicationCheck: Either[String, List[String]] = {
      val scenarioIds: List[String] = (for {
        test <- testsReport.tests.values
        feature <- test.features.featuresMap.values
        scenario <- feature.scenarios.scenariosMap.values
        result <- List(scenario.id)
      } yield result).toList

      if (scenarioIds.toSet.size != scenarioIds.size) s"there are some duplication in scenario ids list ${scenarioIds.mkString(",")}".asLeft
      else scenarioIds.asRight
    }

    for {
      _ <- testIdsDuplicationCheck
      _ <- featureIdsDuplicationCheck
      _ <- scenarioIdsDuplicationCheck
    } yield ()
  }

}
