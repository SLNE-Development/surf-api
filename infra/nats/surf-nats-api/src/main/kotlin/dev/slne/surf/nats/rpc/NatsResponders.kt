package dev.slne.surf.nats.rpc

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class NatsResponders(val mode: ResponderMode)
