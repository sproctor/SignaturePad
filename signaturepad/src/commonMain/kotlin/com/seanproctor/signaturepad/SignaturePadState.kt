package com.seanproctor.signaturepad

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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

    // Whether the next curve starts a new stroke. It does after a gesture starts, and after anything
    // else that discards the points of the stroke in progress.
    private var nextCurveStartsStroke = true

    // The gesture's last point, on the pad or off it. Each move is drawn as the line from here, so
    // a stroke can end and start exactly where that line crosses the pad's edge. Null when there is
    // nothing to draw from.
    private var lastGesturePoint: Offset? = null

    // The number of curves at the end of [beziers] that belong to the stroke in progress. The ones
    // before them are finished, and only change when a stroke ends or the whole signature changes, so
    // the pad can cache them and redraw just the stroke in progress on each move.
    private var strokeCurveCount = 0

    // Changes whenever the finished curves do. Read by drawFinishedStrokes so that the pad knows when
    // to redraw its cache.
    private val finishedStrokesVersion = mutableIntStateOf(0)

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
        // Usually the last stroke has ended and there's no stroke in progress, so the finished
        // strokes don't change and the pad's cache of them stays valid.
        resetStroke(finishedStrokesChanged = strokeCurveCount > 0)
        dragTo(point)
    }

    override fun gestureMoved(point: Offset) {
        if (gestureActive) dragTo(point)
    }

    private fun dragTo(point: Offset) {
        // Only the part of the move that's on the pad is drawn. Leaving the pad ends the stroke at
        // the edge, and coming back in starts a new one at the edge, rather than drawing a line
        // across the pad from where the finger left.
        val onPad = clipToPad(lastGesturePoint ?: point, point)
        if (onPad != null) {
            val (entry, exit) = onPad
            addPoint(entry)
            addPoint(exit)
            if (exit != point) finishStroke()
        }
        lastGesturePoint = point
    }

    /**
     * The part of the line from [from] to [to] that's on the pad, as the points where it enters and
     * leaves, or null if the line misses the pad. An end that's already on the pad is returned as is.
     */
    private fun clipToPad(from: Offset, to: Offset): Pair<Offset, Offset>? {
        val dx = to.x - from.x
        val dy = to.y - from.y
        // Liang–Barsky: the line is from + t·(dx, dy) for t in 0..1, and each edge limits t with
        // p·t <= q. Edges it runs towards the inside of raise the lower limit, and the rest lower
        // the upper one.
        var enter = 0f
        var leave = 1f
        fun limit(p: Float, q: Float): Boolean {
            if (p == 0f) return q >= 0f
            val t = q / p
            if (p < 0f) enter = maxOf(enter, t) else leave = minOf(leave, t)
            return enter <= leave
        }
        if (!limit(-dx, from.x) || !limit(dx, width - from.x) ||
            !limit(-dy, from.y) || !limit(dy, height - from.y)
        ) {
            return null
        }
        // Rounding can leave an interpolated point a hair off the pad, so clamp it back on.
        fun at(t: Float) = Offset(
            (from.x + dx * t).coerceIn(0f, width.toFloat()),
            (from.y + dy * t).coerceIn(0f, height.toFloat()),
        )
        return Pair(if (enter == 0f) from else at(enter), if (leave == 1f) to else at(leave))
    }

    private fun addPoint(point: Offset) {
        // Moves that don't go anywhere add nothing to the stroke. This also skips the entry point of
        // a move that carries on a stroke, since that's the point the stroke already ends at.
        if (points.lastOrNull() == point) return

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

            val bezier = Bezier(startPoint, endPoint, prevPoint, nextPoint, nextCurveStartsStroke)
            beziers.add(bezier)
            strokeCurveCount++
            nextCurveStartsStroke = false

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
            beziers.add(Bezier(startPoint, endPoint, prevPoint, endPoint, nextCurveStartsStroke))
        } else if (points.size == 2) {
            // Usually a tap: its only point is buffered twice, so this draws a dot.
            val (startPoint, endPoint) = points
            beziers.add(Bezier(startPoint, endPoint, startPoint, endPoint, nextCurveStartsStroke))
        }
        resetStroke()
    }

    // Ends the stroke in progress, which makes its curves part of the finished ones. Everything that
    // changes the finished curves (ending a stroke, clearing, resizing, restoring) goes through here.
    private fun resetStroke(finishedStrokesChanged: Boolean = true) {
        points.clear()
        nextCurveStartsStroke = true
        lastGesturePoint = null
        strokeCurveCount = 0
        if (finishedStrokesChanged) finishedStrokesVersion.intValue++
    }

    override fun drawSignature(canvas: Canvas, penColor: Color, penWidth: Float) {
        drawCurves(canvas, beziers, penPaint(penColor, penWidth))
    }

    /**
     * Draws the finished strokes: everything but the stroke in progress. Only observes the changes
     * that affect them, so a cache drawn with this stays valid while a stroke is being drawn.
     */
    internal fun drawFinishedStrokes(canvas: Canvas, penColor: Color, penWidth: Float) {
        finishedStrokesVersion.intValue
        Snapshot.withoutReadObservation {
            val finished = beziers.subList(0, beziers.size - strokeCurveCount)
            drawCurves(canvas, finished, penPaint(penColor, penWidth))
        }
    }

    /** Draws the stroke in progress, the part of the signature that [drawFinishedStrokes] leaves out. */
    internal fun drawStrokeInProgress(canvas: Canvas, penColor: Color, penWidth: Float) {
        // Reading the version redraws this when a stroke ends and moves to the finished ones.
        finishedStrokesVersion.intValue
        val stroke = beziers.subList(beziers.size - strokeCurveCount, beziers.size)
        drawCurves(canvas, stroke, penPaint(penColor, penWidth))
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

        resetStroke()

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
        resetStroke()
        beziers.clear()
        resetRemapSource()
    }

    private fun resetRemapSource() {
        remapSource = emptyList()
        remappedCount = -1
    }

    /**
     * Flattens the captured signature to a list of floats for [rememberSaveable]: the pad size the
     * curves belong to, the started flag, then for each curve its four source points as `x, y` pairs
     * followed by whether it starts a stroke.
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
            data.add(if (bezier.startsStroke) 1f else 0f)
        }
        return data
    }

    /** Restores the signature previously produced by [toFloatList]. */
    internal fun restoreFromFloatList(data: List<Float>) {
        if (data.size < SAVE_HEADER_SIZE) return
        width = data[0].toInt()
        height = data[1].toInt()
        _signatureStarted.value = data[2] != 0f
        resetStroke()
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
                    startsStroke = data[i + 8] != 0f,
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
        drawCurves(Canvas(bitmap), beziers.map { it.scale(scaling) }, penPaint(penColor, penWidth))
    }

    private fun drawCurves(canvas: Canvas, curves: List<Bezier>, paint: Paint) {
        if (curves.isEmpty()) return
        canvas.drawPath(pathOf(curves), paint)
    }

    private fun penPaint(color: Color, width: Float) = Paint().apply {
        this.color = color
        style = PaintingStyle.Stroke
        strokeWidth = width
        // Round ends and corners, so strokes look like they were drawn with a round pen.
        strokeCap = StrokeCap.Round
        strokeJoin = StrokeJoin.Round
    }

    private companion object {
        // width, height, signatureStarted
        const val SAVE_HEADER_SIZE = 3

        // four source points, each an (x, y) pair, then whether the curve starts a stroke
        const val FLOATS_PER_BEZIER = 9
    }
}

/**
 * A [Saver] that lets a [SignaturePadState] survive configuration changes and process death via
 * [rememberSaveable]. The captured signature is preserved; [resizeBehavior] is supplied here rather
 * than saved, because it may hold a non-serializable lambda ([ResizeBehavior.Custom]).
 *
 * Most callers can rely on [rememberSaveableSignaturePadState], which already saves through this.
 * Use this directly only when managing the state with your own [rememberSaveable] call.
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
