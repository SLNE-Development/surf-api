package dev.slne.surf.nats.js.event

import js.promise.Promise
import js.promise.PromiseResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

/**
 * Counts listener handlers whose result has not settled yet.
 */
internal class RunningHandlers {
    var count = 0
        private set

    private var idle = CompletableDeferred(Unit)

    /**
     * Adopts the handler [result] like `await` does: a promise or thenable settles later, any other value at the
     * next microtask. A rejection, including a throwing `then`, is reported to [onFailure].
     */
    fun track(result: Any?, onFailure: (Any?) -> Unit) {
        if (count++ == 0) idle = CompletableDeferred()

        Promise.resolve(PromiseResult(result)).then(
            onFulfilled = { settled() },
            onRejected = { error ->
                try {
                    onFailure(error)
                } finally {
                    settled()
                }
            },
        )
    }

    /**
     * Returns `false` when handlers are still running after [timeout].
     */
    suspend fun awaitIdle(timeout: Duration): Boolean {
        return withTimeoutOrNull(timeout) {
            while (count > 0) idle.await()
        } != null
    }

    private fun settled() {
        if (--count == 0) idle.complete(Unit)
    }
}
