package com.seanproctor.signaturepad

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.HistoricalChange
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.scene.ComposeScenePointer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Android delivers the moves between two frames in one event, with all but the latest in its history.
 * The ui-test touch injection doesn't pass history on, so these tests send events to a scene directly,
 * which takes Compose's internal pointer type.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class SignaturePadBatchedMovesTest {

    private fun drawOnPad(events: ImageComposeScene.() -> Unit): SignaturePadState {
        val state = SignaturePadStateImpl()
        val scene = ImageComposeScene(200, 200, Density(1f)) {
            SignaturePad(state, Color.Black, 3.dp, Modifier.fillMaxSize())
        }
        try {
            scene.render()
            scene.events()
            scene.render()
        } finally {
            scene.close()
        }
        return state
    }

    private fun ImageComposeScene.send(
        type: PointerEventType,
        position: Offset,
        timeMillis: Long,
        history: List<Offset> = emptyList(),
    ) {
        val pointer = ComposeScenePointer(
            id = PointerId(0),
            position = position,
            pressed = type != PointerEventType.Release,
            type = PointerType.Touch,
            historical = history.mapIndexed { i, point -> HistoricalChange(timeMillis - history.size + i, point) },
        )
        sendPointerEvent(type, listOf(pointer), timeMillis = timeMillis)
    }

    private fun SignaturePadState.ink(): List<Offset> =
        RecordingCanvas().also { drawSignature(it, Color.Black, 3f) }.allPoints

    @Test
    fun movesBatchedIntoOneEvent_areAllDrawn() {
        val state = drawOnPad {
            send(PointerEventType.Press, Offset(20f, 100f), 0)
            send(PointerEventType.Move, Offset(60f, 100f), 16)
            // Within one frame the finger went up to y = 20 and came back down.
            send(
                PointerEventType.Move, Offset(140f, 100f), 32,
                history = listOf(Offset(80f, 60f), Offset(100f, 20f), Offset(120f, 60f)),
            )
            send(PointerEventType.Release, Offset(140f, 100f), 48)
        }

        val ink = state.ink()
        assertEquals(20f, ink.minOf { it.y }, 1f, "the batched moves weren't drawn")
        // The stroke only goes right, so drawing the moves out of order would show as ink going back.
        assertTrue(ink.zipWithNext().all { (a, b) -> b.x >= a.x - 1f }, "the batched moves were drawn out of order")
    }

    @Test
    fun movesBatchedIntoTheUpEvent_areAllDrawn() {
        val state = drawOnPad {
            send(PointerEventType.Press, Offset(20f, 100f), 0)
            send(PointerEventType.Move, Offset(60f, 100f), 16)
            send(
                PointerEventType.Release, Offset(140f, 100f), 32,
                history = listOf(Offset(80f, 60f), Offset(100f, 20f), Offset(120f, 60f)),
            )
        }

        val ink = state.ink()
        assertEquals(20f, ink.minOf { it.y }, 1f, "the moves batched into the up event weren't drawn")
        assertEquals(140f, ink.last().x, 1f, "the stroke doesn't end where the finger lifted")
    }

    @Test
    fun liftingBackWhereThePreviousEventWas_endsTheStrokeThere() {
        val state = drawOnPad {
            send(PointerEventType.Press, Offset(20f, 100f), 0)
            send(PointerEventType.Move, Offset(60f, 100f), 16)
            // The finger went up and came back, so the up event's position matches the last move.
            send(
                PointerEventType.Release, Offset(60f, 100f), 32,
                history = listOf(Offset(80f, 60f), Offset(100f, 20f), Offset(80f, 60f)),
            )
        }

        val ink = state.ink()
        assertEquals(20f, ink.minOf { it.y }, 1f, "the moves batched into the up event weren't drawn")
        assertEquals(Offset(60f, 100f), ink.last(), "the stroke doesn't end where the finger lifted")
    }
}
