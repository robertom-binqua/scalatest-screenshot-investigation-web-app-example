package org.binqua.scalatest.reporter

import cats.implicits.catsSyntaxEitherId
import org.binqua.scalatest.reporter.StateEvent.RecordedEvents
import org.scalatest.events.Ordinal

case class Features(featuresMap: Map[String, Feature])

object Features {
  def starting(ordinal: Ordinal, featureDescription: String, scenarioDescription: String, timestamp: Long): Features = {
    val newScenarios =
      Scenarios(scenariosMap = Map(scenarioDescription -> Scenario.starting(ordinal, scenarioDescription, timestamp)))
    val newFeature = Feature(description = featureDescription, scenarios = newScenarios, ordinal)
    Features(featuresMap = Map(featureDescription -> newFeature))
  }

  def testUpdated(
      features: Features,
      featureDescription: String,
      scenarioDescription: String,
      recordedEvent: RecordedEvents,
      updatedTimestamp: Long,
      throwable: Option[Throwable],
      newState: TestOutcome
  ): Either[String, Features] =
    features.featuresMap
      .get(featureDescription)
      .toRight(s"last feature does not have featureDescription equals to $featureDescription")
      .flatMap(featureFound =>
        featureFound.scenarios
          .testUpdate(scenarioDescription, updatedTimestamp, recordedEvent, throwable, newState)
          .map((updatedScenario: Scenarios) => featureFound.copy(scenarios = updatedScenario))
          .map(updatedFeature => Features(featuresMap = features.featuresMap.updated(featureDescription, updatedFeature)))
      )

  def newTestStarting(
      features: Features,
      ordinal: Ordinal,
      featureDescription: String,
      scenarioDescription: String,
      timestamp: Long
  ): Either[String, Features] =
    features.featuresMap.get(featureDescription) match {
      case Some(featureFound) =>
        featureFound
          .withNewScenario(ordinal, scenarioDescription, timestamp)
          .map(updatedFeature => Features(featuresMap = features.featuresMap.updated(featureDescription, updatedFeature)))
      case None =>
        val newScenarios = Scenarios(scenariosMap = Map(scenarioDescription -> Scenario.starting(ordinal, scenarioDescription, timestamp)))
        val newFeature = Feature(description = featureDescription, scenarios = newScenarios, ordinal)
        Features(featuresMap = features.featuresMap.updated(featureDescription, newFeature)).asRight
    }

  def addScreenshot(
      features: Features,
      ordinal: Ordinal,
      featureDescription: String,
      scenarioDescription: String,
      screenshotExternalData: ScreenshotDriverData
  ): Either[String, (Features, Screenshot)] =
    features.featuresMap
      .get(featureDescription)
      .toRight("there are not features. I cannot add a screenshot")
      .flatMap(feature =>
        feature
          .withNewScreenshot(ordinal, scenarioDescription, screenshotExternalData)
          .map((result: (Feature, Screenshot)) => {
            val (updateFeature, screenshotLocation) = result
            (Features(features.featuresMap.updated(featureDescription, updateFeature)), screenshotLocation)
          })
      )
}
