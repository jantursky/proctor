package com.jantursky.proctor

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Pacing for system models, which meter inference per app. The defaults suit AICore: one request at a time, a pause
 * between requests, and backoff when the quota answers.
 */
public class RunnerConfig(
    /** Requests in flight. Keep 1 for system models. */
    public val concurrency: Int = 1,
    /** Pause after every request. */
    public val requestDelay: Duration = 1.seconds,
    /** Retries after a quota error before the attempt is recorded as not graded. */
    public val maxQuotaRetries: Int = 3,
    public val initialBackoff: Duration = 5.seconds,
    public val maxBackoff: Duration = 1.minutes,
) {
    init {
        require(concurrency >= 1) { "concurrency must be at least 1" }
        require(maxQuotaRetries >= 0) { "maxQuotaRetries must not be negative" }
    }
}

/** The run cannot go on and its results would mislead, for example because the app left the foreground. */
public class RunAbortedException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

public class EvalRunner(
    private val backend: ModelBackend,
    private val config: RunnerConfig = RunnerConfig(),
    private val clock: Clock = Clock.System,
) {
    /** Runs every prompt version of [suite], one result each. */
    public suspend fun runAll(suite: EvalSuite): List<RunResult> = suite.prompts.map { run(suite, it.version) }

    /**
     * Runs one prompt version (the first declared by default).
     *
     * A model that is not available yields a result with every case [Status.NotGraded] and the availability
     * recorded, not failed cases.
     *
     * @throws RunAbortedException if the app loses the foreground during the run.
     */
    public suspend fun run(
        suite: EvalSuite,
        promptVersion: String = suite.prompts.first().version,
    ): RunResult {
        val variant = suite.prompt(promptVersion)
        val availability = backend.availability()
        val environment = backend.environment()
        val startedAt = clock.now().toString()
        val cases =
            if (availability is BackendAvailability.Available) {
                runCases(suite, variant)
            } else {
                val reason = "Model not available: ${availability.describe()}"
                suite.cases.map { skipped(it, reason) }
            }
        return RunResult(
            suite = suite.name,
            promptVersion = promptVersion,
            environment = environment,
            sampling = suite.sampling,
            repeat = suite.repeat,
            startedAt = startedAt,
            availability = availability.state(),
            availabilityReason = (availability as? BackendAvailability.Unavailable)?.reason,
            summary = summarize(suite, cases),
            cases = cases,
        )
    }

    private suspend fun runCases(
        suite: EvalSuite,
        variant: PromptVariant,
    ): List<CaseResult> =
        coroutineScope {
            val permits = Semaphore(config.concurrency)
            suite.cases
                .map { case ->
                    val attempts =
                        (1..suite.repeat).map {
                            async { permits.withPermit { attempt(suite, variant, case) } }
                        }
                    case to attempts
                }.map { (case, attempts) -> caseResult(case, attempts.awaitAll()) }
        }

    private suspend fun attempt(
        suite: EvalSuite,
        variant: PromptVariant,
        case: EvalCase,
    ): Attempt {
        val prompt = variant.render(case.input)
        var backoff = config.initialBackoff
        var retries = 0
        while (true) {
            val started = TimeSource.Monotonic.markNow()
            try {
                val output = backend.generate(prompt, suite.sampling)
                val latency = started.elapsedNow().inWholeMilliseconds
                delay(config.requestDelay)
                return graded(suite, case, prompt, output, latency)
            } catch (e: BackendException.QuotaExceeded) {
                if (retries++ >= config.maxQuotaRetries) {
                    return notGraded("Inference quota still exceeded after ${config.maxQuotaRetries} retries")
                }
                delay(e.retryAfter ?: backoff)
                backoff = (backoff * 2).coerceAtMost(config.maxBackoff)
            } catch (e: BackendException.Backgrounded) {
                throw RunAbortedException(
                    "The app lost the foreground during the run; AICore serves only the top foreground app",
                    e,
                )
            } catch (e: BackendException.Unavailable) {
                return notGraded("Model unavailable: ${e.reason}")
            } catch (e: BackendException.Failed) {
                return notGraded("Backend error: ${e.message}")
            }
        }
    }

    private suspend fun graded(
        suite: EvalSuite,
        case: EvalCase,
        prompt: String,
        output: String,
        latency: Long,
    ): Attempt {
        val context = GradeContext(case.input, prompt, output, case.expected)
        val results =
            suite.graders.map { grader ->
                val grade =
                    try {
                        grader.grade(context)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Grade.notGraded("Grader failed: ${e.message ?: e::class.simpleName}")
                    }
                GraderResult(grader.name, grade.status, grade.score, grade.rationale)
            }
        val decided = results.filter { it.status != Status.NotGraded }
        val status =
            when {
                decided.isEmpty() -> Status.NotGraded
                decided.any { it.status == Status.Failed } -> Status.Failed
                else -> Status.Passed
            }
        val score = decided.mapNotNull { it.score }.meanOrNull()
        return Attempt(output, latency, status, score, results)
    }

    private fun notGraded(note: String) = Attempt(status = Status.NotGraded, note = note)

    private fun skipped(
        case: EvalCase,
        reason: String,
    ) = CaseResult(case.id, case.input, case.expected, Status.NotGraded, attempts = listOf(notGraded(reason)))

    private fun caseResult(
        case: EvalCase,
        attempts: List<Attempt>,
    ): CaseResult {
        val decided = attempts.filter { it.status != Status.NotGraded }
        val scores = decided.mapNotNull { it.score }
        val status =
            when {
                decided.isEmpty() -> Status.NotGraded
                decided.any { it.status == Status.Failed } -> Status.Failed
                else -> Status.Passed
            }
        return CaseResult(
            case.id,
            case.input,
            case.expected,
            status,
            scores.meanOrNull(),
            scores.spreadOrNull(),
            attempts,
        )
    }

    private fun summarize(
        suite: EvalSuite,
        cases: List<CaseResult>,
    ): Summary {
        val meanScore = cases.mapNotNull { it.meanScore }.meanOrNull()
        val passed = cases.count { it.status == Status.Passed }
        val failed = cases.count { it.status == Status.Failed }
        val notGraded = cases.count { it.status == Status.NotGraded }
        val score = SuiteScore(meanScore ?: 0.0, cases.size, passed, failed, notGraded)
        return Summary(cases.size, passed, failed, notGraded, meanScore, suite.passWhen(score))
    }

    private fun BackendAvailability.state(): AvailabilityState =
        when (this) {
            BackendAvailability.Available -> AvailabilityState.Available
            BackendAvailability.Downloadable -> AvailabilityState.Downloadable
            BackendAvailability.Downloading -> AvailabilityState.Downloading
            is BackendAvailability.Unavailable -> AvailabilityState.Unavailable
        }

    private fun BackendAvailability.describe(): String =
        when (this) {
            is BackendAvailability.Unavailable -> "unavailable ($reason)"
            else -> state().name.lowercase()
        }
}
