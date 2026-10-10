package dev.slne.surf.nats.rpc

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
public annotation class NatsParam(
    /**
     * Serial name of the argument, used instead of the parameter name
     */
    val name: String,
)
