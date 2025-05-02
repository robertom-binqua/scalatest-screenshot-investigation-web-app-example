package org.binqua.examples.http4sapp.selenium.http4sapp

import cats.effect.{Async, Resource}
import com.comcast.ip4s._
import org.http4s.HttpApp
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.middleware.Logger

object Http4sAppServer {

  def run[F[_] : Async]: Resource[F, Server] =
    EmberServerBuilder
      .default[F]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8081")
      .withHttpApp(createHttpApp())
      .build

  private def createHttpApp[F[_] : Async](): HttpApp[F] =
    Logger.httpApp(logHeaders = true, logBody = true)(httpApp = TwirlExampleApp().app())
}
