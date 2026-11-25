package com.jantursky.proctor

/** What a grader sees about one attempt. */
public data class GradeContext(
    val input: String,
    val prompt: String,
    val output: String,
    val expected: String?,
    /** How long the backend took for this attempt, in milliseconds. `0` when the caller does not measure it. */
    val latencyMillis: Long = 0,
)

/** A grader's verdict; the runner adds the grader's name. */
public data class Grade(
    val status: Status,
    val score: Double?,
    val rationale: String,
) {
    public companion object {
        public fun passed(
            score: Double = 1.0,
            rationale: String = "",
        ): Grade = Grade(Status.Passed, score, rationale)

        public fun failed(
            score: Double = 0.0,
            rationale: String,
        ): Grade = Grade(Status.Failed, score, rationale)

        public fun notGraded(rationale: String): Grade = Grade(Status.NotGraded, null, rationale)
    }
}

/** Scores one output. Keep it deterministic where you can; a thrown exception becomes [Status.NotGraded]. */
public interface Grader {
    public val name: String

    public suspend fun grade(context: GradeContext): Grade
}
