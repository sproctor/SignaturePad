package com.seanproctor.signaturepad

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Paint
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Validates the curve math in [Bezier] against an independent reference.
 *
 * [Bezier.draw] samples the curve uniformly in `t` from 0 to 1 (`t = i / drawSteps`), so for a
 * stroke of `n` drawn points the i-th point is the curve evaluated at `t = i / (n - 1)`. That lets
 * us re-derive every expected coordinate from the textbook cubic-bezier formula without depending
 * on the library's internal control points or arc-length sampling.
 */
class BezierMathTest {

    private val tolerance = 0.05f

    private fun drawnPoints(bezier: Bezier): List<Offset> =
        RecordingCanvas().also { bezier.draw(it, Paint()) }.allPoints

    // --- Independent reference implementations (derived from the math, not the production code) ---

    /** Textbook cubic bezier: B(t) = (1-t)³P0 + 3(1-t)²t·C1 + 3(1-t)t²·C2 + t³P3. */
    private fun cubic(p0: Offset, c1: Offset, c2: Offset, p3: Offset, t: Float): Offset {
        val u = 1f - t
        val b0 = u * u * u
        val b1 = 3f * u * u * t
        val b2 = 3f * u * t * t
        val b3 = t * t * t
        return Offset(
            b0 * p0.x + b1 * c1.x + b2 * c2.x + b3 * p3.x,
            b0 * p0.y + b1 * c1.y + b2 * c2.y + b3 * p3.y,
        )
    }

    /** Reference for the scaled-midpoint control-point smoothing that [Bezier] applies. */
    private fun controlPoints(p1: Offset, p2: Offset, p3: Offset): Pair<Offset, Offset> {
        val m1 = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
        val m2 = Offset((p2.x + p3.x) / 2f, (p2.y + p3.y) / 2f)
        val l1 = sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y))
        val l2 = sqrt((p2.x - p3.x) * (p2.x - p3.x) + (p2.y - p3.y) * (p2.y - p3.y))
        val k = if (l1 + l2 == 0f) 0f else l2 / (l1 + l2)
        val cm = Offset(m2.x + (m1.x - m2.x) * k, m2.y + (m1.y - m2.y) * k)
        val tx = p2.x - cm.x
        val ty = p2.y - cm.y
        return Offset(m1.x + tx, m1.y + ty) to Offset(m2.x + tx, m2.y + ty)
    }

    private fun expectedControls(
        start: Offset,
        end: Offset,
        prev: Offset,
        next: Offset,
    ): Pair<Offset, Offset> {
        val c1 = controlPoints(prev, start, end).second
        val c2 = controlPoints(start, end, next).first
        return c1 to c2
    }

    // --- Tests ---

    @Test
    fun everyDrawnSample_matchesTheClosedFormCubic() {
        // A spread of curved (non-collinear) inputs.
        val cases = listOf(
            arrayOf(Offset(0f, 0f), Offset(40f, 0f), Offset(-10f, 20f), Offset(50f, 20f)),
            arrayOf(Offset(10f, 10f), Offset(30f, 40f), Offset(0f, 0f), Offset(50f, 30f)),
            arrayOf(Offset(100f, 200f), Offset(150f, 180f), Offset(80f, 100f), Offset(200f, 160f)),
        )

        for (case in cases) {
            val (start, end, prev, next) = case
            val (c1, c2) = expectedControls(start, end, prev, next)
            val points = drawnPoints(Bezier(start, end, prev, next))

            assertTrue(points.size >= 2, "expected a sampled curve for $start -> $end")
            val last = points.size - 1
            points.forEachIndexed { i, actual ->
                val t = i.toFloat() / last
                val expected = cubic(start, c1, c2, end, t)
                assertApproxEquals(expected, actual, "case $start->$end at t=$t (i=$i)")
            }
        }
    }

    @Test
    fun collinearEvenlySpacedPoints_produceAStraightLine() {
        // Hand-derived: for prev/start/end/next at (0,0),(10,0),(20,0),(30,0) the smoothing yields
        // control points (15,0) and (15,0), so the curve is the straight segment y=0, x: 10 -> 20.
        val points = drawnPoints(
            Bezier(
                startPoint = Offset(10f, 0f),
                endPoint = Offset(20f, 0f),
                prevPoint = Offset(0f, 0f),
                nextPoint = Offset(30f, 0f),
            ),
        )

        assertApproxEquals(Offset(10f, 0f), points.first(), "start")
        assertApproxEquals(Offset(20f, 0f), points.last(), "end")
        points.forEach { assertEquals(0f, it.y, tolerance, "y must stay on the line, was $it") }
        for (i in 1 until points.size) {
            assertTrue(
                points[i].x >= points[i - 1].x - tolerance,
                "x must advance monotonically: ${points[i - 1].x} -> ${points[i].x}",
            )
        }
    }

    @Test
    fun midpointSample_matchesClosedFormForCollinearCurve() {
        // With control points (15,0)/(15,0): B(0.5) = (P0 + 3·C1 + 3·C2 + P3)/8
        //   x = (10 + 3·15 + 3·15 + 20)/8 = 120/8 = 15, y = 0. Independently arithmetic.
        val points = drawnPoints(
            Bezier(
                startPoint = Offset(10f, 0f),
                endPoint = Offset(20f, 0f),
                prevPoint = Offset(0f, 0f),
                nextPoint = Offset(30f, 0f),
            ),
        )
        // The straight segment is symmetric, so the geometric midpoint of the samples is B(0.5).
        val mid = points[points.size / 2]
        // Only exact when an even number of steps lands a sample on t=0.5; otherwise assert the
        // closed-form value lies between the two central samples.
        val lo = points[(points.size - 1) / 2]
        val hi = points[points.size / 2]
        assertTrue(
            lo.x - tolerance <= 15f && 15f <= hi.x + tolerance,
            "B(0.5).x should be 15, bracketed by ${lo.x}..${hi.x}",
        )
        assertEquals(0f, mid.y, tolerance)
    }

    @Test
    fun allSamplesLieWithinControlPointBoundingBox() {
        // A fundamental property of any bezier: the curve stays within the convex hull (hence the
        // bounding box) of its control points. Independent of the exact evaluation formula.
        val start = Offset(5f, 5f)
        val end = Offset(45f, 15f)
        val prev = Offset(-5f, 30f)
        val next = Offset(60f, -10f)
        val (c1, c2) = expectedControls(start, end, prev, next)

        val xs = listOf(start.x, c1.x, c2.x, end.x)
        val ys = listOf(start.y, c1.y, c2.y, end.y)
        val minX = xs.min(); val maxX = xs.max()
        val minY = ys.min(); val maxY = ys.max()

        for (p in drawnPoints(Bezier(start, end, prev, next))) {
            assertTrue(p.x in (minX - tolerance)..(maxX + tolerance), "x ${p.x} outside [$minX,$maxX]")
            assertTrue(p.y in (minY - tolerance)..(maxY + tolerance), "y ${p.y} outside [$minY,$maxY]")
        }
    }

    private fun assertApproxEquals(expected: Offset, actual: Offset, message: String = "") {
        assertEquals(expected.x, actual.x, tolerance, "x mismatch: $message")
        assertEquals(expected.y, actual.y, tolerance, "y mismatch: $message")
    }
}
