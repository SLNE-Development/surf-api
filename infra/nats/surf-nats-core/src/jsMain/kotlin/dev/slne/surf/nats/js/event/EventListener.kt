package dev.slne.surf.nats.js.event

/**
 * One handler registered with `listen`, the JS counterpart of the JVM `ListenerMethod`.
 */
internal class EventListener(
    val descriptor: EventDescriptor,
    val subject: String,
    val queueGroup: String?,
    val ignoreFromThisClient: Boolean,
    val handler: EventHandler,
)
