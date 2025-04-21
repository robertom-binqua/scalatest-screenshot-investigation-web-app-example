package org.binqua.examples.http4sapp.selenium

import org.binqua.examples.http4sapp.app.ConfiguredChrome
import org.scalatest.featurespec._
import org.scalatest.matchers._

class AppSpec2 extends AnyFeatureSpec with should.Matchers with ConfiguredChrome {

  val host = "http://localhost:8081/"

  Feature("f1") {
    Scenario("s1-f1") {
      go to (host + "home.html")
      pageTitle should be("Home")

      click on linkText("Page1")
      pageTitle should be("Page 1")

      click on linkText("Page2")
      pageTitle should be("Page 2")
    }

    Scenario("s2-f1") {
      go to (host + "page2.html")
      pageTitle should be("Page 2")

      click on linkText("Page1")
      pageTitle should be("Page 1")

      click on linkText("Home")
      pageTitle should be("Home")
    }
  }
  Feature("f2") {
    Scenario("s1-f2") {
      go to (host + "home.html")
      pageTitle should be("Home")

      click on linkText("Page1")
      pageTitle should be("Page 1")

      click on linkText("Page2")
      pageTitle should be("Page 2")
    }

    Scenario("s2-f2") {
      go to (host + "page2.html")
      pageTitle should be("Page 2")

      click on linkText("Page1")
      pageTitle should be("Page 1")

      click on linkText("Home")
      pageTitle should be("Home")

    }
  }

}
