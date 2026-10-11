package dev.slne.surf.nats.rpc

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class NatsResponders(val mode: ResponderMode)
