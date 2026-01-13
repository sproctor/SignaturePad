plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

android {
    compileSdk = 36
    namespace = "com.seanproctor.signaturedemo"

    defaultConfig {
        applicationId = "com.seanproctor.signaturedemo"
        minSdk = 23
        targetSdk = 36
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
    jvm()
    js {
        browser()
        binaries.executable()
    }

    jvmToolchain(17)

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":signaturepad"))
                implementation(libs.compose.material)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.activity.compose)
            }
        }
        jvmMain {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose {
    desktop {
        application {
            mainClass = "com.seanproctor.signaturedemo.MainKt"
        }
    }
}
