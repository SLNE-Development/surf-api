package dev.slne.surf.nats

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class NatsFormat(val format: WireFormat)
