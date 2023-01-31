rootProject.name = "signature-pad"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

plugins {
    id("de.fayard.refreshVersions") version "0.51.0"
}

include(":app")
include(":signaturepad")
