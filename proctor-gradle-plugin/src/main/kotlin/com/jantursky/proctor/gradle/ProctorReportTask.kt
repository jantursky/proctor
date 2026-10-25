package com.jantursky.proctor.gradle

import com.jantursky.proctor.report.ReportJob
import com.jantursky.proctor.report.VerdictOptions
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Writes the report, then fails the build when the results do not meet the pass conditions. */
@CacheableTask
public abstract class ProctorReportTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val resultsDir: DirectoryProperty

    @get:OutputDirectory
    public abstract val outputDir: DirectoryProperty

    @get:Input
    @get:Optional
    public abstract val baseline: Property<String>

    @get:Input
    public abstract val failOnRegression: Property<Boolean>

    @get:Input
    public abstract val failOnNotGraded: Property<Boolean>

    @TaskAction
    public fun report() {
        val output =
            ReportJob.run(
                resultsDir = resultsDir.get().asFile.toPath(),
                outputDir = outputDir.get().asFile.toPath(),
                baseline = baseline.orNull,
                options = VerdictOptions(failOnRegression.get(), failOnNotGraded.get()),
            )
        logger.lifecycle("Proctor report: ${output.html.toUri()}")
        if (!output.verdict.passed) {
            throw GradleException(
                "Proctor results do not meet the pass conditions:\n" +
                    output.verdict.reasons.joinToString("\n") { "  - $it" },
            )
        }
    }
}
