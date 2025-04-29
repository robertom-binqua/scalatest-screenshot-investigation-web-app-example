package org.binqua.examples.http4sapp.app

import java.io.File
import java.nio.file.{Files, Path}

object TestUtil:
  def toString(prefix: Path, suffix: String): String =
    prefix.toFile.getAbsolutePath + File.separator + suffix.replaceAll("/", File.separator)

  def assertPathExist(pathThatShouldExist: Path): Unit =
    assert(Files.exists(pathThatShouldExist), s"file ${pathThatShouldExist.toAbsolutePath.toString} should exist")

