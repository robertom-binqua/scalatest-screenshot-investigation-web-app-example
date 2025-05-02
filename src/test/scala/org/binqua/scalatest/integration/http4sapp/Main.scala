package org.binqua.scalatest.integration.http4sapp

import cats.effect.{IO, IOApp}
import com.comcast.ip4s.IpLiteralSyntax

object Main extends IOApp.Simple {
  val run: IO[Unit] = Http4sAppServer.run[IO](port"8081").useForever
}
