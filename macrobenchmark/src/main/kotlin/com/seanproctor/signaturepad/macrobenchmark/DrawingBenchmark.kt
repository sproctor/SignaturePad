package com.seanproctor.signaturepad.macrobenchmark

import android.content.Intent
import android.graphics.Point
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMacrobenchmarkApi
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingGfxInfoMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Measures frames while drawing on top of a signature that already has [curves] curves. Every
 * stroke added redraws the whole signature, so this is the cost of drawing a signature of that
 * size, frame after frame.
 */
@RunWith(Parameterized::class)
class DrawingBenchmark(private val curves: Int) {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @OptIn(ExperimentalMetricApi::class, ExperimentalMacrobenchmarkApi::class)
    @Test
    fun drawOnSignature() = rule.measureRepeated(
        packageName = PACKAGE,
        // FrameTimingMetric finds frames by the RenderThread's name, which some devices (Lenovo's,
        // for one) leave out of the trace. These don't depend on thread names.
        metrics = listOf(
            FrameTimingGfxInfoMetric(),
            TraceSectionMetric(
                sectionName = "Choreographer#doFrame %",
                label = "uiThreadFrame",
                mode = TraceSectionMetric.Mode.Average,
            ),
            // Some devices name it "DrawFrames".
            TraceSectionMetric(
                sectionName = "DrawFrame%",
                label = "renderThreadFrame",
                mode = TraceSectionMetric.Mode.Average,
            ),
        ),
        // Compiled once for the whole run, in compileApp(), rather than before every test.
        compilationMode = CompilationMode.Ignore(),
        // No startup mode: a cold start asks the app to drop its shader cache, and some devices
        // (Lenovo's, for one) don't deliver that broadcast. Drawing doesn't need a cold start, so
        // the setup restarts the app itself.
        iterations = 3,
        setupBlock = {
            killProcess()
            startActivityAndWait { intent: Intent ->
                intent.setClassName(PACKAGE, "$PACKAGE.BenchmarkActivity")
                intent.putExtra("curves", curves)
            }
            check(device.wait(Until.hasObject(By.res("pad").desc("ready")), 30_000)) {
                "the signature wasn't loaded"
            }
        },
    ) {
        val pad = device.findObject(By.res("pad")).visibleBounds
        // One zigzag stroke across the middle of the pad, about a second long.
        val y = pad.centerY()
        val points = Array(ZIGZAGS + 1) { i ->
            Point(
                pad.left + pad.width() * (i + 1) / (ZIGZAGS + 2),
                y + if (i % 2 == 0) -pad.height() / 10 else pad.height() / 10,
            )
        }
        device.swipe(points, STEPS_PER_ZIGZAG)
        device.waitForIdle()
    }

    companion object {
        private const val PACKAGE = "com.seanproctor.signaturepad.androiddemo"
        private const val ZIGZAGS = 20

        // UiAutomator moves the pointer every 5ms, so each zigzag takes about 50ms.
        private const val STEPS_PER_ZIGZAG = 10

        // 1000 curves is around where drawing the whole signature every frame stops keeping up at
        // 60fps on a low-end device, and 4000 is a long signature.
        @JvmStatic
        @Parameterized.Parameters(name = "curves={0,number,#}")
        fun parameters() = listOf(1000, 4000)

        // Ahead-of-time compiles the app once. CompilationMode.Full would recompile it before every
        // test, which takes about 15 seconds on a low-end device.
        @JvmStatic
        @BeforeClass
        fun compileApp() {
            val output = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
                .executeShellCommand("cmd package compile -f -m speed $PACKAGE")
            check(output.trim() == "Success") { "couldn't compile $PACKAGE: $output" }
        }
    }
}
