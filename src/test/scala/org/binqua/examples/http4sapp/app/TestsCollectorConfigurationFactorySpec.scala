package org.binqua.examples.http4sapp.app

import cats.implicits.catsSyntaxEitherId
import munit.FunSuite

import java.io.File
import java.time.{Clock, ZoneId, ZonedDateTime}

class TestsCollectorConfigurationFactorySpec extends FunSuite {

  private val systemPropertyForTest = "TestsCollectorConfigurationFactorySpec_exampleOfSystemPropertyKey"

  override def beforeAll(): Unit =
    System.clearProperty(systemPropertyForTest)

  override def afterAll(): Unit =
    System.clearProperty(systemPropertyForTest)

  test("given system property does not exist, we cannot proceed") {

    val time = ZonedDateTime.of(2021, 2, 18, 13, 1, 2, 0, ZoneId.of("UTC"))
    val fixedClock: Clock = Clock.fixed(time.toInstant, time.getZone)

    val actual: Either[String, TestsCollectorConfiguration] = TestsCollectorConfigurationFactory.create("exampleOfSystemPropertyKey", fixedClock)

    assertEquals(actual, "System property <exampleOfSystemPropertyKey> specifying the root dir of the report missing. I cannot proceed".asLeft)

  }

  test("given a valid systemProperty reportDestinationKey, TestsCollectorConfigurationFactory creates dirs for report and screenshots with a valid prefix") {

    val time = ZonedDateTime.of(2021, 2, 18, 13, 1, 2, 0, ZoneId.of("UTC"))
    val fixedClock: Clock = Clock.fixed(time.toInstant, time.getZone)

    System.setProperty(systemPropertyForTest, "tests_reports")

    val actual: Either[String, TestsCollectorConfiguration] =
      TestsCollectorConfigurationFactory.create(systemPropertyReportDestinationKey = systemPropertyForTest, fixedClock = fixedClock)

    val expRoot: File =
      new File(new File(System.getProperty("user.dir")).getAbsoluteFile + File.separator + "tests_reports" + File.separator + "at_18_Feb_2021_at_13_01_02")

    val expectedReportDir = new File(expRoot.getAbsoluteFile + File.separator + "report")
    val expectedScreenshotDir = new File(expRoot.getAbsoluteFile + File.separator + "screenshots")

    assertEquals(actual.map(_.reportLocation), expectedReportDir.asRight)
    assertEquals(actual.map(_.screenshotLocation), expectedScreenshotDir.asRight)

    assertEquals(expectedReportDir.exists(), true)
    assertEquals(expectedScreenshotDir.exists(), true)

  }

}
