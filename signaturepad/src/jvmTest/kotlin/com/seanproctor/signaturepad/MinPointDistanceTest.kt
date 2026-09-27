package com.seanproctor.signaturepad

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.scene.ComposeScenePointer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Desktop mice report whole pixels, so a slow stroke arrives as a staircase of 1-pixel steps. */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class MinPointDistanceTest {

    private fun drawStaircase(minPointDistance: Dp, density: Float = 1f): RecordingCanvas {
        val state = SignaturePadStateImpl()
        val scene = ImageComposeScene(400, 400, Density(density)) {
            SignaturePad(state, Color.Black, 3.dp, Modifier.fillMaxSize(), minPointDistance = minPointDistance)
        }
        try {
            scene.render()
            var point = Offset(20f, 100f)
            var time = 0L
            scene.send(PointerEventType.Press, point, time)
            repeat(STEPS) { i ->
                point += if (i % 2 == 0) Offset(1f, 0f) else Offset(0f, 1f)
                time += 8
                scene.send(PointerEventType.Move, point, time)
            }
            scene.send(PointerEventType.Release, point, time + 8)
            scene.render()
        } finally {
            scene.close()
        }
        return RecordingCanvas().also { state.drawSignature(it, Color.Black, 3f) }
    }

    private fun ImageComposeScene.send(type: PointerEventType, position: Offset, timeMillis: Long) {
        val pointer = ComposeScenePointer(
            id = PointerId(0),
            position = position,
            pressed = type != PointerEventType.Release,
            type = PointerType.Touch,
        )
        sendPointerEvent(type, listOf(pointer), timeMillis = timeMillis)
    }

    @Test
    fun movesCloserThanTheMinimum_areSkipped() {
        val everyMove = drawStaircase(0.dp).drawnStrokes.size
        val filtered = drawStaircase(2.dp).drawnStrokes.size
        assertTrue(filtered < everyMove / 2, "expected fewer than half of $everyMove curves, got $filtered")
    }

    @Test
    fun theMinimumIsInDp() {
        val atDensity1 = drawStaircase(2.dp, density = 1f).drawnStrokes.size
        val atDensity2 = drawStaircase(2.dp, density = 2f).drawnStrokes.size
        assertTrue(atDensity2 < atDensity1, "2.dp skipped no more moves at density 2 ($atDensity2 curves) than at 1 ($atDensity1)")
    }

    @Test
    fun aSkippedLastMove_stillEndsTheStrokeWhereThePointerLifted() {
        val ink = drawStaircase(2.dp).allPoints
        assertEquals(Offset(20f + STEPS / 2, 100f + STEPS / 2), ink.last())
    }

    @Test
    fun theDefaultOnDesktop_is2Dp() {
        assertEquals(2.dp, SignaturePadDefaults.minPointDistance)
    }
}

private const val STEPS = 60
