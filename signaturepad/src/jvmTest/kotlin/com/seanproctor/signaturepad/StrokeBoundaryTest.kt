package com.seanproctor.signaturepad

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Each stroke must be drawn as its own contour, so that it gets its own round end caps. That holds
 * even when a stroke starts exactly where the previous one ended, where the two would otherwise be
 * joined into one line.
 */
class StrokeBoundaryTest {

    private fun SignaturePadStateImpl.drawStroke(points: List<Offset>) {
        gestureStarted(points.first())
        points.drop(1).forEach { gestureMoved(it) }
        gestureEnded()
    }

    private fun SignaturePadStateImpl.contours(): Int =
        RecordingCanvas().also { drawSignature(it, Color.Black, 3f) }.contourCount

    // The second stroke starts at the point where the first one ends.
    private fun touchingStrokes(behavior: ResizeBehavior = ResizeBehavior.Fit) =
        SignaturePadStateImpl(behavior).apply {
            setSize(100, 100)
            drawStroke(listOf(Offset(10f, 10f), Offset(20f, 30f), Offset(30f, 20f), Offset(40f, 40f)))
            drawStroke(listOf(Offset(40f, 40f), Offset(60f, 30f), Offset(70f, 60f), Offset(90f, 50f)))
        }

    @Test
    fun strokeStartingWhereTheLastEnded_isASeparateContour() {
        assertEquals(2, touchingStrokes().contours())
    }

    @Test
    fun strokeStartingElsewhere_isASeparateContour() {
        val state = SignaturePadStateImpl().apply {
            setSize(100, 100)
            drawStroke(listOf(Offset(10f, 10f), Offset(20f, 30f), Offset(30f, 20f), Offset(40f, 40f)))
            drawStroke(listOf(Offset(50f, 50f), Offset(60f, 30f), Offset(70f, 60f), Offset(90f, 50f)))
        }
        assertEquals(2, state.contours())
    }

    @Test
    fun eachStroke_isOneContour() {
        val state = SignaturePadStateImpl().apply {
            setSize(100, 100)
            drawStroke(listOf(Offset(10f, 10f), Offset(20f, 30f), Offset(30f, 20f), Offset(40f, 40f), Offset(50f, 10f)))
        }
        assertEquals(1, state.contours())
    }

    @Test
    fun reenteringThePadWhereItWasLeft_startsASeparateContour() {
        val state = SignaturePadStateImpl().apply {
            setSize(100, 100)
            gestureStarted(Offset(40f, 50f))
            // Out through the left edge at (0, 50), then straight back in at the same point.
            listOf(Offset(30f, 60f), Offset(15f, 40f), Offset(0f, 50f), Offset(-5f, 50f))
                .forEach { gestureMoved(it) }
            listOf(Offset(0f, 50f), Offset(15f, 70f), Offset(30f, 80f), Offset(45f, 70f))
                .forEach { gestureMoved(it) }
            gestureEnded()
        }
        assertEquals(2, state.contours())
    }

    @Test
    fun strokeBoundaries_surviveResizing() {
        for (behavior in listOf(ResizeBehavior.Center, ResizeBehavior.Fit, ResizeBehavior.Stretch)) {
            val state = touchingStrokes(behavior)
            state.setSize(200, 150)
            assertEquals(2, state.contours(), "after resizing with $behavior")
        }
    }

    @Test
    fun strokeBoundaries_surviveSavingAndRestoring() {
        val restored = SignaturePadStateImpl(ResizeBehavior.Fit)
        restored.restoreFromFloatList(touchingStrokes().toFloatList())

        assertEquals(2, restored.contours())
    }

    @Test
    fun strokeBoundaries_surviveScaling() {
        // drawOnBitmap scales the curves before drawing them.
        val curves = listOf(
            Bezier(Offset(10f, 10f), Offset(40f, 40f), Offset(10f, 10f), Offset(40f, 40f), startsStroke = true),
            Bezier(Offset(40f, 40f), Offset(90f, 50f), Offset(40f, 40f), Offset(90f, 50f), startsStroke = true),
        ).map { it.scale(2f) }
        val canvas = RecordingCanvas()
        canvas.drawPath(pathOf(curves), Paint())

        assertEquals(2, canvas.contourCount)
    }
}
