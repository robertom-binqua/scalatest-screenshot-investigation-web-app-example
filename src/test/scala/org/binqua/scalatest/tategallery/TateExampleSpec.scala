package org.binqua.scalatest.tategallery

import org.binqua.scalatest.web.{ConfiguredChrome, WithScreenshotsSupport}
import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}
import org.openqa.selenium.{By, WebDriver, WebElement}
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should

import java.time.Duration
import scala.jdk.CollectionConverters.CollectionHasAsScala

class TateExampleSpec extends AnyFeatureSpec with should.Matchers with ConfiguredChrome with GivenWhenThen with WithScreenshotsSupport{

  Feature("feature 1") {

    Scenario("feature 2") {

      note(s"given we go to somewhere")

      note("and given we accept to proceed")

      note("and given we want to become a member")

      note("and given we are absolutely sure to continue")

      note("after we select no gift aid")

      note("we can add the purchase to the basket")

      note("and secure checkout the basket")

    }
  }

  Feature("feature 3") {

    Scenario("feature 4") {

      note(s"given we go to somewhere")

      note("and given we accept to proceed")

      note("and given we want to become a member")

      note("and given we are absolutely sure to continue")

      note("after we select no gift aid")

      note("we can add the purchase to the basket")

      note("and secure checkout the basket")

    }
  }

  Feature("we can visit https://www.tate.org.uk and become a member") {

    Scenario("Happy path: secureCheckout our membership") {

      val str = "https://www.tate.org.uk"

      note(s"given we go to $str")
      go to str
      pageTitle should be("Tate")

      note("and given we accept to proceed")
      takeAScreenshot(click on NavigationLinks.accept())

      note("and given we want to become a member")
      takeAScreenshot(click on NavigationLinks.becomeAMember())

      note("and given we are absolutely sure to continue")
      takeAScreenshot(click on NavigationLinks.continue())

      note("after we select no gift aid")
      takeAScreenshot(click on NavigationLinks.noGifAid())

      note("we can add the purchase to the basket")
      takeAScreenshot(click on NavigationLinks.addToBasket())

      note("and secure checkout the basket")
      takeAScreenshot(click on NavigationLinks.secureCheckout())

      val wait = new WebDriverWait(webDriver, Duration.ofSeconds(10))
      wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//button[normalize-space()='Continue']")))

    }

  }

  object NavigationLinks {
    def continue()(implicit webDriver:WebDriver): WebElement = {
      webDriver.findElement(By.xpath("//button[contains(.,'Continue')]"))
    }

     def becomeAMember()(implicit webDriver:WebDriver): WebElement = {
       webDriver.findElements(By.xpath("//*[contains(text(), 'Become a Member')]")).asScala.toArray.toList.last
    }

     def accept()(implicit webDriver:WebDriver): WebElement = {
      webDriver.findElement(By.xpath("//*[contains(text(), 'I Accept')]"))
    }

    def noGifAid()(implicit webDriver:WebDriver): WebElement = {
      webDriver.findElement(By.xpath("//input[@id='form--dont-add-gift__radio-example']"))
    }

    def addToBasket()(implicit webDriver:WebDriver): WebElement = {
      webDriver.findElement(By.xpath("//button[@data-label='Add To Basket']"))
    }

    def secureCheckout()(implicit webDriver:WebDriver): WebElement = {
      webDriver.findElement(By.xpath("//button[@name='dwfrm_cart_checkoutCart']"))
    }
  }

}
