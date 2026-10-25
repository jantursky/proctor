package com.jantursky.proctor.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

/** Adds the `proctor` extension and the `proctorReport` task. */
public class ProctorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("proctor", ProctorExtension::class.java)
        extension.resultsDir.convention(project.layout.projectDirectory.dir("proctor-results"))
        extension.outputDir.convention(project.layout.buildDirectory.dir("reports/proctor"))
        extension.failOnRegression.convention(false)
        extension.failOnNotGraded.convention(false)

        project.tasks.register("proctorReport", ProctorReportTask::class.java) { task ->
            task.group = "verification"
            task.description = "Builds the Proctor HTML and Markdown report and fails below the pass condition."
            task.resultsDir.set(extension.resultsDir)
            task.outputDir.set(extension.outputDir)
            task.baseline.set(extension.baseline)
            task.failOnRegression.set(extension.failOnRegression)
            task.failOnNotGraded.set(extension.failOnNotGraded)
        }
    }
}
