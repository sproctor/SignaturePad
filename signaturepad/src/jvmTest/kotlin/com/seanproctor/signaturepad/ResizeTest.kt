package com.seanproctor.signaturepad

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import kotlin.test.Test
import kotlin.test.assertEquals

/** Resizing must not lose, shrink, or move the signature when the pad goes back to an earlier size. */
class ResizeTest {

    private fun SignaturePadStateImpl.drawStroke(vararg xy: Float) {
        gestureStarted(Offset(xy[0], xy[1]))
        for (i in 2 until xy.size step 2) gestureMoved(Offset(xy[i], xy[i + 1]))
    }

    private fun SignaturePadStateImpl.ink(): List<Offset> =
        RecordingCanvas().also { drawSignature(it, Color.Black, 3f) }.allPoints

    private fun assertSameInk(expected: List<Offset>, actual: List<Offset>) {
        assertEquals(expected.size, actual.size)
        expected.zip(actual).forEach { (e, a) ->
            assertEquals(e.x, a.x, 0.01f)
            assertEquals(e.y, a.y, 0.01f)
        }
    }

    @Test
    fun fit_rotatingBackAndForth_keepsTheSignature() {
        val state = SignaturePadStateImpl(ResizeBehavior.Fit)
        state.setSize(1080, 1800)
        state.drawStroke(100f, 900f, 300f, 950f, 500f, 850f, 700f, 950f, 900f, 900f)
        val original = state.ink()

        repeat(3) {
            state.setSize(1800, 1080)
            state.setSize(1080, 1800)
        }

        assertSameInk(original, state.ink())
    }

    @Test
    fun fit_narrowingAndWidening_keepsTheSignature() {
        val state = SignaturePadStateImpl(ResizeBehavior.Fit)
        state.setSize(1000, 500)
        state.drawStroke(100f, 250f, 300f, 300f, 500f, 200f, 700f, 300f, 900f, 250f)
        val original = state.ink()

        state.setSize(500, 500)
        state.setSize(1000, 500)

        assertSameInk(original, state.ink())
    }

    @Test
    fun zeroSize_keepsTheSignature() {
        val behaviors = listOf(
            ResizeBehavior.Clear,
            ResizeBehavior.Center,
            ResizeBehavior.Fit,
            ResizeBehavior.Stretch,
        )
        for (behavior in behaviors) {
            val state = SignaturePadStateImpl(behavior)
            state.setSize(100, 100)
            state.drawStroke(10f, 10f, 30f, 40f, 50f, 20f, 70f, 60f, 90f, 30f)
            val original = state.ink()

            state.setSize(0, 0)
            state.setSize(100, 100)

            assertSameInk(original, state.ink())
        }
    }

    @Test
    fun drawOnBitmap_whileZeroSized_draws() {
        val state = SignaturePadStateImpl(ResizeBehavior.Fit)
        state.setSize(100, 100)
        state.drawStroke(10f, 10f, 30f, 40f, 50f, 20f, 70f, 60f, 90f, 30f)
        state.setSize(0, 0)

        // Used to throw "Cannot round NaN value." after scaling by width 0.
        state.drawOnBitmap(ImageBitmap(60, 40), Color.Black, 2f)
    }

    @Test
    fun restoreAfterRotation_rotatingBack_keepsTheSignature() {
        val state = SignaturePadStateImpl(ResizeBehavior.Fit)
        state.setSize(1080, 1800)
        state.drawStroke(100f, 900f, 300f, 950f, 500f, 850f, 700f, 950f, 900f, 900f)
        val original = state.ink()
        state.setSize(1800, 1080)

        // The activity is recreated in landscape, then the user rotates back.
        val restored = SignaturePadStateImpl(ResizeBehavior.Fit)
        restored.restoreFromFloatList(state.toFloatList())
        restored.setSize(1800, 1080)
        restored.setSize(1080, 1800)

        assertSameInk(original, restored.ink())
    }

    @Test
    fun strokeDrawnAfterAResize_survivesTheNextResize() {
        val state = SignaturePadStateImpl(ResizeBehavior.Fit)
        state.setSize(100, 100)
        state.drawStroke(10f, 10f, 30f, 40f, 50f, 20f, 70f, 60f, 90f, 30f)
        state.setSize(200, 100)
        state.drawStroke(150f, 50f, 160f, 70f, 170f, 40f, 180f, 60f, 190f, 50f)
        val curves = RecordingCanvas().also { state.drawSignature(it, Color.Black, 3f) }.drawnStrokes.size

        state.setSize(100, 100)

        assertEquals(curves, RecordingCanvas().also { state.drawSignature(it, Color.Black, 3f) }.drawnStrokes.size)
    }
}
