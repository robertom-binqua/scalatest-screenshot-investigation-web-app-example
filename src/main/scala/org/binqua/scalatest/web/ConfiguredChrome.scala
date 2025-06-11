package org.binqua.scalatest.web

import org.binqua.scalatest.reporter.ScreenshotMoment.{ON_ENTER_PAGE, ON_EXIT_PAGE}
import org.binqua.scalatest.reporter.{ScreenshotDriverData, ScreenshotExternalData, TestsCollector, WebDriverTestsCollector}
import org.openqa.selenium._
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.openqa.selenium.remote.RemoteWebElement
import org.openqa.selenium.support.events.{EventFiringDecorator, WebDriverListener}
import org.scalatest.{BeforeAndAfterAll, Suite}
import org.scalatestplus.selenium.{Driver, WebBrowser}

import java.lang.reflect.Method
import java.util
import java.util.Base64
import scala.collection.mutable
import scala.jdk.CollectionConverters.MapHasAsScala

trait ConfiguredChrome extends WebBrowser with Driver with BeforeAndAfterAll {

  this: Suite =>

  override def afterAll(): Unit = close()

  implicit override val webDriver: WebDriver = {
    val testsCollector: WebDriverTestsCollector = TestsCollector.webDriverTestsCollector
    val original: ChromeDriver = new ChromeDriver(chromeOptions())
    new EventFiringDecorator(new Listener(original, testsCollector)).decorate(original)
  }

  private def chromeOptions(): ChromeOptions = {
    val options: ChromeOptions = new ChromeOptions
    options.setBrowserVersion("137")
    options.setAcceptInsecureCerts(true)
    options
  }

  private class Listener(driver: ChromeDriver, testsCollector: WebDriverTestsCollector) extends WebDriverListener {
    override def beforeAnyCall(target: AnyRef, method: Method, args: Array[AnyRef]): Unit = {

      if (target.isInstanceOf[WebElement]) {
        (target.asInstanceOf[RemoteWebElement]).getTagName
        val element = target.asInstanceOf[RemoteWebElement]
        println(s"(target.asInstanceOf[RemoteWebElement]).getTagName ${element.getTagName}")
        println(s"type ${element.getAttribute("type")}")
        println(s"value ${element.getAttribute("value")}")
        println(s"text ${element.getText}")
      }

      if (method.toString.endsWith("org.openqa.selenium.WebElement.click()")) {

        testsCollector
          .addScreenshot(
            ScreenshotDriverData(
              takeScreenshot,
              driver.getPageSource,
              ScreenshotExternalData(
                pageUrl = driver.getCurrentUrl,
                pageTitle = driver.getTitle,
                screenshotMoment = ON_ENTER_PAGE
              )
            )
          )
      }
    }

    override def afterAnyCall(target: AnyRef, method: Method, args: Array[AnyRef], result: AnyRef): Unit = {
      if (method.toString.endsWith("org.openqa.selenium.WebElement.click()")) {
        testsCollector
          .addScreenshot(
            ScreenshotDriverData(
              takeScreenshot,
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

    private def takeScreenshot: Array[Byte] = {
      val params: util.HashMap[String, Object] = new util.HashMap[String, Object]()
      params.put("format", "jpeg")
      params.put("quality", Integer.valueOf(70))
      params.put("captureBeyondViewport", java.lang.Boolean.TRUE)

      val result: mutable.Map[String, AnyRef] = driver.executeCdpCommand("Page.captureScreenshot", params).asScala

      Base64.getDecoder.decode(result("data").asInstanceOf[String])
    }
  }

}
