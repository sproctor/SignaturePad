rootProject.name = "signature-pad"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

plugins {
    id("de.fayard.refreshVersions") version "0.60.5"
}

include(":demo")
include(":signaturepad")

refreshVersions {
    // work-around https://github.com/Splitties/refreshVersions/issues/640
    file("build/tmp/refreshVersions").mkdirs()
    versionsPropertiesFile = file("build/tmp/refreshVersions/versions.properties")
    rejectVersionIf {
        candidate.stabilityLevel.isLessStableThan(current.stabilityLevel)
    }
}