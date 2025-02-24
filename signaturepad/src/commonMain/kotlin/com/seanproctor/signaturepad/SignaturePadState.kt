package com.seanproctor.signaturepad

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import kotlin.math.min

public interface SignaturePadState {
    public val signatureStarted: State<Boolean>
    public fun gestureStarted(point: Offset)
    public fun gestureMoved(point: Offset)
    public fun drawSignature(canvas: Canvas, penColor: Color, penWidth: Float)
    public fun setSize(newWidth: Int, newHeight: Int)
    public fun clear()
    public fun drawOnBitmap(bitmap: ImageBitmap, penColor: Color, penWidth: Float)
}

public class SignaturePadStateImpl : SignaturePadState {

    private val _signatureStarted = mutableStateOf<Boolean>(false)
    override val signatureStarted: State<Boolean> = _signatureStarted
    private val points = mutableListOf<Offset>()
    private val beziers = mutableStateListOf<Bezier>()
    private var width: Int = 0
    private var height: Int = 0

    override fun gestureStarted(point: Offset) {
        _signatureStarted.value = true
        // Reset state
        points.clear()
        // First segment isn't drawn
        addPoint(point)
        addPoint(point)
    }

    override fun gestureMoved(point: Offset) {
        addPoint(point)
    }

    private fun addPoint(point: Offset) {
        points.add(point)

        // Need 4 points to draw a cubic bezier curve.
        if (points.size > 3) {
            // We're connecting the middle 2 points
            val prevPoint = points[0]
            val startPoint = points[1]
            val endPoint = points[2]
            val nextPoint = points[3]

            // The Bezier's width starts out as the last curve's final width, and
            // gradually changes to the stroke width just calculated. The new
            // width calculation is based on the velocity between the Bezier's
            // start and end points.
            val bezier = Bezier(startPoint, endPoint, prevPoint, nextPoint)
            beziers.add(bezier)

            // Remove the first point
            points.removeAt(0)
        }
    }

    override fun drawSignature(canvas: Canvas, penColor: Color, penWidth: Float) {
        val paint = Paint()
        paint.color = penColor
        paint.strokeWidth = penWidth
        beziers.forEach {
            it.draw(canvas, paint)
        }
    }

    override fun setSize(newWidth: Int, newHeight: Int) {
        width = newWidth
        height = newHeight
        clear()
    }

    override fun clear() {
        _signatureStarted.value = false
        points.clear()
        beziers.clear()
    }

    override fun drawOnBitmap(
        bitmap: ImageBitmap,
        penColor: Color,
        penWidth: Float,
    ) {
        val scaling = min(bitmap.width / width.toFloat(), bitmap.height / height.toFloat())
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.color = penColor
        paint.strokeWidth = penWidth
        beziers.forEach {
            it.scale(scaling).draw(canvas, paint)
        }
    }
}

@Composable
public fun rememberSignaturePadState(): SignaturePadState {
    return remember { SignaturePadStateImpl() }
}
