package org.binqua.scalatest.reporter

import io.circe.{Encoder, Json}
import org.scalatest.events.Ordinal

import java.io.File

object Screenshot {
  implicit val encoder: Encoder[Screenshot] = (screenshot: Screenshot) =>
    Json.obj(
      "originalLocation" -> Json.fromString(screenshot.originalFileLocation.toString),
      "resizedLocation" -> Json.fromString(screenshot.resizeFileLocation.toString),
      "sourceLocation" -> Json.fromString(screenshot.sourceCode.toString),
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

  def sourceCode: File = new File(
    root + File.separator + "sources" + File.separator + s"${index}_$screenshotMoment.txt"
  )

}
