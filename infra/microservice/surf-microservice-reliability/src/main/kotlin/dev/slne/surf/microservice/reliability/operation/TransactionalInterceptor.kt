package dev.slne.surf.microservice.reliability.operation

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.database.reliability.operation.AtomicOperationConflictException
import dev.slne.surf.database.reliability.operation.AtomicOperationInProgressException
import dev.slne.surf.database.reliability.operation.AtomicOperations
import dev.slne.surf.database.reliability.operation.OperationKey
import dev.slne.surf.database.reliability.operation.StoredReply
import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.rpc.NatsPersistentCall
import dev.slne.surf.nats.rpc.NatsPersistentInterceptor
import dev.slne.surf.nats.rpc.NatsPersistentReply
import dev.slne.surf.nats.rpc.NatsRedeliveryException
import kotlinx.coroutines.withContext
import java.util.HexFormat
import kotlin.coroutines.cancellation.CancellationException

private val log = logger()

/**
 * Runs every persistent handler of a service registered on [client] at most once per message in one database
 * transaction, and publishes the handler's events on [client] after the commit.
 *
 * The operation key is ([owner], `<proto service>/<rpc>`, [NatsPersistentCall.messageId]) and the fingerprint is
 * the SHA-256 of the request bytes. The handler, its database statements and its encoded reply commit together; a
 * redelivery, a concurrent delivery on another instance or a dead-letter replay of a committed message receives the
 * stored reply without running the handler.
 *
 * Events published through [TransactionalEvents][dev.slne.surf.microservice.reliability.TransactionalEvents] are
 * published only if the handler run of this delivery committed: not after a failure, a rollback or a stored reply.
 * A failed publish is logged and does not affect the reply.
 *
 * Failure mapping:
 * - Handler exceptions propagate unchanged and nothing is stored, so surf-nats treats them as without interceptor.
 * - A reused message id with another request is answered with an [AtomicOperationConflictException] error reply.
 * - Every other failure, such as an unavailable database, a lock wait timeout or a failed commit, becomes a
 *   [NatsRedeliveryException]: no reply is sent and the message is delivered again.
 * - A deadlock or serialization failure after the handler ran also becomes a redelivery, because a delivery can
 *   run its handler only once.
 */
internal class TransactionalInterceptor(
    private val owner: String,
    private val operations: AtomicOperations,
    private val client: NatsClient,
) : NatsPersistentInterceptor {

    override suspend fun intercept(call: NatsPersistentCall): NatsPersistentReply {
        val key = OperationKey(owner, "${call.serviceName}/${call.methodName}", call.messageId)
        val fingerprint = HexFormat.of().parseHex(call.requestFingerprint)
        var handlerStarted = false
        var handlerRun: HandlerRun? = null

        val stored = try {
            operations.execute(key, fingerprint) {
                if (handlerStarted) throw HandlerRerunRejected()
                handlerStarted = true
                runHandler(call).also { handlerRun = it }.reply
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: AtomicOperationConflictException) {
            return NatsPersistentReply.error(e)
        } catch (e: Exception) {
            throw handlerFailureOrRedelivery(key, e)
        }

        handlerRun?.takeIf { it.committed(stored) }?.let { publishCommitted(key, it.events) }
        return NatsPersistentReply.restore(stored.format, stored.bytes)
    }

    private suspend fun runHandler(call: NatsPersistentCall): HandlerRun {
        val operation = ActiveOperation()
        val reply = try {
            withContext(operation) { call.proceed() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw HandlerFailure(e)
        }
        return HandlerRun(StoredReply(reply.format, reply.bytes), operation.complete())
    }

    private fun publishCommitted(key: OperationKey, events: List<PendingEvent>) {
        for (pending in events) {
            try {
                client.publish(pending.event)
            } catch (e: Exception) {
                log.atWarning()
                    .withCause(e)
                    .log("Event %s of aggregate %s, committed by operation %s, could not be published and is lost",
                        pending.event.javaClass.name, pending.aggregateId ?: "-", key)
            }
        }
    }

    private fun handlerFailureOrRedelivery(key: OperationKey, failure: Exception): Exception {
        val handlerFailure = generateSequence<Throwable>(failure) { it.cause }.filterIsInstance<HandlerFailure>()
            .firstOrNull()
        if (handlerFailure != null) return handlerFailure.cause as Exception

        val reason = when (failure) {
            is AtomicOperationInProgressException -> "is still running on another delivery"
            is HandlerRerunRejected -> "must be retried after a transient database conflict"
            else -> "could not be completed in the database"
        }
        return NatsRedeliveryException("Operation $key $reason", failure)
    }

    /**
     * The handler run of one delivery: the reply it returned to `AtomicOperations.execute` and its events.
     */
    private class HandlerRun(val reply: StoredReply, val events: List<PendingEvent>) {

        /**
         * Whether this run committed, given the reply [stored] that `execute` returned.
         *
         * Relies on `execute` returning the block's own reply instance when the block's transaction committed, and a
         * new instance when it replays a reply stored by another delivery. After a transient failure of this run's
         * commit, a retry can replay the reply of a concurrent delivery; this run's events then belong to a
         * rolled-back transaction.
         */
        fun committed(stored: StoredReply): Boolean = stored === reply
    }
}
