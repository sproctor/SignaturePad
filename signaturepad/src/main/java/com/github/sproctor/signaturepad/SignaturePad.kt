package com.github.sproctor.signaturepad

import android.view.MotionEvent
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged

@ExperimentalComposeUiApi
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
            .pointerInteropFilter { event ->
                if (enabled) {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            state.gestureStarted(event.x, event.y)
                            if (startedSigning != null)
                                startedSigning()
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            state.gestureMoved(event.x, event.y)
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            state.gestureStopped(event.x, event.y)
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            },
        bitmap = bitmap?.asImageBitmap() ?: ImageBitmap(1, 1),
        contentDescription = contentDescription
    )
}
