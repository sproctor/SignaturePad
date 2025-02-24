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
                        onDragStart = {
                            state.gestureStarted(it)
                        },
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