package org.binqua.examples.http4sapp.app

import cats.implicits.{catsSyntaxEitherId, catsSyntaxOptionId}
import io.circe.generic.semiauto.deriveEncoder
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}
import org.scalatest.events.Ordinal

import java.io.File

object Step {
  implicit val featureEncoder: Encoder[Step] = Encoder.instance { step =>
    Json.obj(
      "message" -> Json.fromString(step.message),
      "timestamp" -> Json.fromLong(step.timestamp),
      "ordinal" -> Json.fromString(step.ordinal.toList.mkString("_"))
    )
  }
}

case class Step(ordinal: Ordinal, message: String, throwable: Option[Throwable], timestamp: Long)
