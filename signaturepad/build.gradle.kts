plugins {
    id("com.android.library")
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("com.vanniktech.maven.publish.base")
}

group = "com.seanproctor"
version = "1.0.5"

android {
    namespace = "com.seanproctor.signaturepad"

    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }
    jvm()
    js(IR) {
        browser()
    }

    explicitApi()

    jvmToolchain(11)

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(compose.runtime)
                implementation(compose.foundation)
                implementation("com.soywiz.korlibs.korim:korim:_")
            }
        }
        val skikoMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(compose.ui)
            }
        }
        val jvmMain by getting {
            dependsOn(skikoMain)
        }
        val jsMain by getting {
            dependsOn(skikoMain)
        }
    }

    jvmToolchain(11)
}

configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
    configure(
        com.vanniktech.maven.publish.KotlinMultiplatform(javadocJar = com.vanniktech.maven.publish.JavadocJar.Empty())
    )
}