package org.binqua.scalatest.integration

import cats.effect.{IO, Resource}
import com.comcast.ip4s.IpLiteralSyntax
import fs2.Pipe
import fs2.io.file.{Files, Path}
import munit.CatsEffectSuite
import org.binqua.scalatest.integration.http4sapp.Http4sAppServer
import org.binqua.scalatest.reporter.ConfiguredChrome
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should

class IntegrationSpec extends CatsEffectSuite {

  val expectedFilesToBeGenerated: List[String] = List(
    "report",
    "report/testsReport.js",
    "report/screenshots",
    "report/screenshots/scenario_ordinal_1_3",
    "report/screenshots/scenario_ordinal_1_3/original",
    "report/screenshots/scenario_ordinal_1_3/original/2_ON_EXIT_PAGE.png",
    "report/screenshots/scenario_ordinal_1_3/original/1_ON_ENTER_PAGE.png",
    "report/screenshots/scenario_ordinal_1_3/sources",
    "report/screenshots/scenario_ordinal_1_3/sources/2_ON_EXIT_PAGE.txt",
    "report/screenshots/scenario_ordinal_1_3/sources/1_ON_ENTER_PAGE.txt"
  )

  private val systemPropertyKey = "reportDestinationRoot"
  private val reportDestinationDirName = "test_dir_created_during_test"
  private val reportDestinationDirPath = Path(reportDestinationDirName)

  override def beforeAll(): Unit =
    System.clearProperty(systemPropertyKey)

  override def afterAll(): Unit =
    System.clearProperty(systemPropertyKey)

  test("given we run selenium test programmatically and given we specified the Report class, a report should be generated") {
    val fileGenerated = (for {
      _ <- Http4sAppServer.run[IO](FeaturesForTestPurpose.port)
      _ <- Resource.eval(Files[IO].deleteRecursively(reportDestinationDirPath).attempt)
      _ <- Resource.eval(IO.systemPropertiesForIO.set(systemPropertyKey, reportDestinationDirName))
    } yield ())
      .use(_ =>
        for {
          _ <- runSeleniumTest()
          actualReportFiles <- collectAllReportFiles(reportDestinationDirPath)
        } yield actualReportFiles
      )

    assertIO(obtained = fileGenerated, returns = expectedFilesToBeGenerated)
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

  private def runSeleniumTest(): IO[Unit] = {
    import org.scalatest._
    IO( tools.Runner.run(Array("-R", ".", "-o","-s", "org.binqua.scalatest.integration.FeaturesForTestPurpose","-C","org.binqua.scalatest.reporter.ScreenshotReporterRunner")))
  }


}

object FeaturesForTestPurpose {
  val port = port"8081"
}

class FeaturesForTestPurpose extends AnyFeatureSpec with should.Matchers with ConfiguredChrome with GivenWhenThen {

  val host = s"http://localhost:${FeaturesForTestPurpose.port.value}/"

  Feature("f1") {
    Scenario("s11") {
      info("when we login into the app")
      info("and the user is happy")
      val str = host + "home.html"
      go to str
      pageTitle should be("Home")

      click on linkText("Page1")
      pageTitle should be("Page 1")
    }
    Scenario("s12") {
      Given("1")
      Then("3")
      go to (host + "home.html")
      pageTitle should be("Home")
    }
  }

}
