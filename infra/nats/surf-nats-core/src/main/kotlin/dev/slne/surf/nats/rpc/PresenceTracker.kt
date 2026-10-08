package dev.slne.surf.nats.rpc

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.expireAfterWrite
import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.WireFormat
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.codec.NatsHeaders
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
            if (!message.hasHeaders()) return@subscribe
            val origin = message.headers.getFirst(NatsHeaders.ORIGIN) ?: return@subscribe
            when (message.presenceKind()) {
                NatsHeaders.PRESENCE_JOIN, NatsHeaders.PRESENCE_BEAT -> seen(origin)
                NatsHeaders.PRESENCE_LEAVE -> left(origin)
            }
        }
        val headers = NatsHeaders.base(client.name, WireFormat.CBOR)
            .add(NatsHeaders.PRESENCE, NatsHeaders.PRESENCE_QUERY)
        NatsConnectionManager.connection().publish(subject, headers, EMPTY)
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

    companion object {
        private val EMPTY = ByteArray(0)
    }
}