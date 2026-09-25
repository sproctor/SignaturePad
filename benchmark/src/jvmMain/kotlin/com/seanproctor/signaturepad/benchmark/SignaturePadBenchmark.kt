package com.seanproctor.signaturepad.benchmark

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.seanproctor.signaturepad.ResizeBehavior
import com.seanproctor.signaturepad.SignaturePadState
import com.seanproctor.signaturepad.SignaturePadStateImpl
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Param
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlin.math.sin
import kotlin.random.Random

private const val PAD_WIDTH = 800
private const val PAD_HEIGHT = 300
private const val PEN_WIDTH = 6f

// Each stroke produces one curve per move, so a signature has about as many curves as moves.
private const val MOVES_PER_STROKE = 100

@State(Scope.Benchmark)
class SignaturePadBenchmark {
    /** The number of curves in the signature. A typical signature has a few hundred. */
    @Param("250", "1000", "4000")
    var segments: Int = 0

    private lateinit var strokes: List<List<Offset>>
    private lateinit var signature: SignaturePadState
    private lateinit var screenCanvas: Canvas
    private lateinit var exportBitmap: ImageBitmap
    private val countingCanvas = CountingCanvas()
    private var shrunk = false

    @Setup
    fun setUp() {
        strokes = generateStrokes(segments / MOVES_PER_STROKE)
        signature = sign(strokes)
        screenCanvas = Canvas(ImageBitmap(PAD_WIDTH, PAD_HEIGHT))
        exportBitmap = ImageBitmap(PAD_WIDTH * 2, PAD_HEIGHT * 2)
    }

    /** Captures a whole signature from its gesture events, without drawing it. */
    @Benchmark
    fun capture(): SignaturePadState = sign(strokes)

    /** Produces one frame: the pad's draw-phase work, without rendering. */
    @Benchmark
    fun drawFrame(): Long {
        signature.drawSignature(countingCanvas, Color.Black, PEN_WIDTH)
        return countingCanvas.pointCount + countingCanvas.pathCount
    }

    /** Produces one frame and renders it to a Skia raster canvas. */
    @Benchmark
    fun drawAndRenderFrame() {
        signature.drawSignature(screenCanvas, Color.Black, PEN_WIDTH)
    }

    /** Resizes the pad, switching between two widths, then produces the next frame. */
    @Benchmark
    fun resizeAndDrawFrame(): Long {
        shrunk = !shrunk
        signature.setSize(if (shrunk) PAD_WIDTH * 3 / 4 else PAD_WIDTH, PAD_HEIGHT)
        signature.drawSignature(countingCanvas, Color.Black, PEN_WIDTH)
        return countingCanvas.pointCount + countingCanvas.pathCount
    }

    /** Exports the signature to a bitmap twice the pad's size. */
    @Benchmark
    fun exportToBitmap() {
        signature.drawOnBitmap(exportBitmap, Color.Black, PEN_WIDTH * 2)
    }
}

private fun sign(strokes: List<List<Offset>>): SignaturePadState =
    SignaturePadStateImpl(ResizeBehavior.Fit).apply {
        setSize(PAD_WIDTH, PAD_HEIGHT)
        for (stroke in strokes) {
            gestureStarted(stroke.first())
            for (i in 1 until stroke.size) {
                gestureMoved(stroke[i])
            }
            gestureEnded()
        }
    }

/**
 * Generates [count] looping, cursive-like strokes that stay inside the pad. Consecutive points are up
 * to about 20px apart, like a quick signature sampled at 60Hz. Seeded, so every run draws the same
 * signature.
 */
private fun generateStrokes(count: Int): List<List<Offset>> {
    val random = Random(42)
    return List(count) {
        val startX = 60f + random.nextFloat() * 300f
        val phase = random.nextFloat() * 6f
        List(MOVES_PER_STROKE + 1) { i ->
            Offset(
                x = startX + i * 3f + 30f * sin(i * 0.3f + phase),
                y = PAD_HEIGHT / 2f + 90f * sin(i * 0.17f + phase * 2f),
            )
        }
    }
}
