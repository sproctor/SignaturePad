plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.kotlinx.benchmark)
}

kotlin {
    jvm()

    jvmToolchain(17)

    sourceSets {
        jvmMain {
            dependencies {
                implementation(project(":signaturepad"))
                implementation(libs.compose.foundation)
                implementation(libs.kotlinx.benchmark.runtime)
                // Provides the skiko native runtime so the benchmarks can render to a real canvas.
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

// JMH subclasses the @State classes, so they must be open.
allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

// Run with ./gradlew :benchmark:benchmark. Results are written to build/reports/benchmarks.
benchmark {
    targets {
        register("jvm")
    }
    configurations {
        named("main") {
            warmups = 3
            iterations = 5
            iterationTime = 1
            iterationTimeUnit = "s"
            mode = "avgt"
            outputTimeUnit = "us"
        }
    }
}
