package org.binqua.examples.http4sapp.selenium.http4sapp.model

case class NavItem(title: String, body: String, isActive: Boolean, pageUrl: String, navTitle: String) {

  def isActiveString(): String = if (isActive) "active disabled" else ""

}

object NavItem {

  def home(active: Boolean): NavItem = NavItem(
    title = "Home",
    body = "this is the home body. Just for fun",
    isActive = active,
    pageUrl = "home.html",
    navTitle = "Home"
  )

  def page2(active: Boolean): NavItem = NavItem(
    title = "Page 2",
    body = "this is the body of page 2. Just for fun",
    isActive = active,
    pageUrl = "page2.html",
    navTitle = "Page2"
  )

  def page3(active: Boolean): NavItem = NavItem(
    title = "Page 3",
    body = "this is the body of page 3. Just for fun",
    isActive = active,
    pageUrl = "page3.html",
    navTitle = "Page3"
  )

  def page1(active: Boolean): NavItem = NavItem(
    title = "Page 1",
    body = "this is the body of page 1. Just for fun",
    isActive = active,
    pageUrl = "page1.html",
    navTitle = "Page1"
  )

}
