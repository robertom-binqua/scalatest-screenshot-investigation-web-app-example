package org.binqua.scalatest.reporter.util

import munit.FunSuite

class utilsSpec extends FunSuite {

  test("utils.clean removes empty lines and trims the remaining one") {
    val toBeCleaned =
      """
        |   a b c
        |
        |
        |   this is a piece of test
        |
        |
        |""".stripMargin

    val expected =
      """a b c
        |this is a piece of test""".stripMargin
    assertEquals(utils.clean(toBeCleaned), expected)
  }

}
