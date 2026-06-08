package com.seanproctor.signaturepad

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Paint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BezierTest {

    private fun Bezier.record(): RecordingCanvas =
        RecordingCanvas().also { draw(it, Paint()) }

    @Test
    fun draw_startsAtStartPointAndEndsAtEndPoint() {
        val start = Offset(0f, 0f)
        val end = Offset(40f, 0f)
        val bezier = Bezier(
            startPoint = start,
            endPoint = end,
            prevPoint = Offset(-10f, 10f),
            nextPoint = Offset(50f, 10f),
        )

        val points = bezier.record().allPoints
        assertTrue(points.isNotEmpty(), "a curve between distinct points should draw something")
        assertApproxEquals(start, points.first())
        assertApproxEquals(end, points.last())
    }

    @Test
    fun draw_withCoincidentPoints_staysAtThatPointWithoutNaN() {
        // All four control points identical exercises the divide-by-zero path in the control-point
        // math; the whenNaN guard must keep the drawn coordinates finite and on the point.
        val p = Offset(5f, 5f)
        val bezier = Bezier(startPoint = p, endPoint = p, prevPoint = p, nextPoint = p)

        val points = bezier.record().allPoints
        points.forEach { assertApproxEquals(p, it) }
    }

    @Test
    fun scale_scalesTheCurveEndpoints() {
        val bezier = Bezier(
            startPoint = Offset(2f, 4f),
            endPoint = Offset(20f, 8f),
            prevPoint = Offset(0f, 0f),
            nextPoint = Offset(30f, 10f),
        )

        val scaled = bezier.scale(2f)
        val points = scaled.record().allPoints

        assertTrue(points.isNotEmpty())
        assertApproxEquals(Offset(4f, 8f), points.first())
        assertApproxEquals(Offset(40f, 16f), points.last())
    }

    private fun assertApproxEquals(expected: Offset, actual: Offset, tolerance: Float = 0.01f) {
        assertEquals(expected.x, actual.x, tolerance)
        assertEquals(expected.y, actual.y, tolerance)
    }
}
