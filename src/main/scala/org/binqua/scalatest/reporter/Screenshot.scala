package org.binqua.scalatest.reporter

import io.circe.{Encoder, Json}
import org.scalatest.events.Ordinal

import java.io.File

object Screenshot {
  implicit val encoder: Encoder[Screenshot] = (screenshot: Screenshot) =>
    Json.obj(
      "originalLocation" -> Json.fromString(screenshot.originalFileLocation.toString),
      "sourceLocation" -> Json.fromString(screenshot.sourceCode.toString),
      "pageUrl" -> Json.fromString(screenshot.screenshotExternalData.pageUrl),
      "index" -> Json.fromInt(screenshot.index),
      "pageTitle" -> Json.fromString(screenshot.screenshotExternalData.pageTitle),
      "screenshotMoment" -> Json.fromString(screenshot.screenshotExternalData.screenshotMoment.toString)
    )
}

case class Screenshot(screenshotExternalData: ScreenshotExternalData, ordinal: Ordinal, index: Int) {
  private val root = s"scenario_ordinal_${ordinal.toList.mkString("_")}"

  def originalFileLocation: File = new File(
    root + File.separator + "original" + File.separator + s"${index}_${screenshotExternalData.screenshotMoment}.png"
  )

  def sourceCode: File = new File(
    root + File.separator + "sources" + File.separator + s"${index}_${screenshotExternalData.screenshotMoment}.txt"
  )

}
