package com.seanproctor.signaturepad

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import kotlin.math.min

/**
 * State holder for a [SignaturePad] composable. Tracks gesture input, accumulated bezier curves,
 * and provides methods to draw or export the captured signature.
 */
public interface SignaturePadState {
    /** Observable state that is `true` once the user has begun signing. Reset by [clear]. */
    public val signatureStarted: State<Boolean>

    /** Called when a new drag gesture begins at the given [point]. Resets the current stroke. */
    public fun gestureStarted(point: Offset)

    /** Called as the drag gesture moves to a new [point]. Accumulates bezier curve segments. */
    public fun gestureMoved(point: Offset)

    /**
     * Draws the full signature onto [canvas] using the given [penColor] and [penWidth] (in pixels).
     * Typically called from within a Compose `drawIntoCanvas` block.
     */
    public fun drawSignature(canvas: Canvas, penColor: Color, penWidth: Float)

    /**
     * Updates the logical size of the signature area. If the size changes, the current signature
     * is cleared because the existing point coordinates would no longer map correctly to the
     * new dimensions.
     */
    public fun setSize(newWidth: Int, newHeight: Int)

    /** Clears the signature, resetting [signatureStarted] to `false`. */
    public fun clear()

    /**
     * Draws the captured signature onto the given [bitmap]. The signature is scaled to fit the
     * bitmap dimensions while preserving aspect ratio. [penColor] and [penWidth] control the
     * stroke appearance and are applied at the bitmap's resolution (not scaled with the bitmap),
     * allowing the caller to choose an appropriate width for the target output size.
     */
    public fun drawOnBitmap(bitmap: ImageBitmap, penColor: Color, penWidth: Float)
}

public class SignaturePadStateImpl : SignaturePadState {

    private val _signatureStarted = mutableStateOf(false)
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
        if (width == newWidth && height == newHeight) return
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

/** Creates and remembers a [SignaturePadState] scoped to the current composition. */
@Composable
public fun rememberSignaturePadState(): SignaturePadState {
    return remember { SignaturePadStateImpl() }
}
