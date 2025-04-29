package org.binqua.examples.http4sapp.app

import cats.implicits.catsSyntaxEitherId
import munit.FunSuite
import org.binqua.examples.http4sapp.app.TestUtil.assertPathExist
import org.binqua.scalatest.reporter.{TestsCollectorConfiguration, TestsCollectorConfigurationFactory}

import java.io.File
import java.nio.file.{Path, Paths}
import java.time.{Clock, ZoneId, ZonedDateTime}

class TestsCollectorConfigurationFactorySpec extends FunSuite {

  private val systemPropertyForTest = "TestsCollectorConfigurationFactorySpec_exampleOfSystemPropertyKey"

  override def beforeAll(): Unit =
    System.clearProperty(systemPropertyForTest)

  override def afterAll(): Unit =
    System.clearProperty(systemPropertyForTest)

  test("given system property does not exist, we cannot proceed") {

    val actual: Either[String, TestsCollectorConfiguration] = TestsCollectorConfigurationFactory.create("abc", Clock.systemUTC())

    assertEquals(actual, "The system property abc specifying the root dir of the report missing. I cannot proceed".asLeft)
  }

  test("given system property is empty, we cannot proceed") {

    System.setProperty(systemPropertyForTest, "  ")

    val actual: Either[String, TestsCollectorConfiguration] =
      TestsCollectorConfigurationFactory.create(systemPropertyReportDestinationKey = "abc", fixedClock = Clock.systemUTC())

    assertEquals(actual, "The system property abc specifying the root dir of the report missing. I cannot proceed".asLeft)
  }

  test("given a valid systemProperty reportDestinationKey, TestsCollectorConfigurationFactory creates dirs for report and screenshots with a valid prefix") {

    val time = ZonedDateTime.of(2021, 2, 18, 13, 1, 2, 0, ZoneId.of("UTC"))
    val fixedClock: Clock = Clock.fixed(time.toInstant, time.getZone)

    System.setProperty(systemPropertyForTest, "tests_reports")

    val actual: Either[String, TestsCollectorConfiguration] =
      TestsCollectorConfigurationFactory.create(systemPropertyReportDestinationKey = systemPropertyForTest, fixedClock = fixedClock)

    val expRoot: File =
      new File(new File(System.getProperty("user.dir")).getAbsolutePath + File.separator + "tests_reports" + File.separator + "at_18_Feb_2021_at_13_01_02")

    val expectedReportDir: Path = Paths.get(expRoot.getAbsolutePath,"report")
    val expectedScreenshotDir: Path = expectedReportDir.resolve("screenshots")

    assertEquals(actual.map(_.reportRootLocation), expectedReportDir.toFile.asRight)
    assertEquals(actual.map(_.screenshotsRootLocation), expectedScreenshotDir.toFile.asRight)

    assertPathExist(expectedScreenshotDir)
    assertPathExist(expectedReportDir)

  }

}
