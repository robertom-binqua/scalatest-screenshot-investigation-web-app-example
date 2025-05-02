package org.binqua.scalatest.integration.model

case class NavigationMenu(items: List[NavItem]) {

  private val theActiveOne: NavItem = items.filter(_.isActive).head

  val title: String = theActiveOne.title

  val body: String = theActiveOne.body

}

object NavigationMenu {
  def from(pageIdentifier: String): Either[String, NavigationMenu] = {
    if (List("home", "page1", "page2", "page3").contains(pageIdentifier))
      Right(
        NavigationMenu(
          List(
            NavItem(
              title = "Home",
              body = "this is the home body. Just for fun",
              isActive = "home".equals(pageIdentifier),
              pageUrl = "home.html",
              navTitle = "Home"
            ),
            NavItem(
              title = "Page 1",
              body = "this is the body of page 1. Just for fun",
              isActive = "page1".equals(pageIdentifier),
              pageUrl = "page1.html",
              navTitle = "Page1"
            ),
            NavItem(
              title = "Page 2",
              body = "this is the body of page 2. Just for fun",
              isActive = "page2".equals(pageIdentifier),
              pageUrl = "page2.html",
              navTitle = "Page2"
            ),
            NavItem(
              title = "Page 3",
              body = "this is the body of page 3. Just for fun",
              isActive = "page3".equals(pageIdentifier),
              pageUrl = "page3.html",
              navTitle = "Page3"
            )
          )
        )
      )
    else Left("no page crappy.html exist. Sorry!")
  }
}
