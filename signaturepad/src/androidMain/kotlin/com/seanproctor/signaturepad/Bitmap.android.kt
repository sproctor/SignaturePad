package com.seanproctor.signaturepad

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import korlibs.image.bitmap.Bitmap
import korlibs.image.format.toAndroidBitmap

internal actual fun Bitmap.toComposeImageBitmap(): ImageBitmap {
    return toAndroidBitmap().asImageBitmap()
}