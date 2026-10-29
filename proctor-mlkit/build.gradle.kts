import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kover)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
}

kotlin {
    explicitApi()

    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {}

    android {
        namespace = "com.jantursky.proctor.android"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        // The ML Kit Prompt API itself needs API 26.
        minSdk =
            libs.versions.android.promptMinSdk
                .get()
                .toInt()
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        withHostTest {}
    }

    sourceSets {
        androidMain.dependencies {
            api(project(":proctor"))
            implementation(libs.mlkit.genai.prompt)
        }
        getByName("androidHostTest").dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

kover {
    reports {
        filters {
            excludes {
                // These talk to AICore and the window system; they run on a device, not in host tests.
                classes(
                    "com.jantursky.proctor.android.RealPromptClient",
                    "com.jantursky.proctor.android.ProctorHostActivity",
                )
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
    // Central only needs a javadoc jar to exist. An empty one keeps the upload small: the Dokka HTML was
    // ~700 KB per publication, most of a release against the monthly size and file limits.
    configure(KotlinMultiplatform(javadocJar = JavadocJar.Empty()))
    // CI passes the key as ORG_GRADLE_PROJECT_signingInMemoryKey; local builds publish unsigned.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
    coordinates(artifactId = "proctor-mlkit")
    pom {
        name.set("Proctor Android")
        description.set(
            "Runs Proctor suites against Gemini Nano through the ML Kit GenAI Prompt API: model version, preview " +
                "models, quota and foreground handling.",
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
