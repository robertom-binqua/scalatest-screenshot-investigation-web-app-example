package org.binqua.scalatest.reporter

import io.circe.syntax.EncoderOps
import io.circe.{Json, JsonObject}
import org.apache.commons.io.FileUtils

import java.io.File
import java.nio.charset.StandardCharsets

class ReportFileUtilsImpl(config: TestsCollectorConfiguration) extends ReportFileUtils:
  override def copyFile(from: File, toSuffix: File): Unit =
    FileUtils.copyFile(
      from,
      new File(config.screenshotsRootLocation.getAbsolutePath + File.separator + toSuffix)
    )

  override def writeStringToFile(stringToBeWritten: String, toSuffix: File): Unit =
    FileUtils.writeStringToFile(
      new File(config.screenshotsRootLocation.getAbsolutePath + File.separator + toSuffix),
      stringToBeWritten,
      StandardCharsets.UTF_8
    )

  override def writeReport(tests: Tests): Unit =
    val json: JsonObject = JsonObject(
      "screenshotsLocationPrefix" -> Json.fromString(config.screenshotsLocationPrefix),
      "testsReport" -> tests.asJson
    )
    FileUtils.writeStringToFile(
      config.jsonReportLocation,
      s"window.testsReport = ${json.toJson.spaces2}",
      StandardCharsets.UTF_8
    )

trait ReportFileUtils:

  def copyFile(from: File, toSuffix: File): Unit

  def writeStringToFile(stringToBeWritten: String, toSuffix: File): Unit

  def writeReport(tests: Tests): Unit
