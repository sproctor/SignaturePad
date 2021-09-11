package com.github.sproctor.signaturepad

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
                        onDragStart = { offset ->
                            state.gestureStarted(offset.x, offset.y)
                            if (startedSigning != null)
                                startedSigning()
                        },
                        onDragEnd = { },
                        onDragCancel = { },
                        onDrag = { change: PointerInputChange, _: Offset ->
                            state.gestureMoved(change.position.x, change.position.y)
                        }
                    )
                }
            },
        bitmap = bitmap?.asImageBitmap() ?: ImageBitmap(1, 1),
        contentDescription = contentDescription
    )
}
