package dev.slne.surf.nats.embedded

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.NatsRegistration
import org.jetbrains.annotations.Blocking
import kotlin.reflect.KClass

/**
 * Controls of a [NatsClient] that a dependency-injection integration needs on top of the public
 * client API.
 *
 * Every client created by `NatsClient.create` implements it; obtain it with [EmbeddedNats.control].
 */
@InternalNatsApi
interface NatsClientControl {

    /**
     * Takes part in the shared connection and connects when no other client holds it yet.
     *
     * Idempotent. The client releases its share when it closes, and the connection closes with the
     * last share. Throws [IllegalStateException] once the client is closed, and when the
     * configuration is invalid or no server is reachable.
     */
    @Blocking
    fun connect()

    /**
     * Returns a remote service proxy like [NatsClient.service], but creating it needs no
     * connection.
     *
     * The first call through the proxy starts its presence tracking. A call made while the client
     * is not connected fails with [IllegalStateException].
     */
    fun <S : Any> lazyService(type: KClass<S>): S

    /**
     * Registers the `@NatsListener` methods that [scanType] and its superclasses declare, bound to
     * [listener].
     *
     * [scanType] lets a proxied bean register the methods of the class behind the proxy. [listener]
     * must be an instance of [scanType]. Private and final listener methods are rejected when
     * [scanType] is not the class of [listener], because a proxy cannot intercept them.
     */
    suspend fun registerListener(listener: Any, scanType: Class<*>): NatsRegistration

    /**
     * Stops inbound work of this client only: rejects new registrations and drains the existing
     * ones.
     *
     * Publishing and service calls keep working until [NatsClient.close]. Blocks until the drain
     * finishes, which can take up to `(2 × services + 3) × drainTimeout`.
     */
    @Blocking
    fun stopAccepting()
}
