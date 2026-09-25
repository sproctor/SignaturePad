package com.seanproctor.signaturepad

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.sqrt

/**
 * One cubic segment of a stroke, running from [startPoint] to [endPoint]. [prevPoint] and [nextPoint]
 * are the neighboring input points, used to smooth the joins between segments. [startsStroke] marks
 * the first segment of a stroke. Drawing starts a new contour there and nowhere else, so separate
 * strokes stay separate even when one begins exactly where another ended.
 */
internal class Bezier(
    private val startPoint: Offset,
    private val endPoint: Offset,
    private val prevPoint: Offset,
    private val nextPoint: Offset,
    val startsStroke: Boolean,
) {
    private val control1 = calculateControlPoints(prevPoint, startPoint, endPoint).second
    private val control2 = calculateControlPoints(startPoint, endPoint, nextPoint).first

    /**
     * Adds this curve to [path]. A curve that starts a stroke starts a new contour. Any other curve
     * continues the current one, which ends where this curve starts.
     */
    fun addTo(path: Path) {
        if (startsStroke) path.moveTo(startPoint.x, startPoint.y)
        path.cubicTo(control1.x, control1.y, control2.x, control2.y, endPoint.x, endPoint.y)
    }

    private fun calculateControlPoints(
        p1: Offset,
        p2: Offset,
        p3: Offset,
    ): Pair<Offset, Offset> {
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

    /**
     * Returns a copy of this curve with [transform] applied to each of its source points. The
     * control points are recomputed from the transformed anchors, so any affine mapping produces a
     * correctly shaped curve.
     */
    fun map(transform: (Offset) -> Offset): Bezier {
        return Bezier(
            startPoint = transform(startPoint),
            endPoint = transform(endPoint),
            prevPoint = transform(prevPoint),
            nextPoint = transform(nextPoint),
            startsStroke = startsStroke,
        )
    }

    fun scale(ratio: Float): Bezier = map { it * ratio }

    /**
     * The four points this curve was built from, in the order the constructor accepts them
     * (start, end, prev, next). Used to serialize and rebuild the curve exactly.
     */
    fun sourcePoints(): List<Offset> = listOf(startPoint, endPoint, prevPoint, nextPoint)
}

/**
 * Joins [curves] into a single path, with one contour per stroke. The first curve must start a
 * stroke, since a path's first contour needs a starting point.
 */
internal fun pathOf(curves: List<Bezier>): Path {
    val path = Path()
    curves.forEach { it.addTo(path) }
    return path
}

private fun Float.whenNaN(then: () -> Float): Float =
    if (isNaN())
        then()
    else
        this