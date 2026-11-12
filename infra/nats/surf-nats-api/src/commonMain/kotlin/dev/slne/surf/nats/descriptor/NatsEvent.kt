package dev.slne.surf.nats.descriptor

/**
 * Marks a class generated from a `.proto` message with the `(surf.nats.v1.event)` option as a publishable event.
 *
 * The Gradle plugin generates every implementation together with a static `NATS_DESCRIPTOR` field on the class.
 * The JVM client reads that field, not [natsDescriptor], so publishing or listening to a hand-written implementation
 * fails with an [IllegalArgumentException].
 */
public interface NatsEvent {
    /**
     * Gets the descriptor of the generated event class, the same instance as its static `NATS_DESCRIPTOR`.
     */
    public val natsDescriptor: NatsEventDescriptor<*>
}
