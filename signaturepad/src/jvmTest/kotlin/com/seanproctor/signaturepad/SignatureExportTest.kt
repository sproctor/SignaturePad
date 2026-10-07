package com.seanproctor.signaturepad

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Exporting a [Signature] draws it with Skia, so these checks read the pixels. */
class SignatureExportTest {

    @Test
    fun toImageBitmap_drawsTheSignatureAtThePadSize() {
        val state = SignaturePadStateImpl().apply { setSize(200, 100) }
        state.gestureStarted(Offset(20f, 50f))
        listOf(60f, 100f, 140f, 180f).forEach { state.gestureMoved(Offset(it, 50f)) }
        state.gestureEnded()

        val bitmap = state.signature.toImageBitmap(Color.Black, penWidth = 4f)

        assertEquals(200 to 100, bitmap.width to bitmap.height)
        val pixels = bitmap.toPixelMap()
        assertTrue(pixels[100, 50].alpha > 0.9f, "the stroke isn't drawn")
        assertEquals(0f, pixels[100, 10].alpha, "ink away from the stroke")
    }
}
