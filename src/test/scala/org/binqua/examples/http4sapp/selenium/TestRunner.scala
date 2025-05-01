package org.binqua.examples.http4sapp.selenium

import cats.effect.{Async, IO, Resource}
import com.comcast.ip4s.{ipv4, port}
import fs2.io.file.{Files, Path}
import munit.CatsEffectSuite
import org.binqua.examples.http4sapp.selenium.TestRunner.startTheServer
import org.binqua.scalatest.reporter.ScreenshotReporterRunner
import org.http4s.HttpApp
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.middleware.Logger
import org.scalatest.Args

import java.io.File
import java.nio.file.Paths

object TestRunner:

  def startTheServer: Resource[IO, Server] = {
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8083")
      .withHttpApp(createHttpApp())
      .build
  }

  private def createHttpApp[F[_]: Async](): HttpApp[F] =
    Logger.httpApp(logHeaders = true, logBody = true)(httpApp = TwirlExampleApp().app())

class TestRunner extends CatsEffectSuite:

  override def beforeAll(): Unit = {
    System.setProperty("reportDestinationRoot", "this_is_great")
  }

  override def afterAll(): Unit =
    System.clearProperty("reportDestinationRoot")

  test("this is a test1") {
    val value: IO[Unit] = IO(42).map(it => assertEquals(it, 42))
    value
  }

  test("this is a test") {

    val result: IO[Unit] = for {
      allocated <- startTheServer.allocated
      _ <- assertFilesAreCreated()
      _ <- allocated._2
    } yield ()

    assertIO_(result)

//    allocated.flatMap((x: (Server, IO[Unit])) =>
//      IO(new AppSpec1().run(None, Args(new ScreenshotReporterRunner())))
//    )

  }

  def assertFilesAreCreated(): IO[Unit] = {
    val path: Path = fs2.io.file.Path.fromNioPath(Paths.get("tests_reports")).
    Files(path).exists()
    ???
  }
