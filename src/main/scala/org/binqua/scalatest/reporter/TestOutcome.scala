package org.binqua.scalatest.reporter

import io.circe.{Encoder, Json}

sealed trait TestOutcome
object TestOutcome {

  implicit val encoder: Encoder[TestOutcome] = (a: TestOutcome) => Json.fromString(a.toString.toLowerCase)

  case object SUCCEEDED extends TestOutcome

  case object FAILED extends TestOutcome

  case object STARTING extends TestOutcome
}
