package com.seanproctor.signaturepad

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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
    Canvas(
        modifier = modifier
            .onSizeChanged {
                state.setSize(it.width, it.height)
            }
            .pointerInput(enabled) {
                if (enabled) {
                    detectDragGestures(
                        orientationLock = null,
                        // Start where the finger went down rather than where it crossed the touch
                        // slop, so the beginning of the stroke isn't cut off.
                        onDragStart = { down, _, _ ->
                            state.gestureStarted(down.position)
                        },
                        onDragEnd = { up ->
                            // The up event isn't passed to onDrag, but it can still carry the
                            // last bit of movement.
                            if (up.position != up.previousPosition) {
                                state.gestureMoved(up.position)
                            }
                            state.gestureEnded()
                        },
                        onDragCancel = { state.gestureEnded() },
                        onDrag = { change: PointerInputChange, _: Offset ->
                            val point = Offset(
                                change.position.x,
                                change.position.y,
                            )
                            state.gestureMoved(point)
                        }
                    )
                }
            },
    ) {
        drawIntoCanvas { canvas ->
            state.drawSignature(canvas, penColor, penWidthPx)
        }
    }
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