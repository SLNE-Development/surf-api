package dev.slne.surf.nats

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class NatsService(
    /**
     * If empty, the kebab-case name of the class will be used
     */
    val name: String = "",
    val responders: ResponderMode = ResponderMode.ONE,
)
