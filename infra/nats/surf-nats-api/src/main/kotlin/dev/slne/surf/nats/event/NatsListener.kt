package dev.slne.surf.nats.event

import dev.slne.surf.nats.rpc.Recipients

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class NatsListener(
    /**
     * [Recipients.ANY_INSTANCE] delivers each event to only one instance of the client across all servers
     */
    val recipients: Recipients = Recipients.ALL_INSTANCES,

    /**
     * If true, events published by this client on this instance are ignored. Requires
     * [Recipients.ALL_INSTANCES].
     */
    val ignoreFromThisClient: Boolean = false,
)
