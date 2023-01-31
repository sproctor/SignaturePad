package com.seanproctor.signaturepad

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorInfo
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

internal actual fun Bitmap.toComposeImageBitmap(): ImageBitmap {
    return toBMP32IfRequired().toSkiaBitmap().asComposeImageBitmap()
}

internal fun Bitmap32.toSkiaBitmap(): org.jetbrains.skia.Bitmap {
    val colorType = ColorType.N32
    val alphaType = if (premultiplied) ColorAlphaType.PREMUL else ColorAlphaType.OPAQUE
    val skiaColorSpace = null
    val colorInfo = ColorInfo(colorType, alphaType, skiaColorSpace)
    val imageInfo = ImageInfo(colorInfo, width, height)
    val bitmap = org.jetbrains.skia.Bitmap()
    val pixels = extractBytes()
    bitmap.allocPixels(imageInfo)
    bitmap.installPixels(pixels)
    return bitmap
}