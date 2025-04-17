package org.binqua.examples.http4sapp.app

import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}
import org.binqua.examples.http4sapp.app.TestOutcome.{FAILED, SUCCEEDED}
import org.scalatest.events.Ordinal

import java.io.File

object Test {
  implicit val encoder: Encoder[Test] = (test: Test) =>
    Json.obj(
      "name" -> Json.fromString(test.name),
      "features" -> test.features.features.values.asJson
    )

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
      .map(result => {
        val (features, file) = result
        (this.copy(features = features), file)
      })

}
