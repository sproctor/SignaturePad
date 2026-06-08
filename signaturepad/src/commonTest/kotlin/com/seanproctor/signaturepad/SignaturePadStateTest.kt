package com.seanproctor.signaturepad

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * State-machine behavior that doesn't depend on a rendering backend, so it runs on every target.
 * Drawing/accumulation behavior is verified in jvmTest where a real [androidx.compose.ui.graphics.Paint]
 * is available.
 */
class SignaturePadStateTest {

    @Test
    fun signatureStarted_isFalse_initially() {
        val state = SignaturePadStateImpl()
        assertFalse(state.signatureStarted.value)
    }

    @Test
    fun gestureStarted_marksSignatureAsStarted() {
        val state = SignaturePadStateImpl()
        state.gestureStarted(Offset(1f, 1f))
        assertTrue(state.signatureStarted.value)
    }

    @Test
    fun clear_resetsStartedFlag() {
        val state = SignaturePadStateImpl()
        state.gestureStarted(Offset(0f, 0f))
        state.gestureMoved(Offset(10f, 10f))

        state.clear()

        assertFalse(state.signatureStarted.value)
    }

    @Test
    fun setSize_withSameSize_keepsSignature() {
        val state = SignaturePadStateImpl()
        state.setSize(100, 100)
        state.gestureStarted(Offset(0f, 0f))

        state.setSize(100, 100)

        assertTrue(state.signatureStarted.value, "resizing to the same size must not clear")
    }

    @Test
    fun setSize_withDifferentSize_clearsSignatureByDefault() {
        val state = SignaturePadStateImpl()
        state.setSize(100, 100)
        state.gestureStarted(Offset(0f, 0f))

        state.setSize(200, 150)

        assertFalse(state.signatureStarted.value, "the default behavior must clear on resize")
    }

    @Test
    fun setSize_withNonClearBehavior_keepsSignature() {
        for (behavior in listOf(ResizeBehavior.Center, ResizeBehavior.Fit, ResizeBehavior.Stretch)) {
            val state = SignaturePadStateImpl(behavior)
            state.setSize(100, 100)
            state.gestureStarted(Offset(0f, 0f))

            state.setSize(200, 150)

            assertTrue(state.signatureStarted.value, "$behavior must keep the signature on resize")
        }
    }

    @Test
    fun setSize_firstLayoutPass_doesNotRemap() {
        var invoked = false
        val custom = ResizeBehavior.Custom { point, _, _ -> invoked = true; point }
        val state = SignaturePadStateImpl(custom)

        // Going from the initial 0x0 to the first real size has nothing to remap.
        state.setSize(100, 100)

        assertFalse(invoked, "the custom mapper must not run on the first layout pass")
    }

    @Test
    fun setSize_withCustomBehavior_receivesOldAndNewSizes() {
        var seenOld: Size? = null
        var seenNew: Size? = null
        val custom = ResizeBehavior.Custom { point, oldSize, newSize ->
            seenOld = oldSize
            seenNew = newSize
            point
        }
        val state = SignaturePadStateImpl(custom)
        state.setSize(100, 100)
        // Draw enough points for at least one bezier so the mapper has something to transform.
        state.gestureStarted(Offset(10f, 10f))
        state.gestureMoved(Offset(20f, 20f))
        state.gestureMoved(Offset(30f, 30f))
        state.gestureMoved(Offset(40f, 40f))

        state.setSize(200, 150)

        assertEquals(Size(100f, 100f), seenOld)
        assertEquals(Size(200f, 150f), seenNew)
    }
}
