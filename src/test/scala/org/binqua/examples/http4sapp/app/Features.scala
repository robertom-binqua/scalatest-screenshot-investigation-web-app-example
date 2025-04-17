package org.binqua.examples.http4sapp.app

import cats.implicits.catsSyntaxEitherId
import org.scalatest.events.Ordinal

import java.io.File

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
        Features(Map(featureDescription -> newF)).asRight
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
