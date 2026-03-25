plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    androidLibrary {
        namespace = "com.seanproctor.signaturedemo"

        compileSdk = 36
        minSdk = 23
    }
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
        jvmMain {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.seanproctor.signaturedemo.MainKt"
    }
}
