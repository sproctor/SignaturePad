package com.github.sproctor.signaturedemo

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.soywiz.korim.awt.AWT_INTERNAL_IMAGE_TYPE
import com.soywiz.korim.awt.toAwt
import com.soywiz.korim.bitmap.Bitmap
import java.awt.image.BufferedImage

internal actual fun Bitmap.asImageBitmap(): ImageBitmap {
    val bufferedImage = BufferedImage(width, height, AWT_INTERNAL_IMAGE_TYPE)
    return toAwt(bufferedImage).toComposeImageBitmap()
}