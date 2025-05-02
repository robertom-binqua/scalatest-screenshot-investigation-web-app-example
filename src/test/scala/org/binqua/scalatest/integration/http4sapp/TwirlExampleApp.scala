package org.binqua.scalatest.integration.http4sapp

import cats.effect.Concurrent
import org.binqua.scalatest.integration.model.NavigationMenu
import org.http4s.dsl.Http4sDsl
import org.http4s.twirl._
import org.http4s.{HttpApp, HttpRoutes}

private case class TwirlExampleApp[F[_] : Concurrent]() extends Http4sDsl[F] {

  def app(): HttpApp[F] =
    HttpRoutes
      .of[F] {
        case GET -> Root / pageIdentifier ~ "html" =>
          NavigationMenu.from(pageIdentifier) match {
            case Left(_) => BadRequest(s"Ops! it looks like there is no page $pageIdentifier.html")
            case Right(theNavigationMenu) => Ok(org.binqua.scalatest.integration.http4sapp.html.main(theNavigationMenu))
          }
      }
      .orNotFound

}
