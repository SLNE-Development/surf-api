package dev.slne.surf.nats.rpc

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.expireAfterWrite
import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.codec.header
import dev.slne.surf.nats.codec.presenceKind
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.protocol.ProtocolHeaders
import io.nats.client.Dispatcher
import java.util.concurrent.atomic.AtomicLong

/**
 * Tracks the live responder instances of a service, so an `ALL_INSTANCES` call with an answer can end once every
 * instance has answered.
 *
 * Instances are identified by `Surf-Origin`. A `join` or `beat` keeps an instance for [RpcDefaults.PRESENCE_TTL], and
 * a `leave` removes it. [start] and [refreshAfterReconnect] query all instances and open a warm-up of
 * [RpcDefaults.PRESENCE_WARMUP] during which [allAnswered] returns `false`.
 */
@InternalNatsApi
internal class PresenceTracker(
    private val client: NatsClientImpl,
    private val subject: String,
) {
    @Volatile
    private var warmAt = 0L

    private val queriedAtReconnects = AtomicLong()

    private val members = Caffeine.newBuilder()
        .expireAfterWrite(RpcDefaults.PRESENCE_TTL)
        .executor(Runnable::run)
        .build<String, Unit>()

    fun start(dispatcher: Dispatcher) {
        dispatcher.subscribe(subject) { message ->
            val origin = message.header(ProtocolHeaders.ORIGIN) ?: return@subscribe
            when (message.presenceKind()) {
                ProtocolHeaders.PRESENCE_JOIN, ProtocolHeaders.PRESENCE_BEAT -> seen(origin)
                ProtocolHeaders.PRESENCE_LEAVE -> left(origin)
            }
        }
        queriedAtReconnects.set(reconnects())
        query()
    }

    /**
     * Queries all instances again when the connection reconnected since the last query. Concurrent callers
     * send one query per reconnect.
     */
    fun refreshAfterReconnect() {
        val reconnects = reconnects()
        val queried = queriedAtReconnects.get()
        if (reconnects != queried && queriedAtReconnects.compareAndSet(queried, reconnects)) {
            query()
        }
    }

    fun seen(origin: String) {
        members.put(origin, Unit)
    }

    private fun left(origin: String) {
        members.invalidate(origin)
    }

    /**
     * Returns whether every live instance is in [answered]. Returns `false` during the warm-up and while no instance
     * is known.
     *
     * Callers check this after each reply, so an instance that leaves while a call waits does not end the call.
     */
    fun allAnswered(answered: Set<String>): Boolean {
        if (System.nanoTime() - warmAt < 0) return false

        val live = members.asMap().keys.toList()
        return live.isNotEmpty() && answered.containsAll(live)
    }

    private fun query() {
        warmAt = System.nanoTime() + RpcDefaults.PRESENCE_WARMUP.inWholeNanoseconds
        client.publishPresence(subject, ProtocolHeaders.PRESENCE_QUERY)
    }

    private fun reconnects() = NatsConnectionManager.connection().statistics.reconnects
}
