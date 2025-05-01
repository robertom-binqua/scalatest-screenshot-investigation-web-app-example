package org.binqua.examples.http4sapp.selenium

import cats.effect.Concurrent
import org.binqua.examples.http4sapp.selenium.model.NavigationMenu
import org.http4s.dsl.Http4sDsl
import org.http4s.twirl.*
import org.http4s.{EntityEncoder, HttpApp, HttpRoutes}

private case class TwirlExampleApp[F[_] : Concurrent]() extends Http4sDsl[F] {

  def app(): HttpApp[F] =
    HttpRoutes
      .of[F] {
        case GET -> Root / pageIdentifier ~ "html" =>
          NavigationMenu.from(pageIdentifier) match {
            case Left(_) => BadRequest(s"Ops! it looks like there is no page $pageIdentifier.html")
            case Right(theNavigationMenu) =>
              Ok(org.binqua.examples.http4sapp.html.main(theNavigationMenu))
          }
      }
      .orNotFound

}
