plugins {
    id("com.android.library")
    id("kotlin-android")
    id("maven-publish")
}

android {
    compileSdk = 31

    defaultConfig {
        minSdk = 21
        targetSdk = 31
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += listOf(
            "-Xexplicit-api=strict"
        )
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion =
            de.fayard.refreshVersions.core.versionFor(AndroidX.compose.material)
    }
}

dependencies {
    implementation(AndroidX.core.ktx)
    implementation(AndroidX.compose.material)
}

afterEvaluate {
    publishing {
        publications {
            // Creates a Maven publication called "release".
            create<MavenPublication>("release") {
                // Applies the component for the release build variant.
                from (components["release"])

                // You can then customize attributes of the publication as shown below.
                groupId = "com.github.sproctor"
                artifactId = "signaturepad"
                version = "0.6.0"
            }
        }
    }
}