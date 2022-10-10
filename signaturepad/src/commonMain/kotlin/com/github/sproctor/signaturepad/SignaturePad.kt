package com.github.sproctor.signaturepad

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged

@Composable
public fun SignaturePad(
    modifier: Modifier = Modifier,
    state: SignaturePadState,
    contentDescription: String? = null,
    startedSigning: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val bitmap = state.displayBitmap.value
    Image(
        modifier = modifier
            .onSizeChanged {
                state.setSize(it)
            }
            .pointerInput(enabled) {
                if (enabled) {
                    detectDragGestures(
                        onDragStart = {
                            state.gestureStarted()
                            if (startedSigning != null)
                                startedSigning()
                        },
                        onDragEnd = { },
                        onDragCancel = { },
                        onDrag = { change: PointerInputChange, _: Offset ->
                            val prev = TimedPoint(
                                change.previousPosition.x,
                                change.previousPosition.y,
                                change.previousUptimeMillis,
                            )
                            val point = TimedPoint(
                                change.position.x,
                                change.position.y,
                                change.uptimeMillis,
                            )
                            state.gestureMoved(prev, point)
                        }
                    )
                }
            },
        bitmap = bitmap?.asImageBitmap() ?: ImageBitmap(1, 1),
        contentDescription = contentDescription
    )
}
