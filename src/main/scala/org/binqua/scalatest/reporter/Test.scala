package org.binqua.scalatest.reporter

import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}
import org.binqua.scalatest.reporter.StateEvent.RecordedEvents
import org.binqua.scalatest.reporter.TestOutcome.{FAILED, SUCCEEDED}
import org.scalatest.events.Ordinal

trait WithId {
  val id: String
}

object Test {
  implicit val encoder: Encoder[Test] = (test: Test) =>
    Json.obj(
      "name" -> Json.fromString(test.name),
      "id" -> Json.fromString(Utils.ordinalToString("t", test.ordinal)),
      "features" -> test.features.featuresMap.values.asJson
    )

  def addStep(
      test: Test,
      featureDescription: String,
      scenarioDescription: String,
      ordinal: Ordinal,
      message: String,
      throwable: Option[Throwable],
      timestamp: Long
  ): Either[String, Test] = {
    test.features.featuresMap.get(featureDescription) match {
      case Some(featureFound: Feature) =>
        Scenarios
          .withNewStep(featureFound.scenarios, scenarioDescription, ordinal, message, throwable, timestamp)
          .map(updatedScenario => featureFound.copy(scenarios = updatedScenario))
          .map(updatedFeature => Features(featuresMap = test.features.featuresMap.updated(featureDescription, updatedFeature)))
          .map(updatedFeatures => test.copy(features = updatedFeatures))
      case None => Left(s"feature does not have featureDescription equals to $featureDescription")
    }
  }

}

case class Test(name: String, features: Features, ordinal: Ordinal) extends WithId {
  val id: String = Utils.ordinalToString("t", ordinal)
  def withNewFeatureOrScenario(ordinal: Ordinal, featureDescription: String, scenarioDescription: String, timestamp: Long): Either[String, Test] =
    Features
      .newTestStarting(features, ordinal, featureDescription, scenarioDescription, timestamp)
      .map(newFeatures => this.copy(features = newFeatures))

  def markAsFailed(
      featureDescription: String,
      scenarioDescription: String,
      recordedEvent: RecordedEvents,
      throwable: Option[Throwable],
      failedTimestamp: Long
  ): Either[String, Test] =
    Features
      .testUpdated(features, featureDescription, scenarioDescription, recordedEvent, failedTimestamp, throwable, FAILED)
      .map(newFeatures => this.copy(features = newFeatures))

  def markAsSucceeded(featureDescription: String, scenarioDescription: String, recordedEvent: RecordedEvents, timestamp: Long): Either[String, Test] =
    Features
      .testUpdated(features, featureDescription, scenarioDescription, recordedEvent, timestamp, None, SUCCEEDED)
      .map(newFeatures => this.copy(features = newFeatures))

  def addScreenshot(
      ordinal: Ordinal,
      featureDescription: String,
      scenarioDescription: String,
      screenshotExternalData: ScreenshotDriverData
  ): Either[String, (Test, Screenshot)] =
    Features
      .addScreenshot(features, ordinal, featureDescription, scenarioDescription, screenshotExternalData)
      .map((result: (Features, Screenshot)) => {
        val (features, screenshot) = result
        (this.copy(features = features), screenshot)
      })

}
