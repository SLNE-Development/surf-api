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
 * transaction on the test database, which `suspendTransaction` on that database joins. Nothing is sent to NATS.
 * [published] lists the events each committed block would publish. A block that throws rolls back and adds nothing.
 *
 * Thread-safe. Blocks may run concurrently.
 *
 * ```kotlin
 * val fixture = TransactionalHandlerFixture.create(databaseApi)
 * val service = PunishServiceImpl(fixture.events)
 *
 * fixture.runInHandler { service.ban(BanRequest(player = "steve")) }
 *
 * assertEquals(listOf("steve"), fixture.publishedOf<PlayerBannedEvent>().map { it.player })
 * ```
 */
public class TransactionalHandlerFixture private constructor(
    private val operations: AtomicOperations,
    private val owner: String,
) {
    private val committed = ConcurrentLinkedQueue<PublishedTransactionalEvent>()

    /**
     * Gets the publisher to pass to the services under test.
     *
     * Its [publish][TransactionalEvents.publish] works only inside [runInHandler] and throws an
     * [IllegalStateException] elsewhere.
     */
    public val events: TransactionalEvents = TransactionalEvents()

    /**
     * Gets the events of all committed [runInHandler] blocks since the creation or the last [clear].
     *
     * Returns a snapshot. Events of one block keep their publish order. Blocks that commit concurrently may
     * interleave.
     */
    public val published: List<PublishedTransactionalEvent>
        get() = committed.toList()

    /**
     * Returns the events of type [E] in [published], in the same order.
     */
    public inline fun <reified E : NatsEvent> publishedOf(): List<E> = published.map { it.event }.filterIsInstance<E>()

    /**
     * Runs [block] as a transactional handler and returns its result.
     *
     * Must not be called inside an open transaction. After a deadlock or serialization failure the block runs again,
     * as the redelivery does in production. Only the events of the run that commits are recorded. An exception from
     * [block] rolls back the transaction and is rethrown.
     *
     * @throws IllegalStateException if called inside an open transaction
     */
    public suspend fun <R> runInHandler(block: suspend () -> R): R {
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
    public fun clear() {
        committed.clear()
    }

    public companion object {
        private const val DEFAULT_OWNER = "test"
        private const val OPERATION = "test/runInHandler"
        private val FINGERPRINT = byteArrayOf(1)
        private val EMPTY_REPLY = StoredReply("none", ByteArray(0))

        /**
         * Creates a fixture on [databaseApi] after migrating the reliability schema there.
         *
         * surf-nats must be on the test classpath, but NATS need not be started.
         *
         * @param databaseApi the test database, which must be MariaDB or PostgreSQL
         * @param owner the owner under which the fixture stores its operations, in production the microservice name
         */
        public suspend fun create(databaseApi: DatabaseApi, owner: String = DEFAULT_OWNER): TransactionalHandlerFixture {
            withContext(Dispatchers.IO) { databaseApi.migrateReliabilitySchema() }
            return TransactionalHandlerFixture(AtomicOperations(databaseApi), owner)
        }
    }
}
