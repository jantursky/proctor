#!/usr/bin/env bash
# Publishes the artifacts to Maven Local and runs a fresh consumer project against them: it defines a suite, runs it
# on the fake backend, writes a result file and builds the report through the Gradle plugin. A broken POM, a missing
# variant or a leaked internal type fails here, not for users.
set -euo pipefail

root=$(cd "$(dirname "$0")/.." && pwd)
version=$(sed -n 's/^VERSION_NAME=//p' "$root/gradle.properties")
kotlin=$(sed -n 's/^kotlin = "\(.*\)"/\1/p' "$root/gradle/libs.versions.toml")
consumer=$(mktemp -d)
trap 'rm -rf "$consumer"' EXIT

"$root/gradlew" -p "$root" \
  :proctor:publishToMavenLocal :proctor-report:publishToMavenLocal \
  :proctor-mlkit:publishToMavenLocal :proctor-gradle-plugin:publishToMavenLocal --quiet

# Maven Local overwrites files that two publications share, Maven Central refuses them. Building the upload bundle
# with a made-up token stops at the upload ("Invalid token") when every artifact has a path of its own and at
# "duplicate entry" when two publications collide, as `proctor-android` once did for the core's Android variant.
bundle=$("$root/gradlew" -p "$root" publishAllPublicationsToMavenCentralRepository --no-configuration-cache \
  -PmavenCentralUsername=none -PmavenCentralPassword=none 2>&1 || true)
if grep -q "duplicate entry" <<<"$bundle"; then
  grep "duplicate entry" <<<"$bundle" >&2
  echo "Two publications share an artifact path." >&2
  exit 1
fi
grep -q "Invalid token" <<<"$bundle" || { echo "The Central bundle was not built:" >&2; tail -20 <<<"$bundle" >&2; exit 1; }

mkdir -p "$consumer/src/main/kotlin"
cat > "$consumer/settings.gradle.kts" <<EOF2
pluginManagement {
    repositories {
        // The plugin marker lives in a group named after the plugin id.
        mavenLocal { content { includeGroupAndSubgroups("io.github.jantursky") } }
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal { content { includeGroup("io.github.jantursky") } }
        google()
        mavenCentral()
    }
}

rootProject.name = "consumer"
EOF2
cat > "$consumer/build.gradle.kts" <<EOF2
plugins {
    kotlin("jvm") version "$kotlin"
    application
    id("io.github.jantursky.proctor") version "$version"
}

dependencies {
    implementation("io.github.jantursky:proctor:$version")
    implementation("io.github.jantursky:proctor-report:$version")
}

application { mainClass.set("MainKt") }
EOF2
cat > "$consumer/src/main/kotlin/Main.kt" <<'EOF2'
import com.jantursky.proctor.EvalRunner
import com.jantursky.proctor.FakeBackend
import com.jantursky.proctor.RunnerConfig
import com.jantursky.proctor.evalSuite
import com.jantursky.proctor.report.Report
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.time.Duration.Companion.seconds

fun main() = runBlocking {
    val suite = evalSuite("pack-test") {
        prompt(version = "v1") { input -> "Classify: $input" }
        case(input = "My card was charged twice") { expect("Billing") }
        graders { exactMatch() }
        repeat(2)
    }
    val result = EvalRunner(FakeBackend { _, _ -> "Billing" }, RunnerConfig(requestDelay = 0.seconds)).run(suite)
    check(result.summary.suitePassed) { "the suite should pass" }
    check(Report.of(listOf(result)).toMarkdown().contains("pack-test"))
    File("proctor-results").apply { mkdirs() }.resolve(result.fileName()).writeText(result.toJson())
}
EOF2

"$root/gradlew" -p "$consumer" run --quiet
"$root/gradlew" -p "$consumer" proctorReport --quiet
test -s "$consumer/build/reports/proctor/index.html"
test -s "$consumer/build/reports/proctor/report.md"
echo "Consumer built a report against io.github.jantursky:proctor:$version"
