import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar
plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
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
    implementation(project(":proctor-report"))
    testImplementation(kotlin("test"))
    testImplementation(gradleTestKit())
}

gradlePlugin {
    plugins {
        create("proctor") {
            id = "io.github.jantursky.proctor"
            implementationClass = "com.jantursky.proctor.gradle.ProctorPlugin"
            displayName = "Proctor"
            description =
                "Builds HTML and Markdown reports from Proctor result files and fails the build below the pass condition."
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    // Central only needs a javadoc jar to exist. An empty one keeps the upload small: the Dokka HTML was
    // ~700 KB per publication, most of a release against the monthly size and file limits.
    configure(GradlePlugin(javadocJar = JavadocJar.Empty()))
    // CI passes the key as ORG_GRADLE_PROJECT_signingInMemoryKey; local builds publish unsigned.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
    coordinates(artifactId = "proctor-gradle-plugin")
    pom {
        name.set("Proctor Gradle plugin")
        description.set("Gradle task that turns Proctor result files into a report and a CI verdict.")
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
