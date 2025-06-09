package org.binqua.scalatest.integration

import cats.effect.{IO, Resource}
import cats.instances.unit
import com.comcast.ip4s.IpLiteralSyntax
import fs2.Pipe
import fs2.io.file.{Files, Path}
import munit.CatsEffectSuite
import org.binqua.scalatest.integration.http4sapp.Http4sAppServer
import org.binqua.scalatest.web.{ConfiguredChrome, WithScreenshotsSupport}
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should

class IntegrationSpec extends CatsEffectSuite {

  private val systemPropertyKey = "reportDestinationRoot"
  private val reportDestinationDirName = "test_dir_created_during_test"
  private val reportDestinationDirPath = Path(reportDestinationDirName)

  test(
    "we can run a test containing more tests, more features and more scenarios, to create a .json to be used in the react app development - 22 files should be generated"
  ) {

    val expectedFilesToBeGenerated: List[String] = List(
      "report",
      "report/testsReport.json",
      "report/screenshots",
      "report/screenshots/scenario_ordinal_1_3",
      "report/screenshots/scenario_ordinal_1_3/original",
      "report/screenshots/scenario_ordinal_1_3/original/2_ON_EXIT_PAGE.png",
      "report/screenshots/scenario_ordinal_1_3/original/6_ON_EXIT_PAGE.png",
      "report/screenshots/scenario_ordinal_1_3/original/8_ON_EXIT_PAGE.png",
      "report/screenshots/scenario_ordinal_1_3/original/5_ON_ENTER_PAGE.png",
      "report/screenshots/scenario_ordinal_1_3/original/1_ON_ENTER_PAGE.png",
      "report/screenshots/scenario_ordinal_1_3/original/4_ON_EXIT_PAGE.png",
      "report/screenshots/scenario_ordinal_1_3/original/7_ON_ENTER_PAGE.png",
      "report/screenshots/scenario_ordinal_1_3/original/3_ON_ENTER_PAGE.png",
      "report/screenshots/scenario_ordinal_1_3/sources",
      "report/screenshots/scenario_ordinal_1_3/sources/8_ON_EXIT_PAGE.txt",
      "report/screenshots/scenario_ordinal_1_3/sources/2_ON_EXIT_PAGE.txt",
      "report/screenshots/scenario_ordinal_1_3/sources/6_ON_EXIT_PAGE.txt",
      "report/screenshots/scenario_ordinal_1_3/sources/7_ON_ENTER_PAGE.txt",
      "report/screenshots/scenario_ordinal_1_3/sources/3_ON_ENTER_PAGE.txt",
      "report/screenshots/scenario_ordinal_1_3/sources/5_ON_ENTER_PAGE.txt",
      "report/screenshots/scenario_ordinal_1_3/sources/1_ON_ENTER_PAGE.txt",
      "report/screenshots/scenario_ordinal_1_3/sources/4_ON_EXIT_PAGE.txt",
      "report/screenshots/scenario_ordinal_1_3/withNoHtml",
      "report/screenshots/scenario_ordinal_1_3/withNoHtml/8_ON_EXIT_PAGE.txt",
      "report/screenshots/scenario_ordinal_1_3/withNoHtml/2_ON_EXIT_PAGE.txt",
      "report/screenshots/scenario_ordinal_1_3/withNoHtml/6_ON_EXIT_PAGE.txt",
      "report/screenshots/scenario_ordinal_1_3/withNoHtml/7_ON_ENTER_PAGE.txt",
      "report/screenshots/scenario_ordinal_1_3/withNoHtml/3_ON_ENTER_PAGE.txt",
      "report/screenshots/scenario_ordinal_1_3/withNoHtml/5_ON_ENTER_PAGE.txt",
      "report/screenshots/scenario_ordinal_1_3/withNoHtml/1_ON_ENTER_PAGE.txt",
      "report/screenshots/scenario_ordinal_1_3/withNoHtml/4_ON_EXIT_PAGE.txt"
    )

    val actualFileGenerated: IO[List[String]] = runTest("org.binqua.scalatest.integration.ReactAppUsagePurpose")
      .map(_.sorted)
    assertIO(obtained = actualFileGenerated, expectedFilesToBeGenerated.sorted)
  }

  private def runTest(testToRun: String): IO[List[String]] = {
    val fileGenerated = (for {
      _ <- Http4sAppServer.run[IO](ReactAppUsagePurpose.port)
      _ <- Resource.eval(Files[IO].deleteRecursively(reportDestinationDirPath).attempt)
      _ <- Resource.eval(IO.systemPropertiesForIO.set(systemPropertyKey, reportDestinationDirName))
      _ <- Resource.onFinalize[IO](IO.systemPropertiesForIO.clear(systemPropertyKey).as(unit))
    } yield ())
      .use(_ =>
        for {
          _ <- runSeleniumTest(testToRun)
          actualReportFiles <- collectAllReportFiles(reportDestinationDirPath)
        } yield actualReportFiles
      )
    fileGenerated
  }

  private def collectAllReportFiles(root: Path): IO[List[String]] = {
    val separator = '/'
    val removeTheFirst2Dirs: Pipe[IO, Path, String] = {
      _.filter(_.toString.split(separator).length > 2)
        .map(_.toString.split(separator).drop(2).mkString("/"))
    }

    Files[IO]
      .walk(root)
      .through(removeTheFirst2Dirs)
      .compile
      .toList
  }

  private def runSeleniumTest(testToRun: String): IO[Unit] = {
    import org.scalatest._
    IO(tools.Runner.run(Array("-R", ".", "-o", "-s", testToRun, "-C", "org.binqua.scalatest.reporter.ScreenshotReporterRunner")))
  }

}

object ReactAppUsagePurpose {
  val port = port"8081"
}

class ReactAppUsagePurpose extends AnyFeatureSpec with should.Matchers with WithScreenshotsSupport with ConfiguredChrome with GivenWhenThen {

  val host = s"http://localhost:${ReactAppUsagePurpose.port.value}/"

  Feature("We can go through all the page of our app from home to page 4") {
    Scenario("we can go from home page to last page") {
      note("Given we go to the home page")
      val str = host + "home.html"
      go to str
      pageTitle should be("Home")

      note("When we click page 1")
      takeAScreenshot(click on linkText("Page1"))

      note("Then we jump to Page1")
      pageTitle should be("Page 1")

      note("And when we click page 2")
      takeAScreenshot(click on linkText("Page2"))

      note("Then we jump to Page2")
      pageTitle should be("Page 2")

      note("And when we click page 3")
      takeAScreenshot(click on linkText("Page3"))

      note("Then we jump to Page3")
      pageTitle should be("Page 3")

      note("And when we click page 4")
      takeAScreenshot(click on linkText("Page4"))

      note("Then we jump to Page4")
      pageTitle should be("Page 4")
    }

  }
}
