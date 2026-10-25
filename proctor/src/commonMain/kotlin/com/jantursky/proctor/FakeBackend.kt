package com.jantursky.proctor

/**
 * A scripted [ModelBackend] for deterministic tests, including simulated quota and availability errors.
 *
 * [responder] gets the prompt and the zero-based number of the call. It returns the reply, or throws a
 * [BackendException] to simulate a platform limit.
 */
public class FakeBackend(
    private val availability: BackendAvailability = BackendAvailability.Available,
    private val environment: Environment =
        Environment(
            platform = "fake",
            deviceModel = "fake-device",
            osVersion = "0",
            modelVersion = "fake-model-1",
            backend = "fake",
        ),
    private val responder: (prompt: String, call: Int) -> String,
) : ModelBackend {
    private val received = mutableListOf<String>()

    /** Every prompt the backend was asked, in order, including the ones that threw. */
    public val prompts: List<String> get() = received.toList()

    override suspend fun availability(): BackendAvailability = availability

    override suspend fun environment(): Environment = environment

    override suspend fun generate(
        prompt: String,
        sampling: Sampling,
    ): String {
        val call = received.size
        received += prompt
        return responder(prompt, call)
    }
}
