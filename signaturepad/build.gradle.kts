plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.vanniktech.maven.publish.base)
}

group = "com.seanproctor"
version = "2.0.0"

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
    js {
        browser()
    }

    explicitApi()

    jvmToolchain(17)

    sourceSets {
        val commonMain by getting {
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
