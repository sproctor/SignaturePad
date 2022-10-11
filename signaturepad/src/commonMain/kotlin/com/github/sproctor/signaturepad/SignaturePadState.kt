package com.github.sproctor.signaturepad

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.bitmap.resized
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.paint.ColorPaint
import com.soywiz.korim.paint.DefaultPaint
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.ScaleMode
import kotlin.math.min
import kotlin.math.sqrt

public class SignaturePadState(
    public val density: Density,
    public val penColor: Color = Color.Black,
    public val minPenWidth: Dp = 1.dp,
    public val maxPenWidth: Dp = 4.dp,
    public val velocityFilterWeight: Float = 0.6f,
    public val velocityScale: Float = 1.5f,
) {
    internal val displayBitmap: MutableState<Bitmap?> =
        mutableStateOf(value = null, policy = neverEqualPolicy())

    private var maskBitmap: Bitmap? = null

    private val points = mutableListOf<TimedPoint>()
    private var lastVelocity: Float? = null
    private val minPenWidthPx = with(density) { minPenWidth.toPx() }
    private val maxPenWidthPx = with(density) { maxPenWidth.toPx() }

    init {
        if (velocityFilterWeight < 0f || velocityFilterWeight > 1f) {
            throw IllegalArgumentException("velocityFilterWeight must be between 0 and 1")
        }
        if (minPenWidth > maxPenWidth) {
            throw IllegalArgumentException("minPenWidth cannot be less than maxPenWidth")
        }
    }

    internal fun gestureStarted() {
        // Reset state
        points.clear()
        lastVelocity = null
    }

    internal fun gestureMoved(previousPoint: TimedPoint, point: TimedPoint) {
        // If the previous point was the first one, add it
        if (points.size == 0) {
            points.add(previousPoint)
        }
        addPoint(point)
    }

    private fun addPoint(point: TimedPoint) {
        points.add(point)

        // Need 4 points to draw a cubic bezier curve.
        if (points.size > 3) {
            // We're connecting the middle 2 points
            val prevPoint = points[0]
            val startPoint = points[1]
            val endPoint = points[2]
            val nextPoint = points[3]

            val velocity = filterVelocity(endPoint.velocityFrom(startPoint))

            // The new width is a function of the velocity. Higher velocities
            // correspond to thinner strokes.
            val newWidth = strokeWidth(velocity)

            // Calculate control points based on the 4 points
            val control1 = calculateControlPoint(prevPoint, startPoint, endPoint, false)
            val control2 = calculateControlPoint(startPoint, endPoint, nextPoint, true)

            // The Bezier's width starts out as the last curve's final width, and
            // gradually changes to the stroke width just calculated. The new
            // width calculation is based on the velocity between the Bezier's
            // start and end points.
            addBezier(startPoint, endPoint, control1, control2, newWidth)

            // Remove the first point
            points.removeAt(0)
        }
    }

    private fun addBezier(
        startPoint: ControlPoint,
        endPoint: ControlPoint,
        control1: ControlPoint,
        control2: ControlPoint,
        lineWidth: Float,
    ) {
        val drawSteps = endPoint.distanceTo(startPoint).toInt() + 1

        val displayContext2d = displayBitmap.value!!.getContext2d()
        val maskContext2d = maskBitmap!!.getContext2d()

        displayContext2d.moveTo(startPoint.x.toDouble(), startPoint.y.toDouble())
        maskContext2d.moveTo(startPoint.x.toDouble(), startPoint.y.toDouble())
        for (i in 0..drawSteps) {
            val t = i.toDouble() / drawSteps
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

            displayContext2d.lineTo(x, y)
            maskContext2d.lineTo(x, y)
        }
        displayContext2d.stroke(
            paint = ColorPaint(penColor.toRGBA()),
            lineWidth = lineWidth.toDouble(),
            begin = false,
        )
        maskContext2d.stroke(
            paint = DefaultPaint,
            lineWidth = lineWidth.toDouble(),
            begin = false
        )
        displayContext2d.dispose()
        maskContext2d.dispose()
        displayBitmap.value = displayBitmap.value
    }

    private fun calculateControlPoint(
        p1: ControlPoint,
        p2: ControlPoint,
        p3: ControlPoint,
        first: Boolean // whether to return the first or second control point
    ): ControlPoint {
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
        val k0 = l2 / (l1 + l2)
        val k = if (k0.isNaN()) 0f else k0
        val cmx = m2x + dxm * k
        val cmy = m2y + dym * k

        val tx = p2.x - cmx
        val ty = p2.y - cmy

        return if (first) {
            ControlPoint(m1x + tx, m1y + ty)
        } else {
            ControlPoint(m2x + tx, m2y + ty)
        }
    }

    /**
     * A simple lowpass filter to mitigate velocity aberrations
     */
    private fun filterVelocity(velocity: Float): Float {
        return lastVelocity?.let {
            velocityFilterWeight * velocity + (1f - velocityFilterWeight) * it
        }
            ?: velocity
    }

    private fun strokeWidth(velocity: Float): Float {
        return minPenWidthPx + min(
            maxPenWidthPx - minPenWidthPx,
            (maxPenWidthPx - minPenWidthPx) / (1f + velocity * velocityScale / density.density)
        )
    }

    public fun setSize(size: IntSize) {
        if (size.width != displayBitmap.value?.width || size.height != displayBitmap.value?.height) {
            createBitmap(size.width, size.height)
        }
    }

    public fun clear() {
        val oldBitmap = displayBitmap.value
        if (oldBitmap != null) {
            createBitmap(oldBitmap.width, oldBitmap.height)
        }
    }

    private fun createBitmap(width: Int, height: Int) {
        displayBitmap.value = Bitmap32(width, height, false)
        maskBitmap = Bitmap32(width, height, false)
    }

    public fun getSignatureBitmap(
        width: Int,
        height: Int,
        penColor: Color = Color.Black,
        backgroundColor: Color = Color.White
    ): ImageBitmap {
        val maskBitmap = this.maskBitmap ?: throw Exception("Bitmap not created")
        val result = maskBitmap.resized(width, height, ScaleMode.COVER, Anchor.CENTER)
        return result.asImageBitmap()
    }
}

@Composable
public fun rememberSignaturePadState(penColor: Color): SignaturePadState {
    val density = LocalDensity.current
    return remember { SignaturePadState(density = density, penColor = penColor) }
}

private fun Color.toRGBA(): RGBA {
    return RGBA(
        r = (red * 255).toInt(),
        g = (green * 255).toInt(),
        b = (blue * 255).toInt(),
        a = (alpha * 255).toInt(),
    )
}