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
```

Target-specific tasks follow Gradle KMP conventions (e.g., `jvmTest`, `compileKotlinJvm`). CI runs `./gradlew :signaturepad:allTests` on macOS.

## Architecture

Four modules:
- **signaturepad** — the library, 100% common code (`src/commonMain/kotlin/com/seanproctor/signaturepad/`). Four files: `SignaturePad.kt` (composable and gesture handling), `SignaturePadState.kt` (state interface + impl, saver, `remember` functions), `ResizeBehavior.kt` (how a signature is remapped when the pad resizes), `Bezier.kt` (internal curve math). Uses `explicitApi()` mode.
- **demo** — KMP demo with Android, JVM and JS targets. Platform entry points in `jvmMain` and `jsMain`, shared UI in `commonMain/SignatureBox.kt`.
- **androidDemo** — Android demo app (standalone Android module, not KMP) that shows the shared UI from `:demo`.
- **benchmark** — JVM-only kotlinx-benchmark (JMH) suite for `SignaturePadStateImpl`: capturing, drawing, resizing and exporting a synthetic signature.

### Drawing

- Each touch point extends the stroke with a cubic `Bezier`. Its control points are smoothed from the neighboring points. A stroke's first point is buffered twice so its first segment gets drawn, and the last segment is added when the stroke ends.
- `pathOf` joins every curve into one `Path`, starting a new contour where `Bezier.startsStroke` is set. `drawSignature` and `drawOnBitmap` draw it with a single `drawPath`, using a stroke paint with round caps and joins.
- A tap is a zero-length curve, which the round cap draws as a dot.

### Tests

Most tests are in `signaturepad/src/jvmTest`. `RecordingCanvas` is a fake `Canvas` that records each drawn curve as points sampled along it, so tests can check ink without a real renderer. Anything that depends on how Skia renders, like a tap's dot, draws to an `ImageBitmap` and checks the pixels.

## Key Conventions

- Gradle 9.6.1 with Kotlin DSL, version catalog at `gradle/libs.versions.toml`
- JVM toolchain: Java 17
- Library version defined in `signaturepad/build.gradle.kts` (`version = "2.3.1"`)
- Publishing: Maven Central via vanniktech-maven-publish plugin with GPG signing
- Android: compileSdk 37, minSdk 23
