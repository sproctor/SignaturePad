rootProject.name = "signature-pad"

include(":app")
include(":signaturepad")

plugins {
    // See https://jmfayard.github.io/refreshVersions
    id("de.fayard.refreshVersions") version "0.50.2"
}
