package org.binqua.scalatest.reporter

import java.io.File

trait ReportFileUtils {

  def saveImage(data: Array[Byte], toSuffix: File): Unit

  def copyFile(from: File, toSuffix: File): Unit

  def writeStringToFile(stringToBeWritten: String, toSuffix: File): Unit

  def withNoHtmlElementsToFile(originalSourceToBeWritten: String, toSuffix: File): Unit

  def writeReport(tests: TestsReport): Unit

}
