import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kover)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.binary.compatibility.validator)
}

kotlin {
    explicitApi()

    android {
        namespace = "com.jantursky.proctor"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        withHostTest {}
    }

    jvm { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

apiValidation {
    @OptIn(kotlinx.validation.ExperimentalBCVApi::class)
    klib { enabled = true }
}

kover {
    reports {
        filters {
            excludes {
                // Plain data holders: their generated constructors and serializers add branches that only
                // the JSON round-trip test reaches. The logic around them (file names, parsing) is covered.
                annotatedBy("kotlinx.serialization.Serializable")
            }
        }
        verify {
            rule {
                bound { minValue = 90 }
                bound {
                    coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.BRANCH
                    minValue = 78
                }
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    // CI passes the key as ORG_GRADLE_PROJECT_signingInMemoryKey; local builds publish unsigned.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
    coordinates(artifactId = "proctor")
    pom {
        name.set("Proctor")
        description.set(
            "Evaluation harness for on-device system language models in Kotlin Multiplatform: one suite, " +
                "graders, repeat runs and comparable result files for Gemini Nano and Apple Foundation Models.",
        )
        inceptionYear.set("2026")
        url.set("https://github.com/jantursky/proctor")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/license/mit")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("jantursky")
                name.set("Jan Tursky")
                email.set("jan.tursky@gmail.com")
                url.set("https://jantursky.com")
            }
        }
        scm {
            url.set("https://github.com/jantursky/proctor")
            connection.set("scm:git:https://github.com/jantursky/proctor.git")
            developerConnection.set("scm:git:ssh://git@github.com/jantursky/proctor.git")
        }
    }
}
