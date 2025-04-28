package org.binqua.examples.http4sapp.selenium

import org.binqua.examples.http4sapp.app.ConfiguredChrome
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec._
import org.scalatest.matchers._

class AppSpec1 extends AnyFeatureSpec with should.Matchers with ConfiguredChrome with GivenWhenThen {

  val host = "http://localhost:8081/"

  Feature("f1") {
    Scenario("s11") {
      info("when we login into the app")
      info("given the db is down")
      info("we can retry 3 time")
      info("and the second time we succedeed")
      info("Given we enter our credentials")
      info("we receive a nice welcome page")
      info("and the user is happy")
      go to (host + "home.html")
      pageTitle should be("Home")

      click on linkText("Page1")
      pageTitle should be("Page 1")

      click on linkText("Page2")
      pageTitle should be("Page 2")

      click on linkText("Page3")
      pageTitle should be("Page 3")

      click on linkText("Home")
      info("test finished")
      pageTitle should be("Home")
    }
    Scenario("s12") {
      Given("1")
      Then("3")
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
    Scenario("s13") {
      Given("1")
      Then("2")
      And("3")
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
  }
  Feature("f2") {
    Scenario("s11") {
      Given("1")
      Then("3")
      And("4")
      Given("7")
      When("8")
      Then("9")
      And("11")
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
    Scenario("s12") {
      Given("1")
      Then("3")
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
    Scenario("s13") {
      Given("1")
      Then("2")
      And("3")
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
  }
  Feature("this is a feature with no screenshots and a long name") {
    Scenario("no screenshots here but a  throw new RuntimeException...") {
      Given("1")
      Then("3")
      throw new RuntimeException("f3 s11 fails")
      And("4")
    }
    Scenario("s1111") {
      info("1")
      info("3")
      throw new RuntimeException("f3 s1111 fails")
      info("4")
    }
    Scenario("s12") {
      Given("1")
      Then("3")
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
    Scenario("s13") {
      Given("1")
      Then("2")
      And("3")
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
  }

}
