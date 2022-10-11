package com.github.sproctor.signaturedemo

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.format.toAndroidBitmap

internal actual fun Bitmap.asImageBitmap(): ImageBitmap {
    return toAndroidBitmap().asImageBitmap()
}