package dev.slne.surf.nats.descriptor

import dev.slne.surf.nats.NatsGeneratedApi
import kotlinx.serialization.KSerializer

/**
 * Describes a generated event class.
 *
 * [protoName] is the proto full name of the event message; the event subject is derived from it.
 */
public class NatsEventDescriptor<T : Any> @NatsGeneratedApi constructor(
    public val protoName: String,
    public val serializer: KSerializer<T>,
)
