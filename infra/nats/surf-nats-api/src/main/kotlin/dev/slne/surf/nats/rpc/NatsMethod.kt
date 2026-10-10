package dev.slne.surf.nats.rpc

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class NatsMethod(
    /**
     * Subject token of the method. If empty, the kebab-case name of the method will be used
     */
    val name: String = "",
)
