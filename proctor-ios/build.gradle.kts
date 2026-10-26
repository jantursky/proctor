plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kover)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.binary.compatibility.validator)
}

kotlin {
    explicitApi()

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        // A framework for apps that have no shared Kotlin framework of their own. Apps that do should export
        // :proctor and :proctor-ios from theirs instead.
        target.binaries.framework {
            baseName = "ProctorIos"
            isStatic = true
            export(project(":proctor"))
        }
    }

    sourceSets {
        iosMain.dependencies {
            api(project(":proctor"))
        }
        iosTest.dependencies {
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
    // The iOS targets run Kotlin/Native tests; Kover reports on JVM and Android only, so there is nothing to gate here.
}

mavenPublishing {
    publishToMavenCentral()
    // CI passes the key as ORG_GRADLE_PROJECT_signingInMemoryKey; local builds publish unsigned.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
    coordinates(artifactId = "proctor-ios")
    pom {
        name.set("Proctor iOS")
        description.set(
            "Runs Proctor suites against Apple Foundation Models through a small Swift bridge: availability, " +
                "OS-derived model version, and rate-limit handling.",
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
