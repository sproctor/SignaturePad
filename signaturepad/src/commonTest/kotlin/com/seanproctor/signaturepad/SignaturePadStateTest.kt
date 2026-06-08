package com.seanproctor.signaturepad

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
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
    fun setSize_withDifferentSize_clearsSignature() {
        val state = SignaturePadStateImpl()
        state.setSize(100, 100)
        state.gestureStarted(Offset(0f, 0f))

        state.setSize(200, 150)

        assertFalse(state.signatureStarted.value, "resizing to a new size must clear")
    }
}
