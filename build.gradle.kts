import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("multiplatform") apply false
    kotlin("android") apply false
    id("com.android.application") apply false
    id("com.android.library") apply false
    id("org.jetbrains.compose") apply false
    id("com.vanniktech.maven.publish.base") apply false
}

allprojects {
    repositories {
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    // Credentials must be added to ~/.gradle/gradle.properties per
    // https://vanniktech.github.io/gradle-maven-publish-plugin/central/#secrets
    plugins.withId("com.vanniktech.maven.publish.base") {
        configure<PublishingExtension> {
            repositories {
                maven {
                    name = "testMaven"
                    url = file("${rootProject.buildDir}/testMaven").toURI()
                }
            }
        }
        @Suppress("UnstableApiUsage")
        configure<MavenPublishBaseExtension> {
            publishToMavenCentral(SonatypeHost.S01)
            signAllPublications()
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
}