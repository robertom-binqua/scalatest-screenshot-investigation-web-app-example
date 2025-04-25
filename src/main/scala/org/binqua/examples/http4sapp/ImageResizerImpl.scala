package org.binqua.examples.http4sapp

import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

trait ImageResizer {
  def resizeImage(inputPath: File, outputPath: File, scale: Int): Unit
}

object ImageResizerImpl extends ImageResizer{
  def resizeImage(inputPath: File, outputPath: File,scale : Int): Unit = {
    // Read the original image
    val originalImage: BufferedImage = ImageIO.read(inputPath)
    val width = originalImage.getWidth
    val height = originalImage.getHeight

    // Scale the image to the new size
    val resizedImage = originalImage.getScaledInstance(width / scale, height / scale, Image.SCALE_SMOOTH)
    // Create a new buffered image with the desired dimensions
    val outputImage = new BufferedImage(width / scale, height / scale, BufferedImage.TYPE_INT_RGB)

    // Draw the resized image onto the new buffered image
    val g2d = outputImage.createGraphics()
    g2d.drawImage(resizedImage, 0, 0, null)
    g2d.dispose()

    // Write the resized image to output path
    ImageIO.write(outputImage, "png", outputPath)
  }

  def main(args: Array[String]): Unit = {
    def fullPath(image: String) = s"/Users/robertomalagigi/dev/learning/scalatest-screenshot-investigation-web-app-example/screenshots/AppSpec1_F1_S1/$image"

    val images = List(
      "SS8_onExit.png",
      "SS7_onEnter.png",
      "SS6_onExit.png",
      "SS5_onEnter.png",
      "SS4_onExit.png",
      "SS3_onEnter.png",
      "SS2_onExit.png",
      "SS1_onEnter.png"
    )

//    images.foreach(image => resizeImage(fullPath(image), s"resized_$image"))
  }
}
