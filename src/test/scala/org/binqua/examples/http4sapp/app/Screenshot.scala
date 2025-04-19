package org.binqua.examples.http4sapp.app

import io.circe.{Encoder, Json}
import org.scalatest.events.Ordinal

import java.io.File

object Screenshot {
  implicit val encoder: Encoder[Screenshot] = (screenshot: Screenshot) =>
    Json.obj(
      "location" -> Json.fromString(screenshot.toFile.toString),
      "pageUrl" -> Json.fromString(screenshot.pageUrl),
      "index" -> Json.fromInt(screenshot.index),
      "screenshotMoment" -> Json.fromString(screenshot.screenshotMoment.toString)
    )
}

case class Screenshot(pageUrl: String, screenshotMoment: ScreenshotMoment, ordinal: Ordinal, index: Int) {
  def toFile: File = new File(s"scenario_ordinal_${ordinal.toList.mkString("_")}" + File.separator + s"screenshot_${index}_$screenshotMoment.png")
}
