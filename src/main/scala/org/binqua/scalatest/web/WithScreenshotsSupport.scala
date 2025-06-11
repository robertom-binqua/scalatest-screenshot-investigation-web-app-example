package org.binqua.scalatest.web

import org.scalatest.featurespec.AnyFeatureSpecLike

import java.util.concurrent.atomic.AtomicInteger

trait WithScreenshotsSupport {

  this: AnyFeatureSpecLike =>

  private val counter: AtomicInteger = new AtomicInteger(0)

  def takeAScreenshot(toBeWrapped: => Unit): Unit = {
    note(s"take screenshot now ${counter.incrementAndGet()}")
    note(s"take screenshot now ${counter.incrementAndGet()}")
    toBeWrapped
  }
}

