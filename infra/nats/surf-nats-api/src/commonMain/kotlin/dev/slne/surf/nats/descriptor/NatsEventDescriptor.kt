package dev.slne.surf.nats.descriptor

import dev.slne.surf.nats.NatsGeneratedApi
import kotlinx.serialization.KSerializer

public class NatsEventDescriptor<T : Any> @NatsGeneratedApi constructor(
    public val protoName: String,
    public val subject: String,
    public val global: Boolean,
    public val majorVersion: Int,
    public val serializer: KSerializer<T>,
)
