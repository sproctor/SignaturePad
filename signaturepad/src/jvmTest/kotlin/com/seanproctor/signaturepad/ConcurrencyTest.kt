package com.seanproctor.signaturepad

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import java.util.Collections
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Apps can call [SignaturePadStateImpl] from any thread while the pad takes input and draws on the
 * main thread. These run input, clearing, resizing, drawing and exporting on separate threads at
 * once, and check that nothing throws and the state is consistent afterwards.
 */
class ConcurrencyTest {

    private val runMillis = 1_000L

    private companion object {
        const val MAX_STROKES = 2_000
    }

    // Runs each task on its own thread, repeatedly, until the time is up, and returns what they threw.
    private fun race(vararg tasks: (iteration: Int) -> Unit): List<Throwable> {
        val failures = Collections.synchronizedList(mutableListOf<Throwable>())
        val start = CountDownLatch(1)
        val deadline = System.nanoTime() + runMillis * 1_000_000
        val threads = tasks.map { task ->
            // A daemon, so a thread stuck in the state can't keep the test JVM from exiting.
            thread(isDaemon = true) {
                start.await()
                var i = 0
                try {
                    while (System.nanoTime() < deadline) task(i++)
                } catch (t: Throwable) {
                    failures.add(t)
                }
            }
        }
        start.countDown()
        // A deadlock in the state would hang the test run rather than fail it, so give up on a thread
        // that hasn't stopped long after the time is up.
        threads.forEach {
            it.join(runMillis + 10_000)
            if (it.isAlive) failures.add(AssertionError("${it.name} didn't stop: the state may be deadlocked"))
        }
        return failures
    }

    // Draws one stroke per call, up to a limit, so the signature doesn't grow without bound while the
    // other threads run.
    private fun SignaturePadStateImpl.scribble(i: Int, yOffset: Int = 0) {
        if (i >= MAX_STROKES) return Thread.yield()
        val y = 10f + ((i + yOffset) % 80)
        gestureStarted(Offset(5f, y))
        for (x in 10..190 step 10) gestureMoved(Offset(x.toFloat(), y + (x % 20)))
        gestureEnded()
    }

    private fun SignaturePadStateImpl.drawAll() {
        drawSignature(RecordingCanvas(), Color.Black, 3f)
        drawFinishedStrokes(RecordingCanvas(), Color.Black, 3f)
        drawStrokeInProgress(RecordingCanvas(), Color.Black, 3f)
    }

    @Test
    fun inputClearAndDraw_onSeparateThreads() {
        val state = SignaturePadStateImpl(ResizeBehavior.Fit).apply { setSize(200, 100) }

        val failures = race(
            { i -> state.scribble(i) },
            { i -> if (i % 4 == 0) state.clear() else Thread.yield() },
            { _ -> state.drawAll() },
        )

        assertEquals(emptyList(), failures)
        state.assertConsistent()
    }

    @Test
    fun inputResizeAndExport_onSeparateThreads() {
        val state = SignaturePadStateImpl(ResizeBehavior.Fit).apply { setSize(200, 100) }
        val bitmap = ImageBitmap(100, 50)

        val failures = race(
            { i -> state.scribble(i) },
            { i -> if (i % 2 == 0) state.setSize(400, 200) else state.setSize(200, 100) },
            { _ -> state.drawAll() },
            { _ -> state.drawOnBitmap(bitmap, Color.Black, 2f) },
            { _ -> state.signature.toImageBitmap(Color.Black, 2f) },
            { _ -> SignaturePadStateImpl(ResizeBehavior.Fit).restoreFromFloatList(state.toFloatList()) },
        )

        assertEquals(emptyList(), failures)
        state.assertConsistent()
    }

    @Test
    fun twoInputThreads_atOnce() {
        val state = SignaturePadStateImpl(ResizeBehavior.Fit).apply { setSize(200, 100) }

        val failures = race(
            { i -> state.scribble(i) },
            { i -> state.scribble(i, yOffset = 40) },
            { _ -> state.drawAll() },
        )

        assertEquals(emptyList(), failures)
        state.assertConsistent()
    }

    @Test
    fun aChangeInsideASnapshot_isSeenByOtherCallsRightAway() {
        val state = SignaturePadStateImpl(ResizeBehavior.Fit).apply { setSize(200, 100) }
        state.scribble(0)
        val curvesPerStroke = state.curveCount()

        // As if the app cleared the pad inside Snapshot.withMutableSnapshot, and the user started the
        // next stroke before that snapshot applied.
        val snapshot = Snapshot.takeMutableSnapshot()
        try {
            snapshot.enter { state.clear() }
            state.scribble(1)
            snapshot.apply().check()
        } finally {
            snapshot.dispose()
        }

        assertEquals(curvesPerStroke, state.curveCount(), "the clear() or the stroke after it was lost")
    }

    // toFloatList() stores a 3-float header, then 9 floats per curve.
    private fun SignaturePadStateImpl.curveCount() = (toFloatList().size - 3) / 9

    // Once every thread has stopped, the finished strokes and the stroke in progress together are the
    // whole signature, and a save and restore keeps all of it.
    private fun SignaturePadStateImpl.assertConsistent() {
        fun draw(block: (RecordingCanvas) -> Unit) = RecordingCanvas().also(block).drawnStrokes
        val all = draw { drawSignature(it, Color.Black, 3f) }
        val finished = draw { drawFinishedStrokes(it, Color.Black, 3f) }
        val inProgress = draw { drawStrokeInProgress(it, Color.Black, 3f) }
        assertEquals(all, finished + inProgress)
        assertEquals(finished.size, signature.curves.size, "the signature isn't the finished strokes")
        assertTrue(inProgress.isEmpty(), "every gesture ended, but a stroke is still in progress")

        val saved = toFloatList()
        val restored = SignaturePadStateImpl(ResizeBehavior.Fit).apply { restoreFromFloatList(saved) }
        assertEquals(all.size, draw { restored.drawSignature(it, Color.Black, 3f) }.size)
    }
}
