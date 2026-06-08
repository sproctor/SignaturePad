package com.seanproctor.signaturepad

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies what a [SignaturePadState] actually emits to a [androidx.compose.ui.graphics.Canvas].
 * Lives in jvmTest because it constructs a real Paint via [SignaturePadState.drawSignature].
 */
class SignaturePadDrawingTest {

    private fun drawTo(state: SignaturePadState): RecordingCanvas =
        RecordingCanvas().also { state.drawSignature(it, Color.Black, penWidth = 3f) }

    @Test
    fun drawSignature_withNoInput_drawsNothing() {
        val state = SignaturePadStateImpl()
        assertTrue(drawTo(state).drawnStrokes.isEmpty())
    }

    @Test
    fun clear_removesPreviouslyDrawnStrokes() {
        val state = SignaturePadStateImpl()
        state.setSize(100, 100) // points outside the bounds are ignored, so give the pad a size
        state.gestureStarted(Offset(0f, 0f))
        listOf(Offset(10f, 10f), Offset(20f, 0f), Offset(30f, 10f)).forEach { state.gestureMoved(it) }
        assertTrue(drawTo(state).drawnStrokes.isNotEmpty(), "precondition: signature has strokes")

        state.clear()

        assertTrue(drawTo(state).drawnStrokes.isEmpty(), "cleared signature should draw nothing")
    }

    @Test
    fun gesture_needsFourPointsBeforeFirstCurve() {
        val state = SignaturePadStateImpl()
        state.setSize(100, 100)
        // gestureStarted seeds two points; one move gives three — not enough for a cubic bezier.
        state.gestureStarted(Offset(0f, 0f))
        state.gestureMoved(Offset(10f, 10f))

        assertTrue(drawTo(state).drawnStrokes.isEmpty(), "a single move should not yet produce a curve")
    }

    @Test
    fun gesture_producesOneCurvePerMoveAfterTheFirst() {
        val state = SignaturePadStateImpl()
        state.setSize(100, 100)
        state.gestureStarted(Offset(0f, 0f))
        // Well-separated points so each curve has a non-zero length and is actually drawn.
        val moves = listOf(Offset(10f, 10f), Offset(20f, 0f), Offset(30f, 10f), Offset(40f, 0f))
        moves.forEach { state.gestureMoved(it) }

        // n moves -> n-1 cubic bezier segments.
        assertEquals(moves.size - 1, drawTo(state).drawnStrokes.size)
    }
}
