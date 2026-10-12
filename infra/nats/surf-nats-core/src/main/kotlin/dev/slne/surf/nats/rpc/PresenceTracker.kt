package dev.slne.surf.nats.rpc

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.expireAfterWrite
import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.codec.header
import dev.slne.surf.nats.codec.presenceKind
import dev.slne.surf.nats.connection.NatsConnectionManager
import io.nats.client.Dispatcher
import java.util.concurrent.atomic.AtomicLong

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
            val origin = message.header(NatsHeaders.ORIGIN) ?: return@subscribe
            when (message.presenceKind()) {
                NatsHeaders.PRESENCE_JOIN, NatsHeaders.PRESENCE_BEAT -> seen(origin)
                NatsHeaders.PRESENCE_LEAVE -> left(origin)
            }
        }
        queriedAtReconnects.set(reconnects())
        query()
    }

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

    fun allAnswered(answered: Set<String>): Boolean {
        if (System.nanoTime() - warmAt < 0) return false

        val live = members.asMap().keys.toList()
        return live.isNotEmpty() && answered.containsAll(live)
    }

    private fun query() {
        warmAt = System.nanoTime() + RpcDefaults.PRESENCE_WARMUP.inWholeNanoseconds
        client.publishPresence(subject, NatsHeaders.PRESENCE_QUERY)
    }

    private fun reconnects() = NatsConnectionManager.connection().statistics.reconnects
}
