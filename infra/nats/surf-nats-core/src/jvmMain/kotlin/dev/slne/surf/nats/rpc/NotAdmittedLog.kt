package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * Reports the requests of one service registration that got no handler slot, without one log line per request.
 *
 * The first rejection after a quiet [interval] is reported at once. Later ones are counted and summarized once per
 * [interval] for as long as they continue. A summary pending when [scope] is cancelled is reported then. The summary
 * runs in [scope], which must not be the handler scope of the registration, so a drain does not wait for it.
 * Thread-safe.
 */
@InternalNatsApi
internal class NotAdmittedLog(
    private val serviceName: String,
    private val permits: Int,
    private val scope: CoroutineScope,
    private val interval: Duration,
    private val warn: (String) -> Unit,
) {
    private val lock = Any()
    private var summarizing = false
    private var unreported = 0

    /**
     * Reports that a request on [subject] got no handler slot before its deadline.
     */
    fun report(subject: String) {
        val first = synchronized(lock) {
            if (summarizing) unreported++
            !summarizing.also { summarizing = true }
        }
        if (!first) return

        warn(
            "Request on $subject got no handler slot: all $permits handler slots of $serviceName stayed busy " +
                    "until its deadline (further rejections are summarized every $interval)"
        )
        scope.launch { summarizeWhileRejecting() }
    }

    private suspend fun summarizeWhileRejecting() {
        try {
            do delay(interval) while (summarize())
        } catch (e: CancellationException) {
            summarize()
            throw e
        }
    }

    // Returns whether a rejection was reported since the last call
    private fun summarize(): Boolean {
        val count = synchronized(lock) {
            unreported.also {
                unreported = 0
                summarizing = it > 0
            }
        }
        if (count > 0) warn("$count more requests to $serviceName got no handler slot in the last $interval")
        return count > 0
    }
}
