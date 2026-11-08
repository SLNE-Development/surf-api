package dev.slne.surf.microservice.reliability.test

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.reliability.migrateReliabilitySchema
import dev.slne.surf.database.reliability.operation.AtomicOperations
import dev.slne.surf.database.reliability.operation.OperationKey
import dev.slne.surf.database.reliability.operation.StoredReply
import dev.slne.surf.microservice.reliability.TransactionalEvents
import dev.slne.surf.microservice.reliability.operation.ActiveOperation
import dev.slne.surf.microservice.reliability.operation.PendingEvent
import dev.slne.surf.nats.descriptor.NatsEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Runs code as a handler of a transactional service, so that services can be tested with the [TransactionalEvents]
 * they receive in production.
 *
 * [runInHandler] runs its block like `registerTransactionalService` runs a persistent handler: in one READ COMMITTED
 * transaction on the test database, which `suspendTransaction` on that database joins. Nothing is sent to NATS;
 * [published] lists the events each committed block would publish. A block that throws rolls back and adds nothing.
 *
 * Thread-safe; blocks may run concurrently.
 */
class TransactionalHandlerFixture private constructor(
    private val operations: AtomicOperations,
    private val owner: String,
) {
    private val committed = ConcurrentLinkedQueue<PublishedTransactionalEvent>()

    /**
     * The publisher to pass to the services under test.
     */
    val events = TransactionalEvents()

    /**
     * Events of all committed [runInHandler] blocks since the creation or the last [clear], in publish order within a
     * block; blocks that commit concurrently may interleave.
     */
    val published: List<PublishedTransactionalEvent>
        get() = committed.toList()

    /**
     * Events of type [E] in [published], in the same order.
     */
    inline fun <reified E : NatsEvent> publishedOf(): List<E> = published.map { it.event }.filterIsInstance<E>()

    /**
     * Runs [block] as a transactional handler and returns its result.
     *
     * Must not be called inside an open transaction. After a deadlock or serialization failure the block runs again,
     * as the redelivery does in production; only the events of the run that commits are recorded.
     *
     * @throws IllegalStateException if called inside an open transaction
     */
    suspend fun <R> runInHandler(block: suspend () -> R): R {
        var result: Result<R>? = null
        var events = emptyList<PendingEvent>()
        val key = OperationKey(owner, OPERATION, UUID.randomUUID().toString())

        operations.execute(key, FINGERPRINT) {
            val operation = ActiveOperation()
            result = Result.success(withContext(operation) { block() })
            events = operation.complete()
            EMPTY_REPLY
        }

        events.mapTo(committed) { PublishedTransactionalEvent(it.event, it.aggregateId) }
        return checkNotNull(result).getOrThrow()
    }

    /**
     * Forgets the recorded events.
     */
    fun clear() {
        committed.clear()
    }

    companion object {
        private const val DEFAULT_OWNER = "test"
        private const val OPERATION = "test/runInHandler"
        private val FINGERPRINT = byteArrayOf(1)
        private val EMPTY_REPLY = StoredReply("none", ByteArray(0))

        /**
         * Migrates the reliability schema on [databaseApi] and creates a fixture whose operations belong to [owner].
         *
         * surf-nats must be on the test classpath, but NATS need not be started.
         */
        suspend fun create(databaseApi: DatabaseApi, owner: String = DEFAULT_OWNER): TransactionalHandlerFixture {
            withContext(Dispatchers.IO) { databaseApi.migrateReliabilitySchema() }
            return TransactionalHandlerFixture(AtomicOperations(databaseApi), owner)
        }
    }
}
