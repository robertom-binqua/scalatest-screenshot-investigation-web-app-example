package org.binqua.scalatest.reporter

import cats.effect.IO.catsSyntaxTuple2Parallel
import cats.implicits.{catsSyntaxEitherId, toBifunctorOps}
import org.binqua.scalatest.reporter.Utils.EitherOps

import java.io.File

object TestsCollectorConfiguration:

  /*
   * Playing a little bit with cats ... https://typelevel.org/cats/typeclasses/parallel.html
   */

  private val reportDirName: String = "report"
  private val screenshotsDirName: String = "screenshots"

  def from(reportDirParent: File): Either[String, TestsCollectorConfiguration] = {

    val reportDir = new File(reportDirParent.getAbsolutePath + File.separator + reportDirName)
    reportDir.mkdir()

    val screenshotsDir = new File(reportDir.getAbsolutePath + File.separator + screenshotsDirName)
    screenshotsDir.mkdir()

    (validatedReportDir(reportDir), validateScreenshotsDir(screenshotsDir))
      .parMapN((reportRoot, screenshotsRoot) => {
        new TestsCollectorConfiguration {
          override def reportRootLocation: File = reportRoot

          override def jsonReportLocation: File = new File(reportRoot.getAbsolutePath + File.separator + "testsReport.js")

          override def screenshotsRootLocation: File = screenshotsRoot

          override def screenshotsLocationPrefix: String = s"$reportDirName/$screenshotsDirName/"
        }
      })
      .leftMap(_.mkString(" - "))
  }

  private def validatedReportDir(reportDir: File): Either[List[String], File] =
    if (!reportDir.exists()) List(s"ReportDir $reportDir should exist but it does not").asLeft
    else reportDir.asRight

  private def validateScreenshotsDir(screenshotsDir: File): Either[List[String], File] = {
    if (!screenshotsDir.exists())
      List(s"ScreenshotsDir $screenshotsDir should exist but it does not").asLeft
    else screenshotsDir.asRight
  }

  def unsafeFrom(reportRootParent: File): TestsCollectorConfiguration = from(reportRootParent).getOrThrow

sealed trait TestsCollectorConfiguration:
  def reportRootLocation: File
  def jsonReportLocation: File
  def screenshotsRootLocation: File
  def screenshotsLocationPrefix: String
