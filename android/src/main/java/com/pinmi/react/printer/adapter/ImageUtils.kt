package com.pinmi.react.printer.adapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

object ImageUtils {

    private const val ESC_CHAR = 0x1B.toChar()
    private val SELECT_BIT_IMAGE_MODE = byteArrayOf(0x1B, 0x2A, 33)
    private val SET_LINE_SPACE_24 = byteArrayOf(ESC_CHAR.code.toByte(), 0x33, 24)
    private val SET_LINE_SPACE_32 = byteArrayOf(ESC_CHAR.code.toByte(), 0x33, 32)
    private val LINE_FEED = byteArrayOf(0x0A)
    private val CENTER_ALIGN = byteArrayOf(0x1B, 0X61, 0X31)

    fun getBitmapResized(image: Bitmap, decreaseSizeBy: Float, imageWidth: Int, imageHeight: Int): Bitmap {
        var imageWidthForResize = image.width
        var imageHeightForResize = image.height
        if (imageWidth > 0) {
            imageWidthForResize = imageWidth
        }
        if (imageHeight > 0) {
            imageHeightForResize = imageHeight
        }
        return Bitmap.createScaledBitmap(image, (imageWidthForResize * decreaseSizeBy).toInt(), (imageHeightForResize * decreaseSizeBy).toInt(), true)
    }

    fun getRGB(bmpOriginal: Bitmap, col: Int, row: Int): Int {
        // get one pixel color
        val pixel = bmpOriginal.getPixel(col, row)
        // retrieve color of all channels
        val R = Color.red(pixel)
        val G = Color.green(pixel)
        val B = Color.blue(pixel)
        return Color.rgb(R, G, B)
    }

    fun resizeTheImageForPrinting(image: Bitmap, imageWidth: Int?, imageHeight: Int?): Bitmap {
        // making logo size 150 or less pixels
        val width = image.width
        val height = image.height
        if (imageWidth != null || imageHeight != null) {
            return getBitmapResized(image, 1f, imageWidth!!, imageHeight!!)
        }
        if (width > 200 || height > 200) {
            val decreaseSizeBy: Float
            decreaseSizeBy = if (width > height) {
                200.0f / width
            } else {
                200.0f / height
            }
            return getBitmapResized(image, decreaseSizeBy, 0, 0)
        }
        return image
    }

    fun shouldPrintColor(col: Int): Boolean {
        val threshold = 127
        val a: Int
        val r: Int
        val g: Int
        val b: Int
        val luminance: Int
        a = col shr 24 and 0xff
        if (a != 0xff) { // Ignore transparencies
            return false
        }
        r = col shr 16 and 0xff
        g = col shr 8 and 0xff
        b = col and 0xff
        luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        return luminance < threshold
    }

    fun recollectSlice(y: Int, x: Int, img: Array<IntArray>): ByteArray {
        val slices = byteArrayOf(0, 0, 0)
        var yy = y
        var i = 0
        while (yy < y + 24 && i < 3) {
            var slice: Byte = 0
            for (b in 0..7) {
                val yyy = yy + b
                if (yyy >= img.size) {
                    continue
                }
                val col = img[yyy][x]
                val v = shouldPrintColor(col)
                slice = (slice.toInt() or ((if (v) 1 else 0) shl 7 - b).toByte().toInt()).toByte()
            }
            slices[i] = slice
            yy += 8
            i++
        }
        return slices
    }

    fun getPixelsSlow(image2: Bitmap, imageWidth: Int?, imageHeight: Int?): Array<IntArray> {
        val image = resizeTheImageForPrinting(image2, imageWidth, imageHeight)
        val width = image.width
        val height = image.height
        val result = Array(height) { IntArray(width) }
        for (row in 0 until height) {
            for (col in 0 until width) {
                result[row][col] = getRGB(image, col, row)
            }
        }
        return result
    }

    fun printImageFromStream(outputStream: OutputStream, imageUrl: String, imageWidth: Int?, imageHeight: Int?) {
        return printImageFromStream(outputStream, getBitmapFromURL(imageUrl), imageWidth, imageHeight)
    }

    fun printImageFromStream(outputStream: OutputStream, image: Bitmap, imageWidth: Int?, imageHeight: Int?) {
        val pixels = getPixelsSlow(image, imageWidth, imageHeight)
        outputStream.write(SET_LINE_SPACE_24)
        outputStream.write(CENTER_ALIGN)
        var y = 0
        while (y < pixels.size) {
            // Like I said before, when done sending data,
            // the printer will resume to normal text printing
            outputStream.write(SELECT_BIT_IMAGE_MODE)
            // Set nL and nH based on the width of the image
            outputStream.write(byteArrayOf((0x00ff and pixels[y].size).toByte(), (0xff00 and pixels[y].size shr 8).toByte()))
            for (x in pixels[y].indices) {
                // for each stripe, recollect 3 bytes (3 bytes = 24 bits)
                outputStream.write(recollectSlice(y, x, pixels))
            }

            // Do a line feed, if not the printing will resume on the same line
            outputStream.write(LINE_FEED)
            y += 24
        }
        outputStream.write(SET_LINE_SPACE_32)
        outputStream.write(LINE_FEED)
        outputStream.flush()
    }

    fun getBitmapFromURL(src: String): Bitmap {
        val url = URL(src)
        val connection = url.openConnection() as HttpURLConnection
        connection.doInput = true
        connection.connect()
        val input = connection.inputStream
        val myBitmap = BitmapFactory.decodeStream(input)
        val baos = ByteArrayOutputStream()
        myBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        return myBitmap
    }
}
