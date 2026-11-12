package dev.slne.surf.microservice.reliability

import dev.slne.surf.microservice.reliability.integration.ReliabilityHolder
import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.NatsRegistration
import kotlin.reflect.KClass

/**
 * Registers [implementation] as service [type] whose persistent methods run at most once per message, each in one
 * database transaction.
 *
 * Every persistent fire call and persistent RPC of the registration runs in a READ COMMITTED transaction on the
 * microservice's database. The handler joins it with `suspendTransaction` on that database. The handler's statements
 * and its encoded reply commit together, and the events it publishes through [TransactionalEvents] are published on
 * this client after the commit. A redelivery, a concurrent delivery on another instance or a dead-letter replay of a
 * committed message receives the stored reply without running the handler or publishing events. Stored replies are
 * kept for the `retention` of [ReliabilityConfig.atomicOperations].
 *
 * "Once per message" refers to the message identity of surf-nats, which stays the same across redeliveries and
 * dead-letter replays. A caller that retries after a timeout sends a new message, unless it repeats an idempotency
 * key. Deduplicate such retries with a domain key.
 *
 * Failures:
 * - A handler exception rolls back, stores nothing and is handled by surf-nats as usual: a persistent RPC answers
 *   with the error, and a fire call is redelivered and dead-lettered after its last delivery.
 * - If the database is unavailable, a concurrent delivery holds the message longer than the lock timeout, the
 *   commit fails, or a deadlock rolls back the transaction after the handler ran, the message is redelivered instead
 *   of answered.
 * - A message id reused with another request is answered with an `AtomicOperationConflictException` error.
 *
 * Methods that are not persistent run without a transaction and without deduplication. Events the handlers publish
 * through [TransactionalEvents] carry this client's origin, so its listeners see them as their own. The client must
 * stay open while handlers run.
 *
 * ```kotlin
 * class PunishServiceImpl(private val events: TransactionalEvents) : PunishService {
 *     override suspend fun ban(request: BanRequest) {
 *         suspendTransaction(db = getMicroservice<PunishMicroservice>().database) {
 *             BanTable.insert { it[player] = request.player }
 *         }
 *         events.publish(PlayerBannedEvent(player = request.player), aggregateId = request.player)
 *     }
 * }
 *
 * object PunishMicroservice : Microservice() {
 *     override suspend fun onBootstrap(args: List<String>) {
 *         val client = NatsClient.create("punish")
 *         client.registerTransactionalService(PunishService::class, PunishServiceImpl(events))
 *     }
 * }
 * ```
 *
 * @throws IllegalStateException if the reliability integration is not running, or as described by
 * [NatsClient.registerService]
 */
public suspend fun <S : Any> NatsClient.registerTransactionalService(type: KClass<S>, implementation: S): NatsRegistration =
    registerService(type, implementation, ReliabilityHolder.runtime.interceptorFor(this))

/**
 * Registers [implementation] as transactional service [S].
 *
 * Shortcut for the overload with a [KClass], which describes the semantics.
 */
public suspend inline fun <reified S : Any> NatsClient.registerTransactionalService(implementation: S): NatsRegistration =
    registerTransactionalService(S::class, implementation)
