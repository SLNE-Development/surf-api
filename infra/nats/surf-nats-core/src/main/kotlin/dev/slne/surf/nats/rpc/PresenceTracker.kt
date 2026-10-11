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

@InternalNatsApi
internal class PresenceTracker(
    private val client: NatsClientImpl,
    private val subject: String,
) {
    private val warmAt = System.nanoTime() + RpcDefaults.PRESENCE_WARMUP.inWholeNanoseconds

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
        try {
            NatsConnectionManager.connection()
                .publish(subject, NatsHeaders.presence(client.name, NatsHeaders.PRESENCE_QUERY), NatsHeaders.EMPTY_BODY)
        } catch (e: Exception) {
            client.logger.debug("Failed to query presence on $subject", e)
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

        val live = members.asMap().keys
        return live.isNotEmpty() && answered.containsAll(live)
    }
}
