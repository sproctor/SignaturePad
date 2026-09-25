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
import androidx.compose.ui.graphics.StrokeCap
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
     * Called when the drag gesture ends or is cancelled. Draws the last segment of the stroke, which
     * [gestureMoved] holds back until it knows the next point.
     */
    public fun gestureEnded() {}

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
    // Cleared when the gesture ends, or when clear() cuts a stroke short, so later moves are ignored
    // instead of drawing on the emptied pad.
    private var gestureActive = false
    private val beziers = mutableStateListOf<Bezier>()
    private var width: Int = 0
    private var height: Int = 0

    // The curves the on-screen ones were last remapped from, and the pad size they belong to. Each
    // resize maps from these rather than from the previous result, so going back and forth between
    // sizes (rotating and rotating back, say) puts the signature back exactly where it was.
    private var remapSource: List<Bezier> = emptyList()
    private var remapSourceSize = Size.Zero

    // The curve count right after the last remap. If it differs, strokes were drawn since and what's
    // on screen becomes the new source.
    private var remappedCount = -1

    override fun gestureStarted(point: Offset) {
        _signatureStarted.value = true
        gestureActive = true
        // Reset state
        points.clear()
        addPoint(point)
    }

    override fun gestureMoved(point: Offset) {
        if (gestureActive) addPoint(point)
    }

    private fun addPoint(point: Offset) {
        // Leaving the pad ends the stroke, so coming back in starts a new one rather than drawing a
        // line across the pad from where the finger left.
        if (point.x < 0 || point.x > width || point.y < 0 || point.y > height) {
            finishStroke()
            return
        }

        // A stroke's first point goes in twice so that its first segment gets drawn.
        if (points.isEmpty()) points.add(point)
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

    override fun gestureEnded() {
        gestureActive = false
        finishStroke()
    }

    private fun finishStroke() {
        // The last segment is still waiting for a next point that won't come. Draw it with its own
        // end point standing in for the next one, the same way the first segment reuses its start.
        if (points.size >= 3) {
            val (prevPoint, startPoint, endPoint) = points.takeLast(3)
            beziers.add(Bezier(startPoint, endPoint, prevPoint, endPoint))
        }
        points.clear()
    }

    override fun drawSignature(canvas: Canvas, penColor: Color, penWidth: Float) {
        val paint = penPaint(penColor, penWidth)
        beziers.forEach {
            it.draw(canvas, paint)
        }
    }

    override fun setSize(newWidth: Int, newHeight: Int) {
        // Nothing can be drawn at a zero size (while the pad is collapsed, for example), so keep the
        // signature as it is and remap from the last real size once the pad is laid out again.
        if (newWidth <= 0 || newHeight <= 0) return
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

        if (beziers.size != remappedCount) {
            remapSource = beziers.toList()
            remapSourceSize = Size(oldWidth.toFloat(), oldHeight.toFloat())
        }
        val newSize = Size(newWidth.toFloat(), newHeight.toFloat())
        val remapped = remapSource.map { bezier ->
            bezier.map { point -> resizeBehavior.mapPoint(point, remapSourceSize, newSize) }
        }
        beziers.clear()
        beziers.addAll(remapped)
        remappedCount = beziers.size
    }

    override fun clear() {
        _signatureStarted.value = false
        gestureActive = false
        points.clear()
        beziers.clear()
        resetRemapSource()
    }

    private fun resetRemapSource() {
        remapSource = emptyList()
        remappedCount = -1
    }

    /**
     * Flattens the captured signature to a list of floats for [rememberSaveable]: the pad size the
     * curves belong to, the started flag, then each curve's four source points as `x, y` pairs.
     */
    internal fun toFloatList(): List<Float> {
        // Save the remap source while it's still current, so that resizing after a restore (the
        // activity is recreated on rotation) also maps from it.
        val fromSource = beziers.size == remappedCount
        val saved = if (fromSource) remapSource else beziers
        val data = ArrayList<Float>(SAVE_HEADER_SIZE + saved.size * FLOATS_PER_BEZIER)
        data.add(if (fromSource) remapSourceSize.width else width.toFloat())
        data.add(if (fromSource) remapSourceSize.height else height.toFloat())
        data.add(if (_signatureStarted.value) 1f else 0f)
        for (bezier in saved) {
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
        resetRemapSource()
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

    override fun drawOnBitmap(
        bitmap: ImageBitmap,
        penColor: Color,
        penWidth: Float,
    ) {
        val scaling = min(bitmap.width / width.toFloat(), bitmap.height / height.toFloat())
        val canvas = Canvas(bitmap)
        val paint = penPaint(penColor, penWidth)
        beziers.forEach {
            it.scale(scaling).draw(canvas, paint)
        }
    }

    private fun penPaint(color: Color, width: Float) = Paint().apply {
        this.color = color
        strokeWidth = width
        // Curves are drawn as runs of points. A round cap makes each point a dot instead of a square,
        // so lines keep the same width in every direction.
        strokeCap = StrokeCap.Round
    }

    private companion object {
        // width, height, signatureStarted
        const val SAVE_HEADER_SIZE = 3

        // four source points, each an (x, y) pair
        const val FLOATS_PER_BEZIER = 8
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
