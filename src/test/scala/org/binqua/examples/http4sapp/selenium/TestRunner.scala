package org.binqua.examples.http4sapp.selenium

import org.binqua.scalatest.reporter.ScreenshotReporterRunner
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.{Args, BeforeAndAfterAll}

class TestRunner extends AnyFunSuite with BeforeAndAfterAll {

  override def beforeAll(): Unit = {
    System.setProperty("reportDestinationRoot", "this_is_great")
  }

  override def afterAll(): Unit =
    System.clearProperty("reportDestinationRoot")

  test("this is a test") {
    new AppSpec1().run(None, Args(new ScreenshotReporterRunner()))
  }

}
