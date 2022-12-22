import java.util.Properties
import java.net.URI

plugins {
    id("com.android.library")
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka")
}

group = "com.seanproctor"
version = "1.0.0"

android {
    namespace = "com.seanproctor.signaturepad"

    compileSdk = 33

    defaultConfig {
        minSdk = 21
        targetSdk = 33
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    android {
        publishLibraryVariants("release")
    }
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
    }

    explicitApi()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(compose.runtime)
                implementation(compose.foundation)
                implementation("com.soywiz.korlibs.korim:korim:_")
            }
        }
    }
}

val localProperties = Properties().apply {
    load(File(rootProject.rootDir, "local.properties").inputStream())
}

val dokkaOutputDir = buildDir.resolve("dokka")

tasks.dokkaHtml.configure {
    outputDirectory.set(dokkaOutputDir)
}

val deleteDokkaOutputDir by tasks.register<Delete>("deleteDokkaOutputDirectory") {
    delete(dokkaOutputDir)
}

val javadocJar = tasks.register<Jar>("javadocJar") {
    dependsOn(deleteDokkaOutputDir, tasks.dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaOutputDir)
}

publishing {
    repositories {
        maven {
            name = "sonatype"
            url = URI("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = localProperties.getProperty("ossrhUsername", "")
                password = localProperties.getProperty("ossrhPassword", "")
            }
        }
    }
    publications.withType<MavenPublication> {
        artifact(javadocJar)
        pom {
            name.set("Compose Signature Pad")
            description.set("A signature pad for Jetbrains Compose.")
            url.set("https://github.com/sproctor/SignaturePad")
            licenses {
                license {
                    name.set("Apache")
                    url.set("https://github.com/sproctor/SignaturePad/blob/master/LICENSE")
                }
            }
            developers {
                developer {
                    id.set("sproctor")
                    name.set("Sean Proctor")
                    email.set("sproctor@gmail.com")
                }
            }
            scm {
                url.set("https://github.com/sproctor/SignaturePad/tree/main")
            }
        }
    }
}

ext["signing.keyId"] = localProperties.getProperty("signing.keyId", "")
ext["signing.password"] = localProperties.getProperty("signing.password", "")
ext["signing.secretKeyRingFile"] =
    localProperties.getProperty("signing.secretKeyRingFile", "")

signing {
    sign(publishing.publications)
}