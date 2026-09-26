plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

android {
    compileSdk = 37
    namespace = "com.seanproctor.signaturepad.androiddemo"

    defaultConfig {
        applicationId = "com.seanproctor.signaturepad.androiddemo"
        minSdk = 23
        targetSdk = 37
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
        // Built and installed by :macrobenchmark. A release build signed with the debug key, which
        // adds BenchmarkActivity from src/benchmark.
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":demo"))
    implementation(libs.compose.material)
    implementation(libs.activity.compose)
    // BenchmarkActivity uses the library directly.
    "benchmarkImplementation"(project(":signaturepad"))
    // Lets :macrobenchmark clear the shader cache between runs.
    "benchmarkImplementation"(libs.androidx.profileinstaller)
}
