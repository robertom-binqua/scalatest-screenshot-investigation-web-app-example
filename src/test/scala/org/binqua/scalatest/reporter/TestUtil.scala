package org.binqua.scalatest.reporter

import java.io.File
import java.nio.file.{Files, Path}

object TestUtil {
  def toString(prefix: Path, suffix: String): String =
    prefix.toFile.getAbsolutePath + File.separator + suffix.replaceAll("/", File.separator)

  object Assertions {

    def pathExist(pathThatShouldExist: Path): Unit =
      assert(Files.exists(pathThatShouldExist), s"file ${pathThatShouldExist.toAbsolutePath.toString} should exist")

  }
}
