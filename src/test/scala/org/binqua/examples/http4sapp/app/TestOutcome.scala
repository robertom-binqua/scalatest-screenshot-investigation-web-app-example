package org.binqua.examples.http4sapp.app

import io.circe.{Encoder, Json}
import org.apache.commons.io.FileUtils
import org.binqua.examples.http4sapp.app.ScreenshotMoment._
import org.binqua.examples.http4sapp.util.utils.EitherOps

import java.io.File

sealed trait TestOutcome
object TestOutcome {

  implicit val encoder: Encoder[TestOutcome] = (a: TestOutcome) => Json.fromString(a.toString.toLowerCase)

  case object SUCCEEDED extends TestOutcome

  case object FAILED extends TestOutcome

  case object STARTING extends TestOutcome
}
