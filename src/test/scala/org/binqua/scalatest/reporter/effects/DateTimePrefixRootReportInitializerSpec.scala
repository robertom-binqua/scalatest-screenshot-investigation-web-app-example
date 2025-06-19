package org.binqua.scalatest.reporter.effects

import cats.Applicative
import cats.effect.IO
import cats.effect.kernel.Clock
import fs2.io.file.{Files, Path}
import munit.CatsEffectSuite

import java.time.{Instant, ZoneId, ZonedDateTime}
import scala.concurrent.duration.{DurationLong, FiniteDuration}

class DateTimePrefixRootReportInitializerSpec extends CatsEffectSuite {

  val fixedTime: ZonedDateTime = ZonedDateTime.of(2021, 2, 18, 13, 1, 2, 0, ZoneId.of("UTC"))

  val fixedInstant: Instant = fixedTime.toInstant
  val fixedMillis: Long = fixedInstant.toEpochMilli
  val fixedDuration: FiniteDuration = fixedMillis.millis

  val myClock: Clock[IO] = new Clock[IO] {
    override def applicative: Applicative[IO] = Applicative[IO]

    override def monotonic: IO[FiniteDuration] = IO.pure(0.seconds)

    override def realTime: IO[FiniteDuration] = IO.pure(fixedDuration)
  }

  test("createRoot: given a value for the test_report_destination_root system property, all directories are created properly") {

    val expectedPath: Path = Path("test_dir_created_during_test/at_18_Feb_2021_at_13_01_02/report/screenshots")

    val result: IO[Boolean] = for {
      _ <- Files[IO].deleteIfExists(expectedPath)
      _ <- IO(System.setProperty("test_report_destination_root", "test_dir_created_during_test"))
      underTest <- IO(new DateTimePrefixRootReportInitializer[IO](myClock))
      _ <- underTest.createRoot()
      result <- Files[IO].exists(expectedPath)
    } yield result

    assertIO(result, true, s"$expectedPath should exist but it does not")

  }

  test(
    "createRoot: given that a test_report_destination_root system property is not defined," +
      " then we use the default test_report_root value and all directories are created properly"
  ) {

    val expectedPath: Path = Path("test_report_root/at_18_Feb_2021_at_13_01_02/report/screenshots")

    val result: IO[Boolean] = for {
      _ <- Files[IO].deleteIfExists(expectedPath)
      _ <- IO(System.clearProperty("test_report_destination_root"))
      underTest <- IO(new DateTimePrefixRootReportInitializer[IO](myClock))
      _ <- underTest.createRoot()
      result <- Files[IO].exists(expectedPath)
    } yield result

    assertIO(result, true, s"$expectedPath should exist but it does not")
  }

  test(
    "createRoot: given that a test_report_destination_root system property is empty," +
      " then we use the default test_report_root value and all directories are created properly"
  ) {

    val expectedPath: Path = Path("test_report_root/at_18_Feb_2021_at_13_01_02/report/screenshots")

    val result: IO[Boolean] = for {
      _ <- Files[IO].deleteIfExists(expectedPath)
      _ <- IO(System.setProperty("test_report_destination_root", "    "))
      underTest <- IO(new DateTimePrefixRootReportInitializer[IO](myClock))
      _ <- underTest.createRoot()
      result <- Files[IO].exists(expectedPath)
    } yield result

    assertIO(result, true, s"$expectedPath should exist but it does not")
  }

}
