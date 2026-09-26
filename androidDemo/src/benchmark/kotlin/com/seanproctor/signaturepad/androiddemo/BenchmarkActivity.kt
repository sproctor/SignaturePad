package com.seanproctor.signaturepad.androiddemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.seanproctor.signaturepad.SignaturePad
import com.seanproctor.signaturepad.SignaturePadState
import com.seanproctor.signaturepad.rememberSignaturePadState
import kotlin.math.sin
import kotlin.random.Random

/**
 * A signature pad that starts out holding a generated signature of [EXTRA_CURVES] curves, so
 * :macrobenchmark can measure drawing on top of a signature of a known size without drawing it by
 * hand first. The pad is tagged "pad", and is described as "ready" once the signature is loaded.
 */
class BenchmarkActivity : ComponentActivity() {
    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val curves = intent.getIntExtra(EXTRA_CURVES, 0)
        setContent {
            val state = rememberSignaturePadState()
            var size by remember { mutableStateOf(IntSize.Zero) }
            var ready by remember { mutableStateOf(false) }
            LaunchedEffect(size) {
                if (size != IntSize.Zero && !ready) {
                    state.sign(generateStrokes(curves / MOVES_PER_STROKE, size))
                    ready = true
                }
            }
            Box(
                Modifier.fillMaxSize().semantics { testTagsAsResourceId = true },
                contentAlignment = Alignment.Center,
            ) {
                SignaturePad(
                    state = state,
                    penColor = Color.Black,
                    penWidth = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color(0xFFEEEEEE))
                        .onSizeChanged { size = it }
                        .testTag("pad")
                        .semantics { if (ready) contentDescription = "ready" },
                )
            }
        }
    }

    companion object {
        const val EXTRA_CURVES = "curves"
    }
}

private const val MOVES_PER_STROKE = 100

private fun SignaturePadState.sign(strokes: List<List<Offset>>) {
    for (stroke in strokes) {
        gestureStarted(stroke.first())
        for (i in 1 until stroke.size) gestureMoved(stroke[i])
        gestureEnded()
    }
}

/**
 * Generates [count] looping, cursive-like strokes that fit in a pad of [size], with consecutive
 * points up to about 20px apart. Seeded, so every run draws the same signature. The same shapes as
 * the JVM benchmark's, scaled to the pad's width.
 */
private fun generateStrokes(count: Int, size: IntSize): List<List<Offset>> {
    val random = Random(42)
    val scale = size.width / 800f
    return List(count) {
        val startX = 60f + random.nextFloat() * 300f
        val phase = random.nextFloat() * 6f
        List(MOVES_PER_STROKE + 1) { i ->
            Offset(
                x = (startX + i * 3f + 30f * sin(i * 0.3f + phase)) * scale,
                y = size.height / 2f + 90f * scale * sin(i * 0.17f + phase * 2f),
            )
        }
    }
}
