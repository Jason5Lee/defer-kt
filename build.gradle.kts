import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper

description = "A Golang-inspired resource management library."

val ossrhUsername: String? by project
val ossrhPassword: String? by project

val signingKeyId: String? by project // must be the last 8 digits of the key
val signingKey: String? by project
val signingPassword: String? by project

plugins {
    base
    id("com.github.ben-manes.versions") version Versions.versionsPlugin

    kotlin("multiplatform") version Versions.kotlin
    id("org.jetbrains.dokka") version Versions.dokka
    `maven-publish`
}

group = "me.jason5lee"
version = "2.0.0-SNAPSHOT"

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        listOf("alpha", "beta", "rc", "cr", "m", "eap", "pr", "dev").any {
            candidate.version.contains(it, ignoreCase = true)
        }
    }
}

repositories {
    mavenCentral()
}

plugins.withType<MavenPublishPlugin> {
    apply(plugin = "org.gradle.signing")

    plugins.withType<KotlinMultiplatformPluginWrapper> {
        apply(plugin = "org.jetbrains.dokka")

        kotlin.sourceSets.forEach { println(it.name) }

        val dokkaHtml by tasks.existing(DokkaTask::class) {
            outputDirectory.set(File("$buildDir/docs/javadoc"))
        }

        val javadocJar by tasks.registering(Jar::class) {
            group = LifecycleBasePlugin.BUILD_GROUP
            description = "Assembles a jar archive containing the Javadoc API documentation."
            archiveClassifier.set("javadoc")
            dependsOn(dokkaHtml)
            from(dokkaHtml.get().outputDirectory)
        }

        configure<KotlinMultiplatformExtension> {
            explicitApi()

            jvm {
                mavenPublication {
                    artifact(javadocJar.get())
                }
            }

            js(IR) {
                browser()
                nodejs()
            }

            // According to https://kotlinlang.org/docs/native-target-support.html
            // Tier 1
            linuxX64()
            macosX64()
            macosArm64()
            iosSimulatorArm64()
            iosX64()
            // Tier 2
            linuxArm64()
            watchosSimulatorArm64()
            watchosX64()
            watchosArm32()
            watchosArm64()
            tvosSimulatorArm64()
            tvosX64()
            tvosArm64()
            iosArm64()
            // Tier 3
            androidNativeArm32()
            androidNativeArm64()
            androidNativeX86()
            androidNativeX64()
            mingwX64()
            watchosDeviceArm64()
        }
    }

    configure<PublishingExtension> {
        repositories {
            maven {
                if (project.version.toString().endsWith("SNAPSHOT")) {
                    setUrl("https://s01.oss.sonatype.org/content/repositories/snapshots")
                } else {
                    setUrl("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2")
                }

                credentials {
                    username = ossrhUsername
                    password = ossrhPassword
                }
            }
        }

        publications.withType<MavenPublication> {
            pom {
                name.set(project.name)
                url.set("https://github.com/jason5lee/defer-kt")
                inceptionYear.set("2021")

                description.set(project.description)
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        name.set("Jason5Lee")
                        url.set("https://jason5lee.me/")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/Jason5lee/defer-kt")
                    developerConnection.set("scm:git:git@github.com:Jason5Lee/defer-kt.git")
                    url.set("https://github.com/Jason5Lee/defer-kt")
                }

                issueManagement {
                    system.set("GitHub")
                    url.set("https://github.com/Jason5Lee/defer-kt/issues")
                }

                ciManagement {
                    system.set("GitHub")
                    url.set("https://github.com/Jason5Lee/defer-kt/actions?query=workflow%3Aci")
                }
            }
        }

        configure<SigningExtension> {
            useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
            sign(publications)
        }
    }
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }
    }
}

