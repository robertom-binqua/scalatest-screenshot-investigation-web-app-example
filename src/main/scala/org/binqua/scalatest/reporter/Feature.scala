package org.binqua.scalatest.reporter

import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}
import org.scalatest.events.Ordinal


object Feature {
  implicit val featureEncoder: Encoder[Feature] = Encoder.instance { feature =>
    Json.obj(
      "description" -> Json.fromString(feature.description),
      "id" -> Json.fromString(feature.id),
      "scenarios" -> feature.scenarios.asJson
    )
  }
}

case class Feature(description: String, scenarios: Scenarios, ordinal: Ordinal) extends WithId {

  val id: String = Utils.ordinalToString("f", ordinal)

  def withNewScenario(ordinal: Ordinal, scenarioDescription: String, timestamp: Long): Either[String, Feature] = scenarios
    .testStarting(ordinal, scenarioDescription, timestamp)
    .map(newScenarios => this.copy(scenarios = newScenarios))

  def withNewScreenshot(ordinal: Ordinal, scenarioDescription: String, screenshotExternalData:ScreenshotDriverData): Either[String, (Feature, Screenshot)] =
    scenarios
      .withNewScreenshot(ordinal, scenarioDescription, screenshotExternalData)
      .map((newScenarios: (Scenarios, Screenshot)) => {
        val (updatedScenarios, screenshot) = newScenarios
        (this.copy(scenarios = updatedScenarios), screenshot)
      })

}
