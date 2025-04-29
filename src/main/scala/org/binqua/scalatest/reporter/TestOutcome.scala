package org.binqua.scalatest.reporter

import io.circe.{Encoder, Json}

sealed trait TestOutcome

object TestOutcome:

  given encoder: Encoder[TestOutcome] = (testOutcome: TestOutcome) => Json.fromString(testOutcome.toString.toLowerCase)

  case object SUCCEEDED extends TestOutcome

  case object FAILED extends TestOutcome

  case object STARTING extends TestOutcome
