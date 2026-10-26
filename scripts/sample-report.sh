#!/usr/bin/env bash
# Builds the report for the result files in docs/samples with the published Gradle plugin and writes it next to them
# as docs/samples/report/. Usage: scripts/sample-report.sh
set -euo pipefail

root=$(cd "$(dirname "$0")/.." && pwd)
version=$(sed -n 's/^VERSION_NAME=//p' "$root/gradle.properties")
kotlin=$(sed -n 's/^kotlin = "\(.*\)"/\1/p' "$root/gradle/libs.versions.toml")
consumer=$(mktemp -d)
trap 'rm -rf "$consumer"' EXIT

"$root/gradlew" -p "$root" :proctor-report:publishToMavenLocal :proctor-gradle-plugin:publishToMavenLocal \
  :proctor:publishToMavenLocal --quiet

cat > "$consumer/settings.gradle.kts" <<EOF2
pluginManagement {
    repositories {
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
rootProject.name = "sample-report"
EOF2
cat > "$consumer/build.gradle.kts" <<EOF2
plugins { id("io.github.jantursky.proctor") version "$version" }
proctor {
    resultsDir.set(file("$root/docs/samples"))
    outputDir.set(file("$root/docs/samples/report"))
}
EOF2

"$root/gradlew" -p "$consumer" proctorReport --quiet
echo "Report written to $root/docs/samples/report"
