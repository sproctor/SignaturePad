package com.seanproctor.signaturepad

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PointMode
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal class Bezier(
    private val startPoint: Offset,
    private val endPoint: Offset,
    private val prevPoint: Offset,
    private val nextPoint: Offset,
) {
    private val control1 = calculateControlPoints(prevPoint, startPoint, endPoint).second
    private val control2 = calculateControlPoints(startPoint, endPoint, nextPoint).first
    private val drawSteps = ceil(length()).roundToInt()

    fun draw(canvas: Canvas, paint: Paint) {
        val points = mutableListOf<Offset>()
        for (i in 0..drawSteps) {
            val t = i.toFloat() / drawSteps
            val tt = t * t
            val ttt = tt * t
            val u = 1 - t
            val uu = u * u
            val uuu = uu * u

            val x = uuu * startPoint.x +
                    3 * uu * t * control1.x +
                    3 * u * tt * control2.x +
                    ttt * endPoint.x
            val y = uuu * startPoint.y +
                    3 * uu * t * control1.y +
                    3 * u * tt * control2.y +
                    ttt * endPoint.y

            points.add(Offset(x, y))
        }
        try {
            canvas.drawPoints(points = points, pointMode = PointMode.Points, paint = paint)
        } catch (e: Throwable) {
            // Ignore drawing exceptions
            // I think they happen when resetting canvas while we're drawing
        }
    }

    private fun length(): Float {
        val steps = 10
        var length = 0f
        var px = 0f
        var py = 0f
        repeat(steps) { i ->
            val t = i.toFloat() / steps
            val cx = point(
                t, startPoint.x, control1.x,
                control2.x, endPoint.x
            )
            val cy = point(
                t, startPoint.y, control1.y,
                control2.y, endPoint.y
            )
            if (i > 0) {
                val xDiff = cx - px
                val yDiff = cy - py
                length += sqrt(xDiff * xDiff + yDiff * yDiff)
            }
            px = cx
            py = cy
        }
        return length
    }

    private fun point(t: Float, start: Float, c1: Float, c2: Float, end: Float): Float {
        return (
                start * (1.0f - t) * (1.0f - t) * (1.0f - t)
                        + 3.0f * c1 * (1.0f - t) * (1.0f - t) * t
                        + 3.0f * c2 * (1.0f - t) * t * t
                        + end * t * t * t
                )
    }

    private fun calculateControlPoints(
        p1: Offset,
        p2: Offset,
        p3: Offset,
    ): Pair<Offset, Offset> {
        // http://scaledinnovation.com/analytics/splines/aboutSplines.html
//        val d1 = p1.distanceTo(p2)
//        val d2 = p2.distanceTo(p3)
//        val t = 0.4f
//        val fa = (t * d1 / (d1 + d2)).whenNaN { 0f }
//        val fb = (t * d2 / (d1 + d2)).whenNaN { 0f }
//        val cp1x = p2.x - fa * (p3.x - p1.x)
//        val cp1y = p2.y - fa * (p3.y - p1.y)
//        val cp2x = p2.x + fb * (p3.x - p1.x)
//        val cp2y = p2.y + fb * (p3.y - p1.y)
//        val cp1 = Point(cp1x, cp1y)
//        val cp2 = Point(cp2x, cp2y)
//        println("p1: (${p1.x}, ${p1.y}), p2: (${p2.x}, ${p2.y}), p3: (${p3.x}, ${p3.y})")
//        println("d1: $d1, d2: $d2, fa: $fa, fb: $fb")
//        println("cp1: ($cp1x, $cp1y), cp3: ($cp2x, $cp2y)")
//        return Pair(cp1, cp2)
        // Unknown origin - from original signature pad
        val dx1 = p1.x - p2.x
        val dy1 = p1.y - p2.y
        val dx2 = p2.x - p3.x
        val dy2 = p2.y - p3.y

        val m1x = (p1.x + p2.x) / 2f
        val m1y = (p1.y + p2.y) / 2f
        val m2x = (p2.x + p3.x) / 2f
        val m2y = (p2.y + p3.y) / 2f

        val l1 = sqrt(dx1 * dx1 + dy1 * dy1)
        val l2 = sqrt(dx2 * dx2 + dy2 * dy2)

        val dxm = m1x - m2x
        val dym = m1y - m2y
        val k = (l2 / (l1 + l2)).whenNaN { 0f }
        val cmx = m2x + dxm * k
        val cmy = m2y + dym * k

        val tx = p2.x - cmx
        val ty = p2.y - cmy

        return Pair(Offset(m1x + tx, m1y + ty), Offset(m2x + tx, m2y + ty))
    }

    fun scale(ratio: Float): Bezier {
        return Bezier(
            startPoint = startPoint * ratio,
            endPoint = endPoint * ratio,
            prevPoint = prevPoint * ratio,
            nextPoint = nextPoint * ratio,
        )
    }
}

private fun Float.whenNaN(then: () -> Float): Float =
    if (isNaN())
        then()
    else
        this