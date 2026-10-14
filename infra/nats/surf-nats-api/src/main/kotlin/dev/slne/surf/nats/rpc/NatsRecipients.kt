package dev.slne.surf.nats.rpc

/**
 * Selects the instances addressed by a service call.
 *
 * A method's annotation takes precedence over its interface's annotation. The default is
 * [Recipients.ANY_INSTANCE]. See [NatsService] for delivery semantics for each combination of
 * persistence and return type.
 *
 * @property recipients the instances to address.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class NatsRecipients(val recipients: Recipients)
