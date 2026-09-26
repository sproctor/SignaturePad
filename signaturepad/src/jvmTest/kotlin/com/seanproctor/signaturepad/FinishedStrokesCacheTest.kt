package com.seanproctor.signaturepad

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [SignaturePad] caches the finished strokes and redraws only the stroke in progress on each move.
 * That relies on [SignaturePadStateImpl.drawFinishedStrokes] leaving out the stroke in progress,
 * and only being invalidated when the finished strokes change.
 */
class FinishedStrokesCacheTest {

    private val state = SignaturePadStateImpl(ResizeBehavior.Fit).apply { setSize(100, 100) }

    private val applyObserver = Snapshot.registerApplyObserver { changed, _ ->
        if (changed.any { it in finishedReads }) finishedInvalidated = true
    }
    private val finishedReads = mutableSetOf<Any>()
    private var finishedInvalidated = false

    @AfterTest
    fun disposeObserver() = applyObserver.dispose()

    private fun finished() = RecordingCanvas().also {
        state.drawFinishedStrokes(it, Color.Black, 3f)
    }.drawnStrokes

    private fun inProgress() = RecordingCanvas().also {
        state.drawStrokeInProgress(it, Color.Black, 3f)
    }.drawnStrokes

    private fun all() = RecordingCanvas().also { state.drawSignature(it, Color.Black, 3f) }.drawnStrokes

    // Draws the finished strokes the way the pad's cache does, noting what it reads.
    private fun cacheFinishedStrokes() {
        // Deliver the changes made so far, so they aren't mistaken for ones made after caching.
        Snapshot.sendApplyNotifications()
        finishedReads.clear()
        Snapshot.observe(readObserver = { finishedReads.add(it) }) { finished() }
        finishedInvalidated = false
    }

    private fun invalidatedBy(change: () -> Unit): Boolean {
        change()
        Snapshot.sendApplyNotifications()
        return finishedInvalidated
    }

    private fun stroke(points: List<Offset>) {
        state.gestureStarted(points.first())
        points.drop(1).forEach { state.gestureMoved(it) }
        state.gestureEnded()
    }

    @Test
    fun finishedAndInProgress_drawTheWholeSignature() {
        stroke(listOf(Offset(10f, 10f), Offset(20f, 30f), Offset(30f, 20f), Offset(40f, 40f)))
        state.gestureStarted(Offset(50f, 50f))
        listOf(Offset(60f, 40f), Offset(70f, 60f), Offset(80f, 50f)).forEach { state.gestureMoved(it) }

        assertTrue(finished().isNotEmpty() && inProgress().isNotEmpty(), "precondition: both parts have curves")
        assertEquals(all(), finished() + inProgress())
    }

    @Test
    fun endingAStroke_movesItToTheFinishedStrokes() {
        state.gestureStarted(Offset(10f, 10f))
        listOf(Offset(20f, 30f), Offset(30f, 20f), Offset(40f, 40f)).forEach { state.gestureMoved(it) }
        assertTrue(finished().isEmpty())

        state.gestureEnded()

        assertEquals(all(), finished())
        assertTrue(inProgress().isEmpty())
    }

    @Test
    fun drawingAStroke_doesNotInvalidateTheFinishedStrokes() {
        stroke(listOf(Offset(10f, 10f), Offset(20f, 30f), Offset(30f, 20f), Offset(40f, 40f)))
        state.gestureStarted(Offset(50f, 50f))
        cacheFinishedStrokes()

        val invalidated = invalidatedBy {
            listOf(Offset(60f, 40f), Offset(70f, 60f), Offset(80f, 50f), Offset(90f, 70f))
                .forEach { state.gestureMoved(it) }
        }

        assertFalse(invalidated)
    }

    @Test
    fun endingAStroke_invalidatesTheFinishedStrokes() {
        state.gestureStarted(Offset(10f, 10f))
        listOf(Offset(20f, 30f), Offset(30f, 20f), Offset(40f, 40f)).forEach { state.gestureMoved(it) }
        cacheFinishedStrokes()

        assertTrue(invalidatedBy { state.gestureEnded() })
    }

    @Test
    fun clearingAndResizing_invalidateTheFinishedStrokes() {
        stroke(listOf(Offset(10f, 10f), Offset(20f, 30f), Offset(30f, 20f), Offset(40f, 40f)))

        cacheFinishedStrokes()
        assertTrue(invalidatedBy { state.setSize(50, 50) }, "resizing")

        cacheFinishedStrokes()
        assertTrue(invalidatedBy { state.clear() }, "clearing")
    }
}
