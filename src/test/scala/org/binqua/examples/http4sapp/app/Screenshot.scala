package org.binqua.examples.http4sapp.app

import io.circe.{Encoder, Json}
import org.scalatest.events.Ordinal

import java.io.File

object Screenshot {
  implicit val encoder: Encoder[Screenshot] = (screenshot: Screenshot) =>
    Json.obj(
      "original-location" -> Json.fromString(screenshot.originalFileLocation.toString),
      "resized-location" -> Json.fromString(screenshot.resizeFileLocation.toString),
      "source-location" -> Json.fromString(screenshot.source.toString),
      "pageUrl" -> Json.fromString(screenshot.pageUrl),
      "index" -> Json.fromInt(screenshot.index),
      "screenshotMoment" -> Json.fromString(screenshot.screenshotMoment.toString)
    )
}

case class Screenshot(pageUrl: String, screenshotMoment: ScreenshotMoment, ordinal: Ordinal, index: Int) {
  private val root = s"scenario_ordinal_${ordinal.toList.mkString("_")}"

  def originalFileLocation: File = new File(
    root + File.separator + "original" + File.separator + s"${index}_$screenshotMoment.png"
  )

  def resizeFileLocation: File = new File(
    root + File.separator + "resized" + File.separator + s"${index}_$screenshotMoment.png"
  )

  def source: File = new File(
    root + File.separator + "sources" + File.separator + s"${index}_$screenshotMoment.txt"
  )

}
