package org.binqua.scalatest.reporter

import io.circe.{Encoder, Json}
import org.scalatest.events.Ordinal

object Step {
  implicit val encoder: Encoder[Step] = Encoder.instance { step =>
    Json.obj(
      "message" -> Json.fromString(step.message),
      "timestamp" -> Json.fromLong(step.timestamp),
      "id" -> Json.fromString(s"st_${step.ordinal.toList.mkString("_")}")
    )
  }
}

case class Step(ordinal: Ordinal, message: String, throwable: Option[Throwable], timestamp: Long)
