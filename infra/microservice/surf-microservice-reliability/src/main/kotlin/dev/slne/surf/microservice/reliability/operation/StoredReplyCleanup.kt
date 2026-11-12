package dev.slne.surf.microservice.reliability.operation

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.database.reliability.operation.AtomicOperations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration

private val log = logger()

/**
 * Deletes the expired stored replies of all owners every [interval], in batches.
 *
 * A failed run is logged and retried at the next interval.
 */
internal class StoredReplyCleanup(
    private val operations: AtomicOperations,
    private val interval: Duration,
) {

    /**
     * Starts the periodic cleanup in [scope]. Cancel the returned job to stop it.
     */
    fun start(scope: CoroutineScope): Job = scope.launch {
        while (true) {
            delay(interval)
            try {
                cleanUp()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.atWarning()
                    .withCause(e)
                    .log("Failed to delete expired stored replies")
            }
        }
    }

    private suspend fun cleanUp() {
        var total = 0
        do {
            val deleted = operations.deleteExpired(BATCH_SIZE)
            total += deleted
        } while (deleted == BATCH_SIZE)

        if (total > 0) log.atInfo().log("Deleted %d expired stored replies", total)
    }

    private companion object {
        const val BATCH_SIZE = AtomicOperations.DEFAULT_CLEANUP_BATCH_SIZE
    }
}
