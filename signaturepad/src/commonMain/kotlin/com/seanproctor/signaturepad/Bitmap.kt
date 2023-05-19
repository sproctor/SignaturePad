package com.seanproctor.signaturepad

import androidx.compose.ui.graphics.ImageBitmap
import korlibs.image.bitmap.Bitmap

internal expect fun Bitmap.toComposeImageBitmap(): ImageBitmap