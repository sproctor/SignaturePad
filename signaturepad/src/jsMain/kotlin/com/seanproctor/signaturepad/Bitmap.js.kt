package com.seanproctor.signaturepad

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32

//internal actual fun Bitmap.toComposeImageBitmap(): ImageBitmap {
//    return KorimBackedImageBitmap(toBMP32())
//}
//
//private class KorimBackedImageBitmap(val bitmap: Bitmap32) : ImageBitmap {
//    override val colorSpace: ColorSpace = ColorSpaces.Srgb
//    override val config = ImageBitmapConfig.Argb8888
//    override val hasAlpha = bitmap.premultiplied
//    override val height get() = bitmap.height
//    override val width get() = bitmap.width
//    override fun prepareToDraw() = Unit
//
//    override fun readPixels(
//        buffer: IntArray,
//        startX: Int,
//        startY: Int,
//        width: Int,
//        height: Int,
//        bufferOffset: Int,
//        stride: Int
//    ) {
//        // similar to https://cs.android.com/android/platform/superproject/+/42c50042d1f05d92ecc57baebe3326a57aeecf77:frameworks/base/graphics/java/android/graphics/Bitmap.java;l=2007
//        val lastScanline: Int = bufferOffset + (height - 1) * stride
//        require(startX >= 0 && startY >= 0)
//        require(width > 0 && startX + width <= this.width)
//        require(height > 0 && startY + height <= this.height)
//        require(stride >= width)
//        require(bufferOffset >= 0 && bufferOffset + width <= buffer.size)
//        require(lastScanline >= 0 && lastScanline + width <= buffer.size)
//
//        for (y0 in 0 until height) {
//            for (x0 in 0 until width) {
//                val pos = bufferOffset + y0 * stride + x0
//                buffer[pos] = bitmap.getRgbaRaw(x0 + startX, y0 + startY).value
//            }
//        }
//    }
//}
