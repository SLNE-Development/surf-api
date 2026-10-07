package dev.slne.surf.nats

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class NatsTimeout(val seconds: Long)
