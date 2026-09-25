package com.seanproctor.signaturepad

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathSegment
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.Vertices
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * A [Canvas] that records every [drawPoints] and [drawPath] call instead of rendering, so tests can
 * assert on what a [SignaturePadState] emits without depending on a real rendering backend. All
 * other canvas operations are no-ops.
 */
class RecordingCanvas : Canvas {
    /**
     * One entry per [drawPoints] call, holding the points that were passed, and one per cubic curve
     * in a [drawPath] call, holding points sampled along it.
     */
    val drawnStrokes: MutableList<List<Offset>> = mutableListOf()

    /** Every recorded point, flattened. */
    val allPoints: List<Offset> get() = drawnStrokes.flatten()

    /** The number of contours in the paths passed to [drawPath]: one per `moveTo`. */
    var contourCount: Int = 0

    /** The paint passed to the most recent recorded call. */
    var lastPaint: Paint? = null

    override fun drawPoints(pointMode: PointMode, points: List<Offset>, paint: Paint) {
        drawnStrokes.add(points.toList())
        lastPaint = paint
    }

    override fun save() {}
    override fun restore() {}
    override fun saveLayer(bounds: Rect, paint: Paint) {}
    override fun translate(dx: Float, dy: Float) {}
    override fun scale(sx: Float, sy: Float) {}
    override fun rotate(degrees: Float) {}
    override fun skew(sx: Float, sy: Float) {}
    override fun concat(matrix: Matrix) {}
    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float, clipOp: ClipOp) {}
    override fun clipPath(path: Path, clipOp: ClipOp) {}
    override fun drawLine(p1: Offset, p2: Offset, paint: Paint) {}
    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {}
    override fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radiusX: Float,
        radiusY: Float,
        paint: Paint,
    ) {}

    override fun drawOval(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {}
    override fun drawCircle(center: Offset, radius: Float, paint: Paint) {}
    override fun drawArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        paint: Paint,
    ) {}

    override fun drawPath(path: Path, paint: Paint) {
        for (segment in path) {
            if (segment.type == PathSegment.Type.Move) contourCount++
            if (segment.type == PathSegment.Type.Cubic) {
                drawnStrokes.add(sampleCubic(segment.points))
            }
        }
        lastPaint = paint
    }

    // Samples the cubic curve with the given start, control and end points (as x, y pairs).
    private fun sampleCubic(p: FloatArray): List<Offset> = List(CURVE_SAMPLES + 1) { i ->
        val t = i.toFloat() / CURVE_SAMPLES
        val u = 1 - t
        val a = u * u * u
        val b = 3 * u * u * t
        val c = 3 * u * t * t
        val d = t * t * t
        Offset(
            a * p[0] + b * p[2] + c * p[4] + d * p[6],
            a * p[1] + b * p[3] + c * p[5] + d * p[7],
        )
    }
    override fun drawImage(image: ImageBitmap, topLeftOffset: Offset, paint: Paint) {}
    override fun drawImageRect(
        image: ImageBitmap,
        srcOffset: IntOffset,
        srcSize: IntSize,
        dstOffset: IntOffset,
        dstSize: IntSize,
        paint: Paint,
    ) {}

    override fun drawRawPoints(pointMode: PointMode, points: FloatArray, paint: Paint) {}
    override fun drawVertices(vertices: Vertices, blendMode: BlendMode, paint: Paint) {}
    override fun enableZ() {}
    override fun disableZ() {}
}

private const val CURVE_SAMPLES = 16
