package dev.slne.surf.microservice.reliability.integration

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.reliability.operation.AtomicOperations
import dev.slne.surf.microservice.reliability.ReliabilityConfig
import dev.slne.surf.microservice.reliability.TransactionalEvents
import dev.slne.surf.microservice.reliability.operation.StoredReplyCleanup
import dev.slne.surf.microservice.reliability.operation.TransactionalInterceptor
import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.rpc.NatsPersistentInterceptor
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * The running reliability components of one microservice: stored replies, their cleanup and the event publisher.
 *
 * [owner] namespaces the stored replies. Instances of the same service share it, so they answer each other's
 * redeliveries. The schema must be migrated before [start].
 */
internal class ReliabilityRuntime private constructor(
    val owner: String,
    val events: TransactionalEvents,
    private val operations: AtomicOperations,
    private val scope: CoroutineScope,
) {

    /**
     * Returns the interceptor for a transactional service registered on [serviceClient], which publishes the
     * handlers' events.
     */
    fun interceptorFor(serviceClient: NatsClient): NatsPersistentInterceptor =
        TransactionalInterceptor(owner, operations, serviceClient)

    /**
     * Stops the cleanup without waiting for a database call in progress.
     */
    fun stop() {
        scope.cancel()
    }

    companion object {
        /**
         * Starts the stored replies of [owner] on [databaseApi] and their periodic cleanup.
         */
        fun start(owner: String, databaseApi: DatabaseApi, config: ReliabilityConfig): ReliabilityRuntime {
            val operations = AtomicOperations(databaseApi, config.atomicOperations)
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineName("reliability-$owner"))
            StoredReplyCleanup(operations, config.cleanupInterval).start(scope)

            return ReliabilityRuntime(owner, TransactionalEvents(), operations, scope)
        }
    }
}
