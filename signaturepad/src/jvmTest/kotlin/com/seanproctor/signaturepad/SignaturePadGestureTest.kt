package com.seanproctor.signaturepad

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    @Test
    fun liftingTheFirstFingerWhileASecondIsDown_doesNotDrawOverToIt() = runComposeUiTest {
        val state = SignaturePadStateImpl()
        setContent {
            SignaturePad(state, Color.Black, 3.dp, Modifier.size(200.dp).testTag("pad"))
        }

        onNodeWithTag("pad").performTouchInput {
            down(0, Offset(20f, 100f))
            moveTo(0, Offset(60f, 100f))
            moveTo(0, Offset(100f, 100f))
            // A second finger (or a palm) lands, then the first one lifts.
            down(1, Offset(160f, 180f))
            up(0)
            moveTo(1, Offset(170f, 180f))
            moveTo(1, Offset(180f, 180f))
            // The second finger also moves in its up event.
            updatePointerTo(1, Offset(190f, 180f))
            up(1)
        }
        waitForIdle()

        val ink = RecordingCanvas().also { state.drawSignature(it, Color.Black, 3f) }.allPoints
        assertEquals(100f, ink.maxOf { it.x }, 0.5f, "the first finger's stroke doesn't end where it lifted")
        assertEquals(100f, ink.maxOf { it.y }, 0.5f, "a line was drawn over to the second finger")
    }

    @Test
    fun secondFingerLiftingWithoutMoving_finishesTheFirstFingersStroke() = runComposeUiTest {
        val state = SignaturePadStateImpl()
        setContent {
            SignaturePad(state, Color.Black, 3.dp, Modifier.size(200.dp).testTag("pad"))
        }

        onNodeWithTag("pad").performTouchInput {
            down(0, Offset(20f, 100f))
            moveTo(0, Offset(60f, 100f))
            moveTo(0, Offset(100f, 100f))
            down(1, Offset(160f, 180f))
            up(0)
            // The drag ends on the second finger's up, with no move from it in between.
            up(1)
        }
        waitForIdle()

        val ink = RecordingCanvas().also { state.drawSignature(it, Color.Black, 3f) }.allPoints
        assertEquals(100f, ink.maxOf { it.x }, 0.5f, "the first finger's last segment is missing")
    }

    @Test
    fun dragStartedByASecondFingerAfterTheFirstLifted_drawsNothing() = runComposeUiTest {
        val state = SignaturePadStateImpl()
        setContent {
            SignaturePad(state, Color.Black, 3.dp, Modifier.size(200.dp).testTag("pad"))
        }

        onNodeWithTag("pad").performTouchInput {
            // The first finger lifts before moving far enough to start a drag, and the second one
            // starts it instead.
            down(0, Offset(20f, 100f))
            down(1, Offset(100f, 150f))
            up(0)
            moveTo(1, Offset(140f, 150f))
            moveTo(1, Offset(180f, 150f))
            up(1)
        }
        waitForIdle()

        val ink = RecordingCanvas().also { state.drawSignature(it, Color.Black, 3f) }.allPoints
        assertEquals(emptyList(), ink, "a stroke was drawn for a drag the first finger didn't make")
    }

    @Test
    fun disablingThePadMidStroke_keepsTheStrokeSoFar() = runComposeUiTest {
        val state = SignaturePadStateImpl()
        var enabled by mutableStateOf(true)
        setContent {
            SignaturePad(state, Color.Black, 3.dp, Modifier.size(200.dp).testTag("pad"), enabled = enabled)
        }
        onNodeWithTag("pad").performTouchInput {
            down(Offset(20f, 100f))
            moveTo(Offset(60f, 100f))
            moveTo(Offset(100f, 100f))
            moveTo(Offset(140f, 100f))
        }
        waitForIdle()

        // Restarts pointer input while the finger is still down.
        enabled = false
        waitForIdle()

        val ink = RecordingCanvas().also { state.drawSignature(it, Color.Black, 3f) }.allPoints
        assertEquals(140f, ink.maxOf { it.x }, 0.5f, "the last segment before disabling is missing")
    }
}
