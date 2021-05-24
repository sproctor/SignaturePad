package com.github.sproctor.signaturepad

import android.graphics.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.scale
import kotlin.math.min
import kotlin.math.sqrt

public class SignaturePadState(
    public val density: Density,
    public val penColor: Color = Color.Black,
    public val minPenWidth: Dp = 2.dp,
    public val maxPenWidth: Dp = 5.dp,
    public val velocityFilterWeight: Float = 0.5f,
    public val velocityScale: Float = 0.2f,
) {
    internal val displayBitmap: MutableState<Bitmap?> =
        mutableStateOf(value = null, policy = neverEqualPolicy())
    private val displayPaint = Paint()
    private var displayCanvas: Canvas = Canvas()

    private var maskBitmap: Bitmap? = null
    private val maskPaint = Paint()
    private var maskCanvas: Canvas = Canvas()

    private val points = mutableListOf<TimedPoint>()
    private var lastVelocity: Float? = null
    private val minPenWidthPx = with(density) { minPenWidth.toPx() }
    private val maxPenWidthPx = with(density) { maxPenWidth.toPx() }
    private var lastPenWidth = minPenWidthPx

    init {
        if (velocityFilterWeight < 0f || velocityFilterWeight > 1f) {
            throw IllegalArgumentException("velocityFilterWeight must be between 0 and 1")
        }
        if (minPenWidth > maxPenWidth) {
            throw IllegalArgumentException("minPenWidth cannot be less than maxPenWidth")
        }
        displayPaint.color = penColor.toArgb()
        initPaint(displayPaint)
        maskPaint.color = Color.Black.toArgb()
        initPaint(maskPaint)
    }

    private fun initPaint(paint: Paint) {
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
    }

    internal fun gestureStarted(x: Float, y: Float) {
        // Reset state
        points.clear()
        lastVelocity = null
        lastPenWidth = minPenWidthPx
        val newPoint = TimedPoint(x, y)

        // Since we only draw the middle section, duplicate the first point
        // so that we draw the first section
        points.add(newPoint)
        points.add(newPoint)
    }

    internal fun gestureMoved(x: Float, y: Float) {
        addPoint(x, y)
    }

    internal fun gestureStopped(x: Float, y: Float) {
        addPoint(x, y)
    }

    private fun addPoint(x: Float, y: Float) {
        points.add(TimedPoint(x, y))

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
            addBezier(startPoint, endPoint, control1, control2, lastPenWidth, newWidth)

            // Remove the first point
            points.removeAt(0)

            lastPenWidth = newWidth
        }
    }

    private fun addBezier(
        startPoint: ControlPoint,
        endPoint: ControlPoint,
        control1: ControlPoint,
        control2: ControlPoint,
        startWidth: Float,
        endWidth: Float
    ) {
        val widthDelta = endWidth - startWidth
        val drawSteps = endPoint.distanceTo(startPoint).toInt() + 1

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

            val strokeWidth = startWidth + ttt * widthDelta
            displayPaint.strokeWidth = strokeWidth
            maskPaint.strokeWidth = strokeWidth
            displayCanvas.drawPoint(x, y, displayPaint)
            displayBitmap.value = displayBitmap.value
            maskCanvas.drawPoint(x, y, maskPaint)
        }
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
            (maxPenWidthPx - minPenWidthPx) / (1 + velocity * velocityScale / density.density)
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
        displayBitmap.value = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            displayCanvas = Canvas(this)
        }
        maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            maskCanvas = Canvas(this)
        }
    }

    public fun getSignatureBitmap(
        width: Int,
        height: Int,
        penColor: Color = Color.Black,
        backgroundColor: Color = Color.White
    ): Bitmap {
        val maskBitmap = this.maskBitmap ?: throw Exception("Bitmap not created")
        val result =
            Bitmap.createBitmap(maskBitmap.width, maskBitmap.height, Bitmap.Config.ARGB_8888)
        val signature =
            Bitmap.createBitmap(maskBitmap.width, maskBitmap.height, Bitmap.Config.ARGB_8888)
        val signatureCanvas = Canvas(signature)
        signatureCanvas.drawColor(penColor.toArgb())
        val xferPaint = Paint()
        xferPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        signatureCanvas.drawBitmap(maskBitmap, 0f, 0f, xferPaint)
        val canvas = Canvas(result)
        canvas.drawColor(backgroundColor.toArgb())
        canvas.drawBitmap(signature, 0f, 0f, null)
        return result.scale(width, height)
    }
}

@Composable
public fun rememberSignaturePadState(penColor: Color): SignaturePadState {
    val density = LocalDensity.current
    return remember { SignaturePadState(density = density, penColor = penColor) }
}
