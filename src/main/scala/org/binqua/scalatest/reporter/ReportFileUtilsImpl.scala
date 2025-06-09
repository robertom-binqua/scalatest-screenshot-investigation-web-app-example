package org.binqua.scalatest.reporter

import io.circe.syntax.EncoderOps
import io.circe.{Json, JsonObject}
import org.apache.commons.io.FileUtils
import org.binqua.scalatest.reporter.util.utils
import org.jsoup.Jsoup

import java.io.File
import java.nio.charset.StandardCharsets

class ReportFileUtilsImpl(config: TestsCollectorConfiguration) extends ReportFileUtils {
  override def copyFile(from: File, toSuffix: File): Unit =
    FileUtils.copyFile(
      from,
      new File(config.screenshotsRootLocation + File.separator + toSuffix)
    )

  override def saveImage(data: Array[Byte], toSuffix: File): Unit =
    FileUtils.writeByteArrayToFile(new File(config.screenshotsRootLocation + File.separator + toSuffix), data)

  override def writeStringToFile(stringToBeWritten: String, toSuffix: File): Unit =
    FileUtils.writeStringToFile(
      new File(config.screenshotsRootLocation + File.separator + toSuffix),
      stringToBeWritten,
      StandardCharsets.UTF_8
    )

  override def writeReport(tests: TestsReport): Unit = {
    val json: JsonObject = JsonObject(
      "screenshotsLocationPrefix" -> Json.fromString(config.screenshotsLocationPrefix),
      "testsReport" -> tests.asJson
    )
    FileUtils.writeStringToFile(
      config.jsonReportLocation,
      json.toJson.spaces2,
      StandardCharsets.UTF_8
    )
  }

  override def withNoHtmlElementsToFile(originalSourceToBeWritten: String, toSuffix: File): Unit =
    FileUtils.writeStringToFile(
      new File(config.screenshotsRootLocation + File.separator + toSuffix),
      utils.clean(Jsoup.parse(originalSourceToBeWritten).wholeText()),
      StandardCharsets.UTF_8
    )
}
