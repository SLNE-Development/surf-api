package dev.slne.surf.nats.event

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class NatsListener(
    /**
     * If true, the listener will only be called once across all instances of the application
     */
    val loadBalanced: Boolean = false,

    /**
     * If true, events published by this instance will be ignored. Can only be enabled if
     * [loadBalanced] is set to false.
     */
    val ignoreOwn: Boolean = false,
)
