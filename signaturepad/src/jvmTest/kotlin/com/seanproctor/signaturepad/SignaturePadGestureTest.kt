package com.seanproctor.signaturepad

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SignaturePadGestureTest {

    @Test
    fun swipe_inksFromTouchDownToLift() = runComposeUiTest {
        val state = SignaturePadStateImpl()
        setContent {
            SignaturePad(state, Color.Black, 3.dp, Modifier.size(200.dp).testTag("pad"))
        }

        onNodeWithTag("pad").performTouchInput { swipe(Offset(20f, 100f), Offset(180f, 100f), 300) }
        waitForIdle()

        val ink = RecordingCanvas().also { state.drawSignature(it, Color.Black, 3f) }.allPoints
        assertEquals(20f, ink.minOf { it.x }, 0.5f, "the start of the stroke is missing")
        assertEquals(180f, ink.maxOf { it.x }, 0.5f, "the end of the stroke is missing")
    }

    @Test
    fun liftingAfterAMoveInTheUpEvent_inksToTheLiftPoint() = runComposeUiTest {
        val state = SignaturePadStateImpl()
        setContent {
            SignaturePad(state, Color.Black, 3.dp, Modifier.size(200.dp).testTag("pad"))
        }

        onNodeWithTag("pad").performTouchInput {
            down(Offset(20f, 100f))
            moveTo(Offset(100f, 100f))
            // Moves the pointer without a move event, so the movement arrives with the up event.
            updatePointerTo(0, Offset(180f, 100f))
            up()
        }
        waitForIdle()

        val ink = RecordingCanvas().also { state.drawSignature(it, Color.Black, 3f) }.allPoints
        assertEquals(180f, ink.maxOf { it.x }, 0.5f, "the movement in the up event is missing")
    }
}
