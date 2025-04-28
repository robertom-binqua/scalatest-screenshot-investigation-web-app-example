package org.binqua.scalatest.reporter

sealed trait ScreenshotMoment
object ScreenshotMoment {
  case object ON_ENTER_PAGE extends ScreenshotMoment
  case object ON_EXIT_PAGE extends ScreenshotMoment
}
