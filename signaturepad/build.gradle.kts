@file:OptIn(ExperimentalWasmDsl::class, KotlinNativeCacheApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.mpp.DisableCacheInKotlinVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.vanniktech.maven.publish.base)
}

group = "com.seanproctor"
version = "2.3.0"

kotlin {
    androidLibrary {
        namespace = "com.seanproctor.signaturepad"

        compileSdk = 36
        minSdk = 23
    }
    jvm()
    js {
        browser()
    }
    wasmJs {
        browser()
    }
    iosArm64()
    iosSimulatorArm64()

    // The Kotlin/Native compiler cache holds Compose ui-uikit objects that hard-reference newer
    // UIKit symbols (e.g. UIViewLayoutRegion), which fail to link the iOS test binary. Disable the
    // cache for the native targets so those symbols resolve correctly.
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.all {
            disableNativeCache(
                version = DisableCacheInKotlinVersion.`2_4_0`,
                reason = "Compose ui-uikit cache references newer UIKit symbols that fail to link",
            )
        }
    }

    explicitApi()

    jvmToolchain(17)

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.compose.foundation)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        jvmTest {
            dependencies {
                // Provides the skiko native runtime so Paint()/Canvas can be exercised on the JVM.
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
    configure(
        com.vanniktech.maven.publish.KotlinMultiplatform(javadocJar = com.vanniktech.maven.publish.JavadocJar.Empty())
    )
}
