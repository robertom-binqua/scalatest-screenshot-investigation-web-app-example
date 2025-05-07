package org.binqua.scalatest.reporter

import org.binqua.scalatest.reporter.ScreenshotMoment.{ON_ENTER_PAGE, ON_EXIT_PAGE}
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.openqa.selenium.support.events.{EventFiringDecorator, WebDriverListener}
import org.openqa.selenium.{OutputType, TakesScreenshot, WebDriver, WebElement}
import org.scalatest.{BeforeAndAfterAll, Suite}
import org.scalatestplus.selenium.{Driver, WebBrowser}

import java.lang.reflect.Method

trait ConfiguredChrome extends WebBrowser with Driver with BeforeAndAfterAll {

  this: Suite =>

  override def afterAll(): Unit = {
    close()
  }

  implicit override val webDriver: WebDriver = {
    val original: WebDriver = new ChromeDriver(chromeOptions())
    new EventFiringDecorator(new Listener(original)).decorate(original)
  }

  private def chromeOptions(): ChromeOptions = {
    val options: ChromeOptions = new ChromeOptions
    options.setBrowserVersion("135")
    options.addArguments("--disable-features=MediaRouter")
    options.setAcceptInsecureCerts(true)
    options
  }

  class Listener(driver: WebDriver) extends WebDriverListener {
    val testsCollector: TestsCollector = TestsCollector.testsCollector
    override def beforeAnyCall(target: AnyRef, method: Method, args: Array[AnyRef]): Unit = {
      //      val driver = target.asInstanceOf[ChromeDriver]
      //      println(s"driver $driver")
//      println(s"beforeAnyCall ${target.getClass}")
//      println(s"method $method")

      if (target.isInstanceOf[WebElement]) {
        // (target.asInstanceOf[RemoteWebElement]).getTagName
//        val element = target.asInstanceOf[RemoteWebElement]
//        println(s"(target.asInstanceOf[RemoteWebElement]).getTagName ${element.getTagName}")
//        println(s"type ${element.getAttribute("type")}")
//        println(s"value ${element.getAttribute("value")}")
//        println(s"text ${element.getText}")
      }
      if (method.toString.endsWith("org.openqa.selenium.WebElement.click()")) {
        testsCollector
          .addScreenshot(
            ScreenshotDriverData(
              driver.asInstanceOf[TakesScreenshot].getScreenshotAs(OutputType.FILE),
              driver.getPageSource,
              ScreenshotExternalData(
                driver.getCurrentUrl,
                driver.getTitle,
                ON_ENTER_PAGE
              )
            )
          )
      }
    }

    override def afterAnyCall(target: AnyRef, method: Method, args: Array[AnyRef], result: AnyRef): Unit = {
//      println(s"afterAnyCall ${target.getClass} ")
      if (method.toString.endsWith("org.openqa.selenium.WebElement.click()")) {
        testsCollector
          .addScreenshot(
            ScreenshotDriverData(
              driver.asInstanceOf[TakesScreenshot].getScreenshotAs(OutputType.FILE),
              driver.getPageSource,
              ScreenshotExternalData(
                driver.getCurrentUrl,
                driver.getTitle,
                ON_EXIT_PAGE
              )
            )
          )
      }
    }
  }

}
