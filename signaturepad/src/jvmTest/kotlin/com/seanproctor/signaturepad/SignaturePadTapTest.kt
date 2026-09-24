package com.seanproctor.signaturepad

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SignaturePadTapTest {

    private fun SignaturePadState.strokes(): List<List<Offset>> =
        RecordingCanvas().also { drawSignature(it, Color.Black, 3f) }.drawnStrokes

    // A dot is a stroke whose points all sit on the same spot.
    private fun List<Offset>.isDotAt(point: Offset) = isNotEmpty() && all { it == point }

    private fun ComposeUiTest.touch(input: TouchInjectionScope.() -> Unit): SignaturePadState {
        val state = SignaturePadStateImpl()
        setContent {
            SignaturePad(state, Color.Black, 3.dp, Modifier.size(200.dp).testTag("pad"))
        }
        onNodeWithTag("pad").performTouchInput(input)
        waitForIdle()
        return state
    }

    @Test
    fun zeroLengthCurve_isDrawnAsADot() {
        val canvas = RecordingCanvas()

        Bezier(Offset.Zero, Offset.Zero, Offset.Zero, Offset.Zero).draw(canvas, Paint())

        assertEquals(listOf(listOf(Offset.Zero)), canvas.drawnStrokes)
    }

    @Test
    fun gestureWithoutMoving_drawsADot() {
        val state = SignaturePadStateImpl()
        state.setSize(100, 100)

        state.gestureStarted(Offset(40f, 60f))
        state.gestureEnded()

        val strokes = state.strokes()
        assertEquals(1, strokes.size)
        assertTrue(strokes[0].isDotAt(Offset(40f, 60f)))
    }

    @Test
    fun dot_survivesSaveAndRestore() {
        val state = SignaturePadStateImpl()
        state.setSize(100, 100)
        state.gestureStarted(Offset(40f, 60f))
        state.gestureEnded()

        val saver = SignaturePadStateSaver()
        val saved = with(saver) { SaverScope { true }.save(state) }
        val restored = saver.restore(saved!!)!!

        val strokes = restored.strokes()
        assertEquals(1, strokes.size)
        assertTrue(strokes[0].isDotAt(Offset(40f, 60f)))
    }

    @Test
    fun tap_drawsADot() = runComposeUiTest {
        val state = touch { click(Offset(100f, 100f)) }

        assertTrue(state.signatureStarted.value)
        val strokes = state.strokes()
        assertEquals(1, strokes.size)
        assertTrue(strokes[0].isDotAt(Offset(100f, 100f)))
    }

    @Test
    fun markShorterThanTheTouchSlop_drawsADot() = runComposeUiTest {
        val state = touch {
            down(Offset(100f, 100f))
            moveTo(Offset(104f, 101f))
            up()
        }

        assertTrue(state.signatureStarted.value)
        val strokes = state.strokes()
        assertEquals(1, strokes.size)
        // Taps are reported where the finger lifted.
        assertTrue(strokes[0].isDotAt(Offset(104f, 101f)))
    }

    @Test
    fun swipe_isNotAlsoTreatedAsATap() = runComposeUiTest {
        val state = touch { swipe(Offset(20f, 100f), Offset(180f, 100f), 300) }

        val strokes = state.strokes()
        assertTrue(strokes.isNotEmpty())
        assertTrue(strokes.none { it.isDotAt(it.first()) }, "the swipe also drew a dot")
    }
}
