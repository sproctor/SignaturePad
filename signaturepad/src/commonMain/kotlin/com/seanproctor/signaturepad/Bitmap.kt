package com.seanproctor.signaturepad

import androidx.compose.ui.graphics.ImageBitmap
import com.soywiz.korim.bitmap.Bitmap

internal expect fun Bitmap.toComposeImageBitmap(): ImageBitmap