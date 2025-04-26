package org.binqua.examples.http4sapp.app

import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}
import org.scalatest.events.Ordinal

import java.io.File


object Feature {
  implicit val featureEncoder: Encoder[Feature] = Encoder.instance { feature =>
    Json.obj(
      "description" -> Json.fromString(feature.description),
      "id" -> Json.fromString(Utils.ordinalToString("f", feature.ordinal)),
      "scenarios" -> feature.scenarios.asJson
    )
  }
}

case class Feature(description: String, scenarios: Scenarios, ordinal: Ordinal) {
  def withNewScenario(ordinal: Ordinal, scenarioDescription: String, timestamp: Long): Either[String, Feature] = scenarios
    .testStarting(ordinal, scenarioDescription, timestamp)
    .map(newScenarios => this.copy(scenarios = newScenarios))

  def withNewScreenshot(ordinal: Ordinal, scenarioDescription: String, pageUrl: String, screenshotMoment: ScreenshotMoment): Either[String, (Feature, Screenshot)] =
    scenarios
      .withNewScreenshot(ordinal, scenarioDescription, pageUrl, screenshotMoment)
      .map((newScenarios: (Scenarios, Screenshot)) => {
        val (updatedScenarios, screenshot) = newScenarios
        (this.copy(scenarios = updatedScenarios), screenshot)
      })

}
