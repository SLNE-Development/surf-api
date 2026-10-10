package dev.slne.surf.nats.rpc

// TODO: 10.10.2026 16:58 - document the wire contract for external callers

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
public annotation class NatsService(
    /**
     * If empty, the kebab-case name of the class will be used
     */
    val name: String = "",
    val responders: ResponderMode = ResponderMode.ONE,

    /**
     * If false, the service is addressed without the client prefix and can be called and served by any client
     */
    val prefixed: Boolean = true,
)
