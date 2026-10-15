package com.seanproctor.signaturepad

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** [SignaturePadState.signature] gets a new value when the finished strokes change, and only then. */
class SignatureTest {

    private val state = SignaturePadStateImpl(ResizeBehavior.Fit).apply { setSize(100, 100) }

    private val strokeA = listOf(Offset(10f, 10f), Offset(20f, 30f), Offset(30f, 20f), Offset(40f, 40f))
    private val strokeB = listOf(Offset(50f, 50f), Offset(60f, 40f), Offset(70f, 60f), Offset(80f, 50f))

    private fun stroke(points: List<Offset>) {
        state.gestureStarted(points.first())
        points.drop(1).forEach { state.gestureMoved(it) }
        state.gestureEnded()
    }

    @Test
    fun signature_isEmptyInitially() {
        assertTrue(SignaturePadStateImpl().signature.isEmpty)
    }

    @Test
    fun endingAStroke_publishesIt() {
        stroke(strokeA)

        assertFalse(state.signature.isEmpty)
    }

    @Test
    fun theStrokeInProgress_isPublishedWhenItEnds() {
        stroke(strokeA)
        val before = state.signature

        state.gestureStarted(strokeB.first())
        strokeB.drop(1).forEach { state.gestureMoved(it) }
        assertSame(before, state.signature, "the stroke in progress was published")

        state.gestureEnded()
        assertNotSame(before, state.signature)
        assertTrue(state.signature.curves.size > before.curves.size)
    }

    @Test
    fun aSignature_neverChanges() {
        stroke(strokeA)
        val signature = state.signature
        val curves = signature.curves.toList()

        stroke(strokeB)
        state.setSize(50, 50)
        state.clear()

        assertEquals(curves, signature.curves)
    }

    @Test
    fun aStrokeThatDrawsNothing_keepsTheSignature() {
        stroke(strokeA)
        val before = state.signature

        stroke(listOf(Offset(-10f, 20f), Offset(-20f, 50f), Offset(-5f, 110f)))

        assertSame(before, state.signature)
    }

    @Test
    fun clear_publishesAnEmptySignature() {
        stroke(strokeA)

        state.clear()

        assertTrue(state.signature.isEmpty)
    }

    @Test
    fun clearingAnEmptyPad_keepsTheSignature() {
        val before = state.signature

        state.clear()

        assertSame(before, state.signature)
    }

    @Test
    fun resizing_publishesTheRemappedSignature() {
        stroke(strokeA)
        val before = state.signature

        state.setSize(200, 200)

        assertNotSame(before, state.signature)
        assertEquals(before.curves.size, state.signature.curves.size)
    }

    @Test
    fun resizingAnEmptyPad_keepsTheSignature() {
        val before = state.signature

        state.setSize(200, 200)

        assertSame(before, state.signature)
    }

    @Test
    fun restoring_publishesTheRestoredSignature() {
        stroke(strokeA)

        val restored = SignaturePadStateImpl().apply { restoreFromFloatList(state.toFloatList()) }

        assertEquals(state.signature.curves.size, restored.signature.curves.size)
    }

    @Test
    fun toImageBitmap_ofAnEmptySignature_isNull() {
        assertNull(state.signature.toImageBitmap(Color.Black, 3f), "a pad that was never drawn on")

        stroke(strokeA)
        state.clear()
        state.setSize(200, 200)
        assertNull(state.signature.toImageBitmap(Color.Black, 3f), "a cleared pad, resized since")
    }

    @Test
    fun aSignature_hasThePadSize_unlessItsEmpty() {
        stroke(strokeA)
        assertEquals(100 to 100, state.signature.width to state.signature.height)

        state.setSize(200, 150)
        assertEquals(200 to 150, state.signature.width to state.signature.height)

        state.clear()
        assertEquals(0 to 0, state.signature.width to state.signature.height)
    }

    @Test
    fun aTapBeforeTheFirstLayout_drawsNothing() {
        val state = SignaturePadStateImpl()
        state.gestureStarted(Offset.Zero)
        state.gestureEnded()

        state.setSize(100, 100)

        assertTrue(state.signature.isEmpty)
    }

    @Test
    fun movesDuringAStroke_dontInvalidateReadersOfTheSignature() {
        stroke(strokeA)
        state.gestureStarted(strokeB.first())
        Snapshot.sendApplyNotifications()
        // Observes reads the way composition and drawWithCache do.
        val observer = SnapshotStateObserver { it() }.apply { start() }
        var invalidated = false
        try {
            observer.observeReads(this, onValueChangedForScope = { invalidated = true }) { state.signature }

            strokeB.drop(1).forEach { state.gestureMoved(it) }
            Snapshot.sendApplyNotifications()
            assertFalse(invalidated, "a move during the stroke invalidated a reader")

            state.gestureEnded()
            Snapshot.sendApplyNotifications()
            assertTrue(invalidated, "the stroke ending didn't invalidate a reader")
        } finally {
            observer.stop()
        }
    }
}
