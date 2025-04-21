package org.binqua.examples.http4sapp.app

import cats.implicits.catsSyntaxEitherId
import org.binqua.examples.http4sapp.app.StateEvent.RecordedEvents
import org.scalatest.events.Ordinal

import java.io.File

case class Features(featuresMap: Map[String, Feature]) {

  def testUpdated(
      featureDescription: String,
      scenarioDescription: String,
      recordedEvent: RecordedEvents,
      updatedTimestamp: Long,
      newState: TestOutcome
  ): Either[String, Features] =
    featuresMap
      .get(featureDescription)
      .toRight(s"last feature does not have featureDescription equals to $featureDescription")
      .flatMap(featureFound =>
        featureFound.scenarios
          .testUpdate(scenarioDescription, updatedTimestamp, recordedEvent, newState)
          .map((updatedScenario: Scenarios) => featureFound.copy(scenarios = updatedScenario))
          .map(updatedFeature => Features(featuresMap = featuresMap.updated(featureDescription, updatedFeature)))
      )

  def newTestStarting(ordinal: Ordinal, featureDescription: String, scenarioDescription: String, timestamp: Long): Either[String, Features] =
    featuresMap.get(featureDescription) match {
      case Some(featureFound) =>
        featureFound
          .withNewScenario(ordinal, scenarioDescription, timestamp)
          .map(updatedFeature => Features(featuresMap = featuresMap.updated(featureDescription, updatedFeature)))
      case None =>
        val newScenarios =
          Scenarios(scenariosMap =
            Map(
              scenarioDescription -> Scenario(
                ordinal = ordinal,
                description = scenarioDescription,
                startedTimestamp = timestamp,
                finishedTimestamp = None,
                screenshots = None,
                steps = None,
                testOutcome = TestOutcome.STARTING
              )
            )
          )
        val newFeature = Feature(description = featureDescription, scenarios = newScenarios)
        Features(featuresMap =  featuresMap.updated(featureDescription, newFeature)).asRight
    }

  def addScreenshot(
      ordinal: Ordinal,
      featureDescription: String,
      scenarioDescription: String,
      pageUrl: String,
      screenshotMoment: ScreenshotMoment
  ): Either[String, (Features, File)] =
    featuresMap
      .get(featureDescription)
      .toRight("there are not features. I cannot add a screenshot")
      .flatMap(feature =>
        feature
          .withNewScreenshot(ordinal, scenarioDescription, pageUrl, screenshotMoment)
          .map((result: (Feature, File)) => {
            val (updateFeature, screenshotLocation) = result
            (Features(featuresMap.updated(featureDescription, updateFeature)), screenshotLocation)
          })
      )
}

object Features {
  def starting(ordinal: Ordinal, featureDescription: String, scenarioDescription: String, timestamp: Long): Features = {
    val newScenarios =
      Scenarios(scenariosMap =
        Map(
          scenarioDescription -> Scenario(
            ordinal = ordinal,
            description = scenarioDescription,
            startedTimestamp = timestamp,
            finishedTimestamp = None,
            screenshots = None,
            steps = None,
            testOutcome = TestOutcome.STARTING
          )
        )
      )
    val newFeature = Feature(description = featureDescription, scenarios = newScenarios)
    Features(featuresMap = Map(featureDescription -> newFeature))
  }
}
