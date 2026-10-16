package com.seanproctor.signaturepad

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Exporting a [Signature] draws it with Skia, so these checks read the pixels. */
class SignatureExportTest {

    @Test
    fun toImageBitmap_drawsTheSignatureAtThePadSize() {
        val state = SignaturePadStateImpl().apply { setSize(200, 100) }
        state.gestureStarted(Offset(20f, 50f))
        listOf(60f, 100f, 140f, 180f).forEach { state.gestureMoved(Offset(it, 50f)) }
        state.gestureEnded()

        val bitmap = assertNotNull(state.signature.toImageBitmap(Color.Black, penWidth = 4f))

        assertEquals(200 to 100, bitmap.width to bitmap.height)
        val pixels = bitmap.toPixelMap()
        assertTrue(pixels[100, 50].alpha > 0.9f, "the stroke isn't drawn")
        assertEquals(0f, pixels[100, 10].alpha, "ink away from the stroke")
    }

    @Test
    fun toImageBitmap_afterAResize_hasTheNewPadSize() {
        val state = SignaturePadStateImpl(ResizeBehavior.Fit).apply { setSize(200, 100) }
        state.gestureStarted(Offset(20f, 50f))
        listOf(60f, 100f, 140f, 180f).forEach { state.gestureMoved(Offset(it, 50f)) }
        state.gestureEnded()

        state.setSize(400, 200)

        val bitmap = assertNotNull(state.signature.toImageBitmap(Color.Black, penWidth = 4f))
        assertEquals(400 to 200, bitmap.width to bitmap.height)
    }
}
