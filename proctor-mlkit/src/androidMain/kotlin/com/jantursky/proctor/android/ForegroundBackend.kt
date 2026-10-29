package com.jantursky.proctor.android

import com.jantursky.proctor.BackendAvailability
import com.jantursky.proctor.BackendException
import com.jantursky.proctor.Environment
import com.jantursky.proctor.ModelBackend
import com.jantursky.proctor.Sampling

/**
 * Stops a run the moment the host activity leaves the foreground, before AICore has to refuse the call. Wrap the
 * backend and pass [ProctorHostActivity.isForeground]: a lost foreground aborts the run with a clear error instead of
 * recording cases as failures.
 */
public class ForegroundBackend(
    private val delegate: ModelBackend,
    private val isForeground: () -> Boolean,
) : ModelBackend {
    override suspend fun availability(): BackendAvailability = delegate.availability()

    override suspend fun environment(): Environment = delegate.environment()

    override suspend fun generate(
        prompt: String,
        sampling: Sampling,
    ): String {
        if (!isForeground()) throw BackendException.Backgrounded()
        return delegate.generate(prompt, sampling)
    }
}
