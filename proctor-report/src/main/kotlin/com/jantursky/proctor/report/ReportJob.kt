package com.jantursky.proctor.report

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

/** The files a job wrote and whether the results meet the pass conditions. */
public class ReportOutput internal constructor(
    public val html: Path,
    public val markdown: Path,
    public val verdict: Verdict,
)

/** Reads a directory of result files and writes `index.html` and `report.md` next to each other. */
public object ReportJob {
    /**
     * @param baseline file name of the run to compare the others against; the oldest run of each suite when `null`.
     * @throws IllegalArgumentException if the directory holds no results or no file is named [baseline].
     */
    public fun run(
        resultsDir: Path,
        outputDir: Path,
        baseline: String? = null,
        options: VerdictOptions = VerdictOptions(),
    ): ReportOutput {
        val runs = ResultFiles.readAll(resultsDir)
        require(runs.isNotEmpty()) { "No *.proctor.json files in $resultsDir" }
        val baselineRun =
            baseline?.let { name ->
                runs.firstOrNull { it.fileName() == name || it.fileName() == name.substringAfterLast('/') }
                    ?: throw IllegalArgumentException(
                        "No result file named $name in $resultsDir; found ${runs.map { it.fileName() }}",
                    )
            }
        val report = Report.of(runs, baselineRun)
        outputDir.createDirectories()
        val html = outputDir.resolve("index.html").also { it.writeText(report.toHtml()) }
        val markdown = outputDir.resolve("report.md").also { it.writeText(report.toMarkdown()) }
        return ReportOutput(html, markdown, report.verdict(options))
    }
}
