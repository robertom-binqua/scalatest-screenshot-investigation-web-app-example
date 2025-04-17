package org.binqua.examples.http4sapp.app

import io.circe.generic.semiauto.deriveEncoder
import io.circe.{Encoder, Json}
import org.scalatest.events.Ordinal

import java.io.File

object Scenario {
  implicit val ordinalEncoder: Encoder[Ordinal] = (ordinal: Ordinal) => Json.fromString(ordinal.toList.mkString("_"))
  implicit val encoder: Encoder[Scenario] = deriveEncoder[Scenario]

  def starting(ordinal: Ordinal, name: String, timestamp: Long): Scenario =
    Scenario(
      ordinal = ordinal,
      description = name,
      startedTimestamp = timestamp,
      finishedTimestamp = None,
      screenshots = None,
      testOutcome = TestOutcome.STARTING
    )

}

case class Scenario(
    ordinal: Ordinal,
    description: String,
    startedTimestamp: Long,
    finishedTimestamp: Option[Long],
    screenshots: Option[List[Screenshot]],
    testOutcome: TestOutcome
) {

  def withNewScreenshot(pageUrl: String, screenshotMoment: ScreenshotMoment): (Scenario, File) = {
    val maybeScreenshots: Option[List[Screenshot]] = screenshots
      .map(s => Screenshot(pageUrl, screenshotMoment, ordinal, s.size + 1) :: s)
      .orElse(Some(List(Screenshot(pageUrl, screenshotMoment, ordinal, 1))))
    (this.copy(screenshots = maybeScreenshots), maybeScreenshots.get.head.toFile)
  }
}
