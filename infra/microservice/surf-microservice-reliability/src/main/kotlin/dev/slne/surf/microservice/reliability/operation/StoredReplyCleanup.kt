package dev.slne.surf.microservice.reliability.operation

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.database.reliability.operation.AtomicOperations
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.context.SmartLifecycle
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration

private val log = logger()

/**
 * Deletes the expired stored replies of all owners every [interval], in batches, while the context
 * runs.
 *
 * The first run starts one [interval] after the context started. A failed run is logged and retried
 * at the next interval. [stop] cancels the cleanup without waiting for a database call in progress.
 */
internal class StoredReplyCleanup(
    private val operations: AtomicOperations,
    private val interval: Duration,
) : SmartLifecycle {

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineName("stored-reply-cleanup"),
    )

    @Volatile
    private var job: Job? = null

    override fun start() {
        job = scope.launch {
            while (true) {
                delay(interval)
                runOnce()
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
    }

    override fun isRunning(): Boolean = job?.isActive == true

    /**
     * Cancels the cleanup for good when the context closes.
     */
    fun close() {
        scope.cancel("The context is closed")
    }

    private suspend fun runOnce() {
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

    private suspend fun cleanUp() {
        var total = 0

        do {
            val deleted = operations.deleteExpired(BATCH_SIZE)
            total += deleted
        } while (deleted == BATCH_SIZE)

        if (total > 0) {
            log.atInfo()
                .log("Deleted %d expired stored replies", total)
        }
    }

    private companion object {
        const val BATCH_SIZE = AtomicOperations.DEFAULT_CLEANUP_BATCH_SIZE
    }
}
