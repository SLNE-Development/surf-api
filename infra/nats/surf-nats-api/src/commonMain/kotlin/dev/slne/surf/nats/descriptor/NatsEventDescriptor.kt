package dev.slne.surf.nats.descriptor

import dev.slne.surf.nats.NatsGeneratedApi
import kotlinx.serialization.KSerializer

/**
 * Describes a generated event class.
 *
 * Only generated code creates instances. Every generated event class exposes its descriptor as the static field
 * `NATS_DESCRIPTOR` and through [NatsEvent.natsDescriptor].
 *
 * @property protoName the proto full name of the event message, such as `surf.demo.v1.StatusChanged`. The event
 * subject derives from it, so renaming the message or its package in the `.proto` breaks the protocol.
 * @property serializer the serializer of the event class.
 */
public class NatsEventDescriptor<T : Any> @NatsGeneratedApi constructor(
    public val protoName: String,
    public val serializer: KSerializer<T>,
)
