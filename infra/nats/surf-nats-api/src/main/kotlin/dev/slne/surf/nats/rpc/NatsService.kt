package dev.slne.surf.nats.rpc

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
public annotation class NatsService(
    /**
     * If empty, the kebab-case name of the class will be used
     */
    val name: String = "",
    val responders: ResponderMode = ResponderMode.ONE,
)
