package com.jantursky.proctor.gradle

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

public abstract class ProctorExtension {
    /** Directory holding the `*.proctor.json` files pulled from the devices. Defaults to `proctor-results`. */
    public abstract val resultsDir: DirectoryProperty

    /** Where `index.html` and `report.md` go. Defaults to `build/reports/proctor`. */
    public abstract val outputDir: DirectoryProperty

    /** File name of the run to compare the others against. The oldest run of each suite when unset. */
    public abstract val baseline: Property<String>

    /** Also fail when a case passes in the baseline and fails in another run. Default `false`. */
    public abstract val failOnRegression: Property<Boolean>

    /** Also fail when any case could not be graded, for example because of quota. Default `false`. */
    public abstract val failOnNotGraded: Property<Boolean>
}
