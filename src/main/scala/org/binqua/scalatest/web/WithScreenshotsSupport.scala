package org.binqua.scalatest.web

import org.scalatest.featurespec.AnyFeatureSpecLike

trait WithScreenshotsSupport {

  this: AnyFeatureSpecLike =>

  private var counter: Int = 0

  def takeAScreenshot(toBeWrapped: => Unit): Unit = {
    counter += 1
    note(s"take screenshot now ${counter}")
    counter += 1
    note(s"take screenshot now ${counter}")
    toBeWrapped
  }
}

