plugins {
    id("com.android.library")
    kotlin("multiplatform")
    id("maven-publish")
    id("org.jetbrains.compose")
}

group = "com.github.sproctor"
version = "0.9.3"

android {
    namespace = "com.github.sproctor.signaturepad"

    compileSdk = 33

    defaultConfig {
        minSdk = 21
        targetSdk = 33
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    android {
        publishLibraryVariants("release")
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
    }
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
    }
//    js(IR) {
//        browser()
//        binaries.executable()
//    }

    explicitApi()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(compose.runtime)
                implementation(compose.foundation)
                api("com.soywiz.korlibs.korim:korim:_")
            }
        }
    }
}