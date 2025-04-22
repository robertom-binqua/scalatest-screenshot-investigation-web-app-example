package org.binqua.examples.http4sapp.app

import cats.implicits.catsSyntaxEitherId
import munit.FunSuite

import java.io.File

class TestsCollectorConfigurationSpec extends FunSuite {

  test("reportLocationFile and screenshotsRoot have to exist otherwise we cannot proceed") {
    assertEquals(TestsCollectorConfiguration.from(reportLocationRoot = new File("1"), screenshotsRoot = new File("2")),"reportLocationRoot dir 1 has to exist but it does not - screenshotsRoot dir 2 has to exist but it does not".asLeft)
  }

}
