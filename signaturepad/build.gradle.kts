@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.vanniktech.maven.publish.base)
}

group = "com.seanproctor"
version = "2.1.3"

kotlin {
    androidLibrary {
        namespace = "com.seanproctor.signaturepad"

        compileSdk = 36
        minSdk = 21
    }
    jvm()
    js {
        browser()
    }
    wasmJs {
        browser()
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    explicitApi()

    jvmToolchain(17)

    sourceSets {
        commonMain {
            dependencies {
                api(compose.runtime)
                implementation(compose.foundation)
            }
        }
    }
}

configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
    configure(
        com.vanniktech.maven.publish.KotlinMultiplatform(javadocJar = com.vanniktech.maven.publish.JavadocJar.Empty())
    )
}
