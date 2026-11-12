package dev.slne.surf.nats

import dev.slne.surf.nats.descriptor.NatsEvent
import dev.slne.surf.nats.internal.NatsClientFactory
import dev.slne.surf.nats.internal.NatsClientName
import dev.slne.surf.nats.rpc.NatsPersistentInterceptor
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Blocking
import org.jetbrains.annotations.CheckReturnValue
import kotlin.reflect.KClass

/**
 * Provides event publishing, listener registration, and remote service calls for one logical application.
 *
 * Create one client per application [name] with [create] and close it during application shutdown. A client does
 * not survive a restart of the NATS instance: close it before the instance is disabled and create a new one after the
 * instance is loaded again.
 *
 * Events and services are classes that the `dev.slne.surf.nats` Gradle plugin generates from `.proto` files. In the
 * following example, `StatusChanged` is generated from a message with the `(surf.nats.v1.event)` option, and
 * `StatusService` from a service whose `Status` rpc takes `google.protobuf.Empty` and returns a `StatusReply`:
 *
 * ```kotlin
 * suspend fun example() {
 *     val client = NatsClient.create("status-app")
 *     try {
 *         client.registerListener(object {
 *             @NatsListener
 *             fun onStatus(event: StatusChanged) { println(event.status) }
 *         })
 *         client.registerService<StatusService>(object : StatusService {
 *             override suspend fun status() = StatusReply("ready")
 *         })
 *         val service = client.service<StatusService>()
 *         client.publish(StatusChanged(service.status().status))
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
     * Appears in the `Surf-Origin` header of outgoing messages and is the queue group of
     * [dev.slne.surf.nats.rpc.Recipients.ANY_INSTANCE] listeners, so each application receives such an event once.
     * Subjects, streams and service queue groups derive from the proto names of the contracts, not from this name.
     */
    public val name: String

    /**
     * Whether the client is closed.
     *
     * Becomes `true` once [close] has drained the registrations, and remains `false` during the drain.
     */
    public val isClosed: Boolean

    /**
     * Publishes an event without waiting for delivery or flushing the connection.
     *
     * The event must be an instance of a generated event message. Returns once the message has been
     * handed to the connection. Delivery is not guaranteed. Events exceeding the server's payload
     * limit are rejected and are never offloaded. Serialization failures propagate to the caller.
     * Each call sends a new random event ID, see [dev.slne.surf.nats.event.NatsEventContext.eventId].
     *
     * Listeners registered on this client can receive the event too, unless they set
     * [dev.slne.surf.nats.event.NatsListener.ignoreFromThisClient].
     *
     * @throws IllegalArgumentException if the event class is not a generated event, its proto name is invalid, or the
     * message is too large.
     * @throws IllegalStateException if the client is closed or NATS is not connected.
     */
    public fun publish(event: NatsEvent)

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
     * @param listener the object whose annotated methods receive the events. The client keeps a strong reference to
     * it until the registration is closed.
     * @return the registration of all listener methods of [listener].
     * @throws IllegalArgumentException if listener methods are invalid or none are found.
     * @throws IllegalStateException if the client is closed or closing, NATS is not connected, or a subject already
     * belongs to another event type.
     * @throws java.util.concurrent.TimeoutException if the server does not confirm the subscriptions within 5 seconds.
     * The subscriptions of this call are removed again.
     */
    @IgnorableReturnValue
    public suspend fun registerListener(listener: Any): NatsRegistration

    /**
     * Registers an implementation of the generated service interface [type].
     *
     * The implementation must implement [type]. Interface validation problems are reported together.
     * This client allows only one active registration for each proto service name. All clients that register the
     * same service share its [dev.slne.surf.nats.rpc.Recipients.ANY_INSTANCE] and persistent requests, whatever
     * their [name]. Registration returns once the server has processed the subscriptions and, for persistent methods,
     * storage and consumers have been provisioned.
     *
     * Handlers run on [kotlinx.coroutines.Dispatchers.Default] and may run concurrently. Move blocking
     * work to [kotlinx.coroutines.Dispatchers.IO] using [kotlinx.coroutines.withContext].
     *
     * ```kotlin
     * val registration = client.registerService(StatusService::class, object : StatusService {
     *     override suspend fun status() = StatusReply("ready")
     * })
     * ```
     *
     * @param type the generated service interface, not the implementation class.
     * @param implementation the object that handles the calls.
     * @return the registration whose [NatsRegistration.close] stops serving the service.
     * @throws IllegalArgumentException if the interface, its methods, or the implementation are invalid.
     * @throws IllegalStateException if the client is closed or closing, NATS is not connected, or the service is
     * already registered.
     * @throws java.util.concurrent.TimeoutException if the server does not confirm the subscriptions within 5 seconds.
     * The registration is closed again.
     */
    @IgnorableReturnValue
    public suspend fun <S : Any> registerService(type: KClass<S>, implementation: S): NatsRegistration

    /**
     * Registers an implementation of the generated service interface [type] whose persistent methods run through
     * [interceptor].
     *
     * Behaves like [registerService] without an interceptor, except that every persistent fire call and persistent
     * RPC of this registration is handled by [NatsPersistentInterceptor.intercept]. Methods that are not persistent
     * do not use the interceptor.
     *
     * @param type the generated service interface, not the implementation class.
     * @param implementation the object that handles the calls.
     * @param interceptor the interceptor that handles every persistent call of this registration.
     * @return the registration whose [NatsRegistration.close] stops serving the service.
     * @throws IllegalArgumentException if the interface, its methods, or the implementation are invalid.
     * @throws IllegalStateException if the client is closed or closing, NATS is not connected, or the service is
     * already registered.
     * @throws java.util.concurrent.TimeoutException if the server does not confirm the subscriptions within 5 seconds.
     * The registration is closed again.
     */
    @IgnorableReturnValue
    public suspend fun <S : Any> registerService(
        type: KClass<S>,
        implementation: S,
        interceptor: NatsPersistentInterceptor,
    ): NatsRegistration

    /**
     * Returns a remote service proxy cached per interface on this client.
     *
     * Validates [type] using the same rules as [registerService]. Each method of the proxy sends a call to the
     * instances that serve the service, which can be in this or any other client. Proxy calls may throw subtypes
     * of [dev.slne.surf.nats.rpc.NatsRpcException]. Calls started after this client is closed throw
     * [IllegalStateException].
     *
     * ```kotlin
     * val status = client.service(StatusService::class)
     * val reply = status.status()
     * ```
     *
     * @param type the generated service interface.
     * @return the proxy, the same instance for repeated calls with the same [type].
     * @throws IllegalArgumentException if the service interface or its methods are invalid.
     * @throws IllegalStateException if the client is closed, or is closing and has no proxy for [type] yet.
     */
    @CheckReturnValue
    public fun <S : Any> service(type: KClass<S>): S

    /**
     * Closes all registrations, cancels remaining listener and handler coroutines, and releases [name].
     *
     * This operation is idempotent and may block while service registrations drain, as described
     * by [NatsRegistration.close]. During the drain, [publish] and calls through existing proxies keep
     * working for running handlers, while registering and creating new proxies throw
     * [IllegalStateException]. [isClosed] becomes `true` once the drain has finished.
     * The client cannot be reopened. Create a new client to reuse the name.
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
         * Creating a client opens no connection: the client uses the shared connection of the NATS instance.
         *
         * @param name lowercase alphanumeric groups separated by hyphens, as described by [NatsClient.name].
         * @throws IllegalArgumentException if [name] is invalid.
         * @throws IllegalStateException if an open client already uses [name].
         */
        @CheckReturnValue
        public fun create(@NatsClientName name: String): NatsClient {
            return NatsClientFactory.instance.create(name)
        }
    }
}