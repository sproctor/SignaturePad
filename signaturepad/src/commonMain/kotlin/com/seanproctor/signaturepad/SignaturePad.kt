package com.seanproctor.signaturepad

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * A composable that captures a handwritten signature via drag gestures and renders it as
 * bezier curves.
 *
 * @param state the [SignaturePadState] that holds the captured signature data.
 * @param penColor the color used to draw the signature strokes.
 * @param penWidth the width of the signature strokes in density-independent pixels.
 * @param modifier optional [Modifier] applied to the underlying canvas.
 * @param enabled when `false`, pointer input is ignored and the user cannot draw.
 */
@Composable
public fun SignaturePad(
    state: SignaturePadState,
    penColor: Color,
    penWidth: Dp,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val penWidthPx = with(LocalDensity.current) { penWidth.toPx() }
    val finishedStrokes = rememberGraphicsLayer()
    Spacer(
        modifier = modifier
            .clipToBounds()
            .pointerInput(state, enabled) {
                if (enabled) {
                    // A tap, or a mark too short to pass the touch slop, never starts a drag, so
                    // draw it as a dot where the finger lifted. Once a drag takes over, the tap is
                    // cancelled. A tap that lifts outside the pad is skipped, so it doesn't mark
                    // the signature as started without drawing anything.
                    detectTapGestures { position ->
                        if (position.x in 0f..size.width.toFloat() &&
                            position.y in 0f..size.height.toFloat()
                        ) {
                            state.gestureStarted(position)
                            state.gestureEnded()
                        }
                    }
                }
            }
            .onSizeChanged {
                state.setSize(it.width, it.height)
            }
            .pointerInput(state, enabled) {
                if (enabled) {
                    // The finger drawing the stroke. If it lifts while another finger is down, the
                    // drag carries on with that finger, which shouldn't draw.
                    var strokePointer: PointerId? = null
                    try {
                        detectDragGestures(
                            orientationLock = null,
                            // Start where the finger went down rather than where it crossed the
                            // touch slop, so the beginning of the stroke isn't cut off.
                            onDragStart = { down, slopChange, _ ->
                                // If that finger lifted before the drag started and another one
                                // started it instead, there's no stroke to draw.
                                if (slopChange.id == down.id) {
                                    strokePointer = down.id
                                    state.gestureStarted(down.position)
                                } else {
                                    strokePointer = null
                                }
                            },
                            onDragEnd = { up ->
                                if (up.id != strokePointer) {
                                    if (strokePointer != null) state.gestureEnded()
                                    return@detectDragGestures
                                }
                                // The up event isn't passed to onDrag, but it can still carry the
                                // last bit of movement.
                                up.historical.forEach { state.gestureMoved(it.position) }
                                if (up.historical.isNotEmpty() || up.position != up.previousPosition) {
                                    state.gestureMoved(up.position)
                                }
                                state.gestureEnded()
                            },
                            onDragCancel = { state.gestureEnded() },
                            onDrag = { change: PointerInputChange, _: Offset ->
                                // Another finger took over, so end the stroke rather than draw a
                                // line over to it, and ignore the rest of the drag.
                                if (change.id != strokePointer) {
                                    if (strokePointer != null) state.gestureEnded()
                                    strokePointer = null
                                    return@detectDragGestures
                                }
                                val point = Offset(
                                    change.position.x,
                                    change.position.y,
                                )
                                // Moves between two frames arrive together (Android batches them),
                                // with all but the latest in the history.
                                change.historical.forEach { state.gestureMoved(it.position) }
                                state.gestureMoved(point)
                            }
                        )
                    } finally {
                        // Neither callback runs when this block is cancelled mid-drag (enabled or
                        // state changed), so finish the stroke here.
                        state.gestureEnded()
                    }
                }
            }
            .drawWithCache {
                if (state !is SignaturePadStateImpl) {
                    return@drawWithCache onDrawBehind {
                        drawIntoCanvas { state.drawSignature(it, penColor, penWidthPx) }
                    }
                }
                // Every move redraws the pad, and redrawing a long signature is slow. The finished
                // strokes only change when a stroke ends, so they're drawn once into an offscreen
                // layer, which is reused until then. This block reruns when they change.
                //
                // The two parts are separate draws, so with a translucent pen, the places where the
                // stroke in progress crosses a finished one would be blended twice and look darker.
                // Instead both are drawn opaque into one layer, which is blended once with the pen's
                // alpha, the same as drawing the whole signature as one path.
                val opaquePen = penColor.copy(alpha = 1f)
                val translucentLayer = if (penColor.alpha < 1f) {
                    Paint().apply { alpha = penColor.alpha }
                } else {
                    null
                }
                finishedStrokes.compositingStrategy = CompositingStrategy.Offscreen
                finishedStrokes.record {
                    drawIntoCanvas { state.drawFinishedStrokes(it, opaquePen, penWidthPx) }
                }
                onDrawBehind {
                    drawIntoCanvas { canvas ->
                        if (translucentLayer != null) canvas.saveLayer(size.toRect(), translucentLayer)
                        drawLayer(finishedStrokes)
                        state.drawStrokeInProgress(canvas, opaquePen, penWidthPx)
                        if (translucentLayer != null) canvas.restore()
                    }
                }
            },
    )
}

@Deprecated("Use SignaturePad(SignaturePadState, Color, Dp, Modifier = Modifier, Boolean) instead")
@Composable
public fun SignaturePad(
    state: SignaturePadState,
    penColor: Color,
    penWidth: Dp,
    modifier: Modifier = Modifier,
    startedSigning: () -> Unit,
    enabled: Boolean = true,
) {
    SignaturePad(
        state = state,
        penColor = penColor,
        penWidth = penWidth,
        modifier = modifier,
        enabled = enabled,
    )
    val started by state.signatureStarted
    LaunchedEffect(started) {
        if (started) {
            startedSigning()
        }
    }
}