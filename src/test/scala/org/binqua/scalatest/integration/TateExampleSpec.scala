package org.binqua.scalatest.integration

import org.binqua.scalatest.web.ConfiguredChrome
import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}
import org.openqa.selenium.{By, WebDriver, WebElement}
import org.scalatest.{GivenWhenThen, Ignore}
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should

import java.time.Duration
import scala.jdk.CollectionConverters.CollectionHasAsScala

@Ignore
class TateExampleSpec extends AnyFeatureSpec with should.Matchers with ConfiguredChrome with GivenWhenThen {

  Feature("we can visit https://www.tate.org.uk and become a member") {

    Scenario("Happy path: secureCheckout our membership") {

      val str = "https://www.tate.org.uk"

      info(s"given we go to $str")
      go to str
      pageTitle should be("Tate")

      info("and given we accept to proceed")
      click on NavigationLinks.accept()

      info("and given we want to become a member")
      click on NavigationLinks.becomeAMember()

      info("and given we are absolutely sure to continue")
      click on NavigationLinks.continue()

      info("after we select no gift aid")
      click on NavigationLinks.noGifAid()

      info("we can add the purchase to the basket")
      click on NavigationLinks.addToBasket()

      info("and secure checkout the basket")
      click on NavigationLinks.secureCheckout()

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
