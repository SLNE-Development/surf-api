package dev.slne.surf.nats

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Responders(val mode: ResponderMode)
