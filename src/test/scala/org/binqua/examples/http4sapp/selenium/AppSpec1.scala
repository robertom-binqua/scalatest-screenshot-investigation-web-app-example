package org.binqua.examples.http4sapp.selenium

import org.binqua.examples.http4sapp.app.ConfiguredChrome
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec._
import org.scalatest.matchers._

class AppSpec1 extends AnyFeatureSpec with should.Matchers with ConfiguredChrome with GivenWhenThen {

  val host = "http://localhost:8081/"

  Feature("f1: Navigation bar should work") {
    Scenario("s1: we can go from home to page3") {
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

    Scenario("s2: we can go from page2 to home") {
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

  Feature("f2: Navigation bar should work:2") {
    Scenario("s1: we can go from home to page2") {
      go to (host + "home.html")
      pageTitle should be("Home")

      click on linkText("Page1")
      pageTitle should be("Page 1")

      click on linkText("Page2")
      pageTitle should be("Page 2")
    }

    Scenario("s2: we can go from page2 to home") {
      go to (host + "page2.html")
      pageTitle should be("Page 2")

      click on linkText("Page1")
      pageTitle should be("Page 1")

      click on linkText("Home")
      pageTitle should be("Home")

    }
  }

}
