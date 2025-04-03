package org.binqua.examples.http4sapp

import org.scalatest.featurespec._
import org.scalatest.matchers._

class AppSpec1 extends AnyFeatureSpec with should.Matchers with ConfiguredChrome  {

  val host = "http://localhost:8081/"

  Feature("Navigation bar should work:1") {
    Scenario("we can go from home to page3") {
      go to (host + "home.html")
      pageTitle should be("Home")

      click on linkText("Page1")
      pageTitle should be("Page 1")

      click on linkText("Page2")
      pageTitle should be("Page 2")

      click on linkText("Page3")
      pageTitle should be("Page 3")

      click on linkText("Home")
      pageTitle should be("Home")
    }

    Scenario("we can go from page2 to home") {
      go to (host + "page3.html")
      pageTitle should be("Page 3")

      click on linkText("Page2")
      pageTitle should be("Page 2")

      click on linkText("Page1")
      pageTitle should be("Page 1")

      click on linkText("Home")
      pageTitle should be("Home")
    }
  }
  Feature("Navigation bar should work:2") {
    Scenario("we can go from home to page2") {
      go to (host + "home.html")
      pageTitle should be("Home")

      click on linkText("Page1")
      pageTitle should be("Page 1")

      click on linkText("Page2")
      pageTitle should be("Page 2")
    }

    Scenario("we can go from page2 to home") {
      go to (host + "page2.html")
      pageTitle should be("Page 2")

      click on linkText("Page1")
      pageTitle should be("Page 1")

      click on linkText("Home")
      pageTitle should be("Home")

    }
  }

}
