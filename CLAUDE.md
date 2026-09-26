# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Compose Signature Pad — a Kotlin Multiplatform signature capture library for Compose. Targets: Android, JVM, JS, WasmJS, iOS (arm64, simulator arm64).

## Build Commands

```bash
./gradlew build              # Build and test all modules
./gradlew allTests           # Run tests for all targets
./gradlew check              # Run all checks including lint
./gradlew lintFix            # Run lint with auto-fix
./gradlew :signaturepad:jvmTest # Fastest test run; most tests live in jvmTest
./gradlew :benchmark:benchmark  # Run JMH benchmarks (reports in benchmark/build/reports/benchmarks)
./gradlew :macrobenchmark:connectedBenchmarkAndroidTest  # Frame timing on a connected Android device
```

Target-specific tasks follow Gradle KMP conventions (e.g., `jvmTest`, `compileKotlinJvm`). CI runs `./gradlew :signaturepad:allTests` on macOS.

## Architecture

Five modules:
- **signaturepad** — the library, 100% common code (`src/commonMain/kotlin/com/seanproctor/signaturepad/`). Four files: `SignaturePad.kt` (composable and gesture handling), `SignaturePadState.kt` (state interface + impl, saver, `remember` functions), `ResizeBehavior.kt` (how a signature is remapped when the pad resizes), `Bezier.kt` (internal curve math). Uses `explicitApi()` mode.
- **demo** — KMP demo with Android, JVM and JS targets. Platform entry points in `jvmMain` and `jsMain`, shared UI in `commonMain/SignatureBox.kt`.
- **androidDemo** — Android demo app (standalone Android module, not KMP) that shows the shared UI from `:demo`.
- **benchmark** — JVM-only kotlinx-benchmark (JMH) suite for `SignaturePadStateImpl`: capturing, drawing, resizing and exporting a synthetic signature.
- **macrobenchmark** — Jetpack Macrobenchmark for `androidDemo` on a real device: frame times while drawing on top of a signature of 1,000 or 4,000 curves. It drives `BenchmarkActivity`, which only exists in `androidDemo`'s `benchmark` build type (`src/benchmark`). Takes about 2 minutes. Over wireless adb, Gradle's test runner can hang or uninstall the app mid-run; installing both `benchmark` APKs and running `adb shell am instrument -w -e class com.seanproctor.signaturepad.macrobenchmark.DrawingBenchmark com.seanproctor.signaturepad.macrobenchmark/androidx.test.runner.AndroidJUnitRunner` is more reliable. Results go to `/sdcard/Android/media/com.seanproctor.signaturepad.macrobenchmark/`.

### Drawing

- Each touch point extends the stroke with a cubic `Bezier`. Its control points are smoothed from the neighboring points. A stroke's first point is buffered twice so its first segment gets drawn, and the last segment is added when the stroke ends.
- `pathOf` joins every curve into one `Path`, starting a new contour where `Bezier.startsStroke` is set. `drawSignature` and `drawOnBitmap` draw it with a single `drawPath`, using a stroke paint with round caps and joins.
- A tap is a zero-length curve, which the round cap draws as a dot.
- `SignaturePad` doesn't redraw the whole signature on each move. `SignaturePadStateImpl` tracks which curves belong to the stroke in progress (`strokeCurveCount`). The finished strokes are recorded into an offscreen `GraphicsLayer` inside `drawWithCache`, which is only re-recorded when `finishedStrokesVersion` changes (in `resetStroke`: a stroke ends, or the signature is cleared, resized or restored). Each frame draws that layer plus the stroke in progress. With a translucent pen, both are drawn opaque inside a `saveLayer` carrying the pen's alpha, so crossings are blended once, as with a single path. Any other `SignaturePadState` implementation is drawn with `drawSignature` every frame.

### Tests

Most tests are in `signaturepad/src/jvmTest`. `RecordingCanvas` is a fake `Canvas` that records each drawn curve as points sampled along it, so tests can check ink without a real renderer. Anything that depends on how Skia renders, like a tap's dot, draws to an `ImageBitmap` and checks the pixels.

## Key Conventions

- Gradle 9.6.1 with Kotlin DSL, version catalog at `gradle/libs.versions.toml`
- JVM toolchain: Java 17
- Library version defined in `signaturepad/build.gradle.kts` (`version = "2.3.1"`)
- Publishing: Maven Central via vanniktech-maven-publish plugin with GPG signing
- Android: compileSdk 37, minSdk 23
