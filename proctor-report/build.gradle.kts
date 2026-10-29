import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kover)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.binary.compatibility.validator)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    explicitApi()
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    api(project(":proctor"))
    testImplementation(kotlin("test"))
}

kover {
    reports {
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
    configure(KotlinJvm(javadocJar = JavadocJar.Empty()))
    // CI passes the key as ORG_GRADLE_PROJECT_signingInMemoryKey; local builds publish unsigned.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
    coordinates(artifactId = "proctor-report")
    pom {
        name.set("Proctor Report")
        description.set(
            "Collects Proctor result files and builds HTML and Markdown reports with diffs between platforms, " +
                "model versions and prompt versions, plus a pass verdict for CI.",
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
