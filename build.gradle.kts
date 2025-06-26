import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.vanniktech.maven.publish.base) apply false
}

tasks.wrapper {
    gradleVersion = "8.14.2"
}

allprojects {
    // Credentials must be added to ~/.gradle/gradle.properties per
    // https://vanniktech.github.io/gradle-maven-publish-plugin/central/#secrets
    plugins.withId("com.vanniktech.maven.publish.base") {
        configure<PublishingExtension> {
            repositories {
                maven {
                    name = "testMaven"
                    url = rootProject.layout.buildDirectory.file("/testMaven").get().asFile.toURI()
                }
            }
        }
        configure<MavenPublishBaseExtension> {
            publishToMavenCentral(automaticRelease = true)
            signAllPublications()
            pom {
                name.set("Compose Signature Pad")
                description.set("A signature pad for Compose Multiplatform.")
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