package dev.slne.surf.nats.event

import dev.slne.surf.nats.rpc.Recipients

/**
 * Marks an event listener method with a single parameter whose type is a generated event message
 * ([dev.slne.surf.nats.descriptor.NatsEvent]).
 *
 * The method must return [Unit] and may be suspending. [dev.slne.surf.nats.NatsClient.registerListener]
 * scans the class hierarchy, excluding interfaces. Overrides of annotated listeners must repeat this
 * annotation and are registered only once.
 *
 * Listeners run concurrently on [kotlinx.coroutines.Dispatchers.Default] and must not block it. An
 * event that cannot be decoded is dropped and logged, and a listener that throws does not affect the
 * other listeners. Suspending listeners can read the publisher metadata with [natsEventContext].
 *
 * ```kotlin
 * // StatusChanged is generated from a .proto message with the (surf.nats.v1.event) option.
 * class StatusListener {
 *     @NatsListener
 *     suspend fun onStatus(event: StatusChanged) {
 *         val context = natsEventContext()
 *         println("${context.origin}: ${event.status}")
 *     }
 * }
 * ```
 *
 * @property recipients [Recipients.ALL_INSTANCES] delivers each event to every listening client.
 * [Recipients.ANY_INSTANCE] delivers each event to one listening client among all clients with the same
 * [dev.slne.surf.nats.NatsClient.name], which balances the events between the instances of one
 * application.
 * @property ignoreFromThisClient whether to skip events published by this client in this JVM.
 * Events from other clients in the same JVM are treated as coming from a different client.
 * Enabling this option requires [Recipients.ALL_INSTANCES]. Other combinations are rejected
 * at registration.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class NatsListener(
    val recipients: Recipients = Recipients.ALL_INSTANCES,

    val ignoreFromThisClient: Boolean = false,
)
