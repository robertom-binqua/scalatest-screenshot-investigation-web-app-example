package org.binqua.examples.http4sapp.util

object utils {

  implicit class EitherOps[L,R](either: Either[L, R]) {
    def getOrThrow: R = {
      either match {
        case Right(value) => value
        case Left(error)  => throw new RuntimeException(s"Unexpected Left: $error")
      }
    }
  }

}
