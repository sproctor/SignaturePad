plugins {
    id("com.android.application")
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

android {
    compileSdk = 34
    namespace = "com.github.sproctor.signaturedemo"

    defaultConfig {
        applicationId = "com.github.sproctor.signaturedemo"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

kotlin {
    androidTarget()
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
    }
    js(IR) {
        browser()
        binaries.executable()
    }

    jvmToolchain(11)

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":signaturepad"))
                implementation(compose.material)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("androidx.activity:activity-compose:_")
                implementation(compose.preview)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(compose.web.core)
            }
        }
    }
}

compose {
    desktop {
        application {
            mainClass = "com.github.sproctor.signaturedemo.MainKt"
        }
    }
    experimental {
        web.application {}
    }
}