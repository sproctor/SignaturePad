# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Compose Signature Pad — a Kotlin Multiplatform signature capture library for Compose. Targets: Android, JVM, JS, WasmJS, iOS (x64, arm64, simulator arm64).

## Build Commands

```bash
./gradlew build              # Build and test all modules
./gradlew allTests           # Run tests for all targets
./gradlew check              # Run all checks including lint
./gradlew lintFix            # Run lint with auto-fix
```

Target-specific tasks follow Gradle KMP conventions (e.g., `jvmTest`, `compileKotlinJvm`).

## Architecture

Three modules:
- **signaturepad** — the library, 100% common code (`src/commonMain/kotlin/com/seanproctor/signaturepad/`). Three files: `SignaturePad.kt` (composable), `SignaturePadState.kt` (state interface + impl), `Bezier.kt` (internal curve math). Uses `explicitApi()` mode.
- **demo** — KMP demo app with JVM and JS targets. Platform entry points in `jvmMain` and `jsMain`, shared UI in `commonMain/SignatureBox.kt`.
- **androidDemo** — Android demo app (standalone Android module, not KMP).

## Key Conventions

- Gradle 9.1.0 with Kotlin DSL, version catalog at `gradle/libs.versions.toml`
- JVM toolchain: Java 17
- Library version defined in `signaturepad/build.gradle.kts` (`version = "2.1.3"`)
- Publishing: Maven Central via vanniktech-maven-publish plugin with GPG signing
- Android: compileSdk 36, minSdk 23
