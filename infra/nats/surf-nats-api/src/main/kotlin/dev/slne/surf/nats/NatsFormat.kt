package dev.slne.surf.nats

/**
 * Selects the format sent by an event publisher or service proxy.
 *
 * Applies to event classes, service interfaces, and service methods. A method's annotation takes
 * precedence over its interface's annotation. The default is [WireFormat.CBOR]. Receivers decode
 * messages using the incoming format, and service responses use the request's format. Sender and
 * receiver annotations need not match.
 *
 * @property format the outgoing serialization format.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class NatsFormat(val format: WireFormat)
