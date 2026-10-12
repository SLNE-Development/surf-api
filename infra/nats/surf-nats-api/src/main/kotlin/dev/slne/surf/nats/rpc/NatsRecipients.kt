package dev.slne.surf.nats.rpc

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class NatsRecipients(val recipients: Recipients)
