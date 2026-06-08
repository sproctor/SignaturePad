package com.seanproctor.signaturepad

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
     * Updates the logical size of the signature area. If the size changes, the existing signature
     * is remapped to the new dimensions according to the state's [ResizeBehavior] (cleared,
     * recentered, scaled, or transformed by a custom mapper).
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

public class SignaturePadStateImpl(
    private val resizeBehavior: ResizeBehavior = ResizeBehavior.Clear,
) : SignaturePadState {

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
        // Don't add points outside the bounds
        if (point.x < 0 || point.x > width || point.y < 0 || point.y > height)
            return

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
        val oldWidth = width
        val oldHeight = height
        width = newWidth
        height = newHeight

        // The first layout pass establishes the size from nothing, so there is nothing to remap.
        if (oldWidth == 0 || oldHeight == 0) return

        if (resizeBehavior == ResizeBehavior.Clear) {
            clear()
            return
        }

        points.clear()

        val oldSize = Size(oldWidth.toFloat(), oldHeight.toFloat())
        val newSize = Size(newWidth.toFloat(), newHeight.toFloat())
        val remapped = beziers.map { bezier ->
            bezier.map { point -> resizeBehavior.mapPoint(point, oldSize, newSize) }
        }
        beziers.clear()
        beziers.addAll(remapped)
    }

    override fun clear() {
        _signatureStarted.value = false
        points.clear()
        beziers.clear()
    }

    /**
     * Flattens the captured signature to a list of floats for [rememberSaveable]: the pad size, the
     * started flag, then each curve's four source points as `x, y` pairs.
     */
    internal fun toFloatList(): List<Float> {
        val data = ArrayList<Float>(SAVE_HEADER_SIZE + beziers.size * FLOATS_PER_BEZIER)
        data.add(width.toFloat())
        data.add(height.toFloat())
        data.add(if (_signatureStarted.value) 1f else 0f)
        for (bezier in beziers) {
            for (point in bezier.sourcePoints()) {
                data.add(point.x)
                data.add(point.y)
            }
        }
        return data
    }

    /** Restores the signature previously produced by [toFloatList]. */
    internal fun restoreFromFloatList(data: List<Float>) {
        if (data.size < SAVE_HEADER_SIZE) return
        width = data[0].toInt()
        height = data[1].toInt()
        _signatureStarted.value = data[2] != 0f
        points.clear()
        beziers.clear()
        var i = SAVE_HEADER_SIZE
        while (i + FLOATS_PER_BEZIER <= data.size) {
            beziers.add(
                Bezier(
                    startPoint = Offset(data[i], data[i + 1]),
                    endPoint = Offset(data[i + 2], data[i + 3]),
                    prevPoint = Offset(data[i + 4], data[i + 5]),
                    nextPoint = Offset(data[i + 6], data[i + 7]),
                )
            )
            i += FLOATS_PER_BEZIER
        }
    }

    private companion object {
        // width, height, signatureStarted
        const val SAVE_HEADER_SIZE = 3

        // four source points, each an (x, y) pair
        const val FLOATS_PER_BEZIER = 8
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

/**
 * A [Saver] that lets a [SignaturePadState] survive configuration changes and process death via
 * [rememberSaveable]. The captured signature is preserved; [resizeBehavior] is supplied here rather
 * than saved, because it may hold a non-serializable lambda ([ResizeBehavior.Custom]).
 *
 * Most callers can rely on [rememberSignaturePadState], which already saves through this. Use this
 * directly only when managing the state with your own [rememberSaveable] call.
 */
public fun SignaturePadStateSaver(
    resizeBehavior: ResizeBehavior = ResizeBehavior.Clear,
): Saver<SignaturePadState, Any> = listSaver(
    save = { state -> (state as SignaturePadStateImpl).toFloatList() },
    restore = { data -> SignaturePadStateImpl(resizeBehavior).apply { restoreFromFloatList(data) } },
)

/** Creates and remembers a [SignaturePadState] scoped to the current composition. */
@Composable
public fun rememberSignaturePadState(
    resizeBehavior: ResizeBehavior = ResizeBehavior.Clear,
): SignaturePadState {
    return remember(resizeBehavior) { SignaturePadStateImpl(resizeBehavior) }
}

/**
 * Creates and remembers a [SignaturePadState] that survives configuration changes and process death
 * via [rememberSaveable]. Use this instead of [rememberSignaturePadState] when the captured
 * signature must be preserved across such events (e.g. an orientation change).
 *
 * @param resizeBehavior how an in-progress signature is transformed when the pad is resized.
 * Defaults to [ResizeBehavior.Clear]. Note that on a size change (e.g. an orientation change) the
 * restored signature is remapped according to this behavior, so [ResizeBehavior.Clear] will discard
 * it — choose another behavior to keep the signature across such changes.
 */
@Composable
public fun rememberSaveableSignaturePadState(
    resizeBehavior: ResizeBehavior = ResizeBehavior.Clear,
): SignaturePadState {
    return rememberSaveable(resizeBehavior, saver = SignaturePadStateSaver(resizeBehavior)) {
        SignaturePadStateImpl(resizeBehavior)
    }
}
