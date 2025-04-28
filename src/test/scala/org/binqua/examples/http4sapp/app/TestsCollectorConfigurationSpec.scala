package org.binqua.examples.http4sapp.app

import cats.implicits.catsSyntaxEitherId
import munit.FunSuite
import org.binqua.scalatest.reporter.TestsCollectorConfiguration

import java.io.File
import java.nio.file.{Files, Path}

class TestsCollectorConfigurationSpec extends FunSuite {

  test("given reportDirParent does not exist TestsCollectorConfiguration cannot be created") {

    val tempDir: Path = Files.createTempDirectory("TestsCollectorConfigurationSpec")

    def toString(prefix: Path, suffix: String) =
      prefix.toFile.getAbsolutePath + File.separator + suffix.replaceAll("/", File.separator)

    assertEquals(
      TestsCollectorConfiguration.from(reportDirParent = tempDir.resolve("IDoNotExist").toFile),
      (s"ReportDir ${toString(tempDir, "IDoNotExist/report")} should exist but it does not - " +
        s"ScreenshotsDir ${toString(tempDir, "IDoNotExist/report/screenshots")} should exist but it does not").asLeft
    )
  }

}
