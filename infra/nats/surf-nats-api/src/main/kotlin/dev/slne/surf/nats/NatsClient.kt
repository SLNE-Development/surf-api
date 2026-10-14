package dev.slne.surf.nats

import dev.slne.surf.nats.internal.NatsClientFactory
import dev.slne.surf.nats.internal.NatsClientName
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Blocking
import org.jetbrains.annotations.CheckReturnValue
import kotlin.reflect.KClass

/**
 * Provides event publishing, listener registration, and remote service calls for one logical application.
 *
 * Create one client per application [name] and close it during application shutdown.
 *
 * ```kotlin
 * @NatsEvent
 * data class StatusChanged(val status: String)
 *
 * @NatsService
 * interface StatusService {
 *     suspend fun status(): String
 * }
 *
 * suspend fun example() {
 *     val client = NatsClient.create("status-app")
 *     try {
 *         client.registerListener(object {
 *             @NatsListener
 *             fun onStatus(event: StatusChanged) { println(event.status) }
 *         })
 *         client.registerService<StatusService>(object : StatusService {
 *             override suspend fun status() = "ready"
 *         })
 *         val service = client.service<StatusService>()
 *         client.publish(StatusChanged(service.status()))
 *     } finally {
 *         client.close()
 *     }
 * }
 * ```
 */
@ApiStatus.NonExtendable
@MustUseReturnValues
public interface NatsClient : AutoCloseable {
    /**
     * Identifies the application on the network using lowercase alphanumeric groups separated by hyphens.
     *
     * Prefixes non-global events and services and groups [dev.slne.surf.nats.rpc.Recipients.ANY_INSTANCE]
     * listeners and non-global service handlers. Changing this name changes the protocol.
     */
    public val name: String

    /**
     * Whether this client has been closed.
     */
    public val isClosed: Boolean

    /**
     * Publishes an event without waiting for delivery or flushing the connection.
     *
     * The event class must be annotated with [dev.slne.surf.nats.event.NatsEvent]. Returns once the
     * message has been handed to the connection. Delivery is not guaranteed. Events exceeding the
     * server's payload limit are rejected and are never offloaded. Serialization failures propagate
     * to the caller.
     *
     * @throws IllegalArgumentException if the event annotation or subject is invalid, or the message is too large.
     * @throws IllegalStateException if the client is closed or no NATS connection is initialized.
     */
    public fun publish(event: Any)

    /**
     * Registers the [dev.slne.surf.nats.event.NatsListener] methods in the listener's class hierarchy.
     *
     * Validation problems are reported together. Returns once the server has processed the
     * subscriptions, so subsequent publications can be received. Closing the returned
     * [NatsRegistration] removes these listeners without waiting for running invocations.
     *
     * Listeners run concurrently on [kotlinx.coroutines.Dispatchers.Default], with no ordering
     * guarantee. Move blocking work to [kotlinx.coroutines.Dispatchers.IO] using
     * [kotlinx.coroutines.withContext]. Listener failures are logged and do not affect other listeners.
     *
     * @throws IllegalArgumentException if listener methods are invalid or none are found.
     * @throws IllegalStateException if the client is closed or a subject already belongs to another event type.
     */
    @IgnorableReturnValue
    public suspend fun registerListener(listener: Any): NatsRegistration

    /**
     * Registers an implementation of the [dev.slne.surf.nats.rpc.NatsService] interface [type].
     *
     * The implementation must implement [type]. Interface validation problems are reported together.
     * This client allows only one active registration for each service name within its global or
     * client scope. Registration returns once the server has processed the subscriptions and, for
     * persistent methods, storage and consumers have been provisioned.
     *
     * Handlers run on [kotlinx.coroutines.Dispatchers.Default] and may run concurrently. Move blocking
     * work to [kotlinx.coroutines.Dispatchers.IO] using [kotlinx.coroutines.withContext].
     *
     * @throws IllegalArgumentException if the interface, its methods, or the implementation are invalid.
     * @throws IllegalStateException if the client is closed or the service is already registered.
     */
    @IgnorableReturnValue
    public suspend fun <S : Any> registerService(type: KClass<S>, implementation: S): NatsRegistration

    /**
     * Returns a remote service proxy cached per interface on this client.
     *
     * Validates [type] using the same rules as [registerService]. Proxy calls follow the delivery
     * and failure semantics described by [dev.slne.surf.nats.rpc.NatsService] and may throw subtypes
     * of [dev.slne.surf.nats.rpc.NatsRpcException]. Calls started after this client is closed throw
     * [IllegalStateException].
     *
     * @throws IllegalArgumentException if the service interface or its methods are invalid.
     * @throws IllegalStateException if the client is closed.
     */
    @CheckReturnValue
    public fun <S : Any> service(type: KClass<S>): S

    /**
     * Closes all registrations, cancels remaining listener and handler coroutines, and releases [name].
     *
     * This operation is idempotent and may block while service registrations drain, as described
     * by [NatsRegistration.close]. The client cannot be reopened. Create a new client to reuse the name.
     * Close clients before the NATS instance is disabled and create new clients after it is loaded again.
     */
    @Blocking
    override fun close()

    /**
     * Creates clients for logical applications.
     */
    public companion object Factory {
        /**
         * Creates a client with a name unique among open clients in this JVM.
         *
         * @param name lowercase alphanumeric groups separated by hyphens, as described by [NatsClient.name].
         * @param serializersModule contextual and polymorphic serializers for events, service arguments,
         * and return values.
         * @throws IllegalArgumentException if [name] is invalid.
         * @throws IllegalStateException if an open client already uses [name].
         */
        @CheckReturnValue
        public fun create(
            @NatsClientName name: String,
            serializersModule: SerializersModule = EmptySerializersModule()
        ): NatsClient {
            return NatsClientFactory.instance.create(name, serializersModule)
        }
    }
}