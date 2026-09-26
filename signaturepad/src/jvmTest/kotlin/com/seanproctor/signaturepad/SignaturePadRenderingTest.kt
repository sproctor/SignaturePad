package com.seanproctor.signaturepad

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [SignaturePad] draws the finished strokes from a cache and the stroke in progress on top. These
 * tests render the pad itself, so they cover how the two parts are put together on screen.
 */
class SignaturePadRenderingTest {

    // Renders a pad with one finished stroke across it at y = 100, and a stroke in progress down it
    // at x = 100, so the two cross in the middle.
    private fun renderCrossingStrokes(penColor: Color): PixelMap {
        val state = SignaturePadStateImpl()
        val scene = ImageComposeScene(200, 200, Density(1f)) {
            SignaturePad(state, penColor, 3.dp, Modifier.fillMaxSize())
        }
        try {
            // Lays the pad out, so the strokes have a size to be drawn in.
            scene.render()
            state.gestureStarted(Offset(20f, 100f))
            listOf(60f, 100f, 140f, 180f).forEach { state.gestureMoved(Offset(it, 100f)) }
            state.gestureEnded()
            state.gestureStarted(Offset(100f, 20f))
            listOf(60f, 100f, 140f, 180f).forEach { state.gestureMoved(Offset(100f, it)) }
            return scene.render().toComposeImageBitmap().toPixelMap()
        } finally {
            scene.close()
        }
    }

    @Test
    fun finishedStrokesAndTheStrokeInProgress_areBothDrawn() {
        val pixels = renderCrossingStrokes(Color.Black)

        assertTrue(pixels[60, 100].alpha > 0.9f, "the finished stroke isn't drawn")
        assertTrue(pixels[100, 60].alpha > 0.9f, "the stroke in progress isn't drawn")
    }

    @Test
    fun translucentPen_blendsWhereStrokesCrossOnce() {
        val pixels = renderCrossingStrokes(Color.Black.copy(alpha = 0.5f))

        assertEquals(0.5f, pixels[60, 100].alpha, 0.05f, "the finished stroke's alpha")
        assertEquals(0.5f, pixels[100, 60].alpha, 0.05f, "the stroke in progress's alpha")
        assertEquals(0.5f, pixels[100, 100].alpha, 0.05f, "the crossing was blended twice")
    }
}
