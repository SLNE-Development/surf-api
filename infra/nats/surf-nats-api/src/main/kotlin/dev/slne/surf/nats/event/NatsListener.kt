package dev.slne.surf.nats.event

import dev.slne.surf.nats.rpc.Recipients

/**
 * Marks an event listener method with a single parameter whose type is annotated with [NatsEvent].
 *
 * The method must return [Unit] and may be suspending. Registration scans the class hierarchy,
 * excluding interfaces. Overrides of annotated listeners must repeat this annotation and are
 * registered only once.
 *
 * ```kotlin
 * @NatsEvent
 * data class StatusChanged(val status: String)
 *
 * class StatusListener {
 *     @NatsListener
 *     suspend fun onStatus(event: StatusChanged) {
 *         val context = natsEventContext()
 *         println("${context.origin}: ${event.status}")
 *     }
 * }
 * ```
 *
 * @property recipients [Recipients.ALL_INSTANCES] delivers to each listening instance.
 * [Recipients.ANY_INSTANCE] selects one listening instance among clients with the same name.
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
