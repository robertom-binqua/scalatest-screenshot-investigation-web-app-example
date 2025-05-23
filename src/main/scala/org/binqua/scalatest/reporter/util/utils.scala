package org.binqua.scalatest.reporter.util

object utils {

  implicit class EitherOps[L, R](either: Either[L, R]) {
    def getOrThrow: R = {
      either match {
        case Right(value) => value
        case Left(error)  => throw new RuntimeException(s"Unexpected Left: $error")
      }
    }
  }

  def clean(toBeCleaned: String): String = LazyList.from(toBeCleaned.split("\n")).filter(_.trim.nonEmpty).map(_.trim).mkString("\n")

}
