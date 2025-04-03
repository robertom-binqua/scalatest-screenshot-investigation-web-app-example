package org.binqua.examples.http4sapp

import cats.effect.{IO, IOApp}

object Main extends IOApp.Simple {
  val run: IO[Unit] = Http4sAppServer.run[IO]
}
