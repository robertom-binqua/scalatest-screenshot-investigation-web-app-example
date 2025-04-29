package org.binqua.scalatest.reporter

import io.circe.{Encoder, Json}

enum TestOutcome:

  case SUCCEEDED, FAILED, STARTING

object TestOutcome:

  given Encoder[TestOutcome] = (testOutcome: TestOutcome) =>  Json.fromString(testOutcome.toString.toLowerCase)
