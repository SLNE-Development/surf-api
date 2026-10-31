package dev.slne.surf.nats.descriptor

import dev.slne.surf.nats.NatsGeneratedApi

/**
 * Describes a generated service interface.
 *
 * [protoName] is the proto full name of the service; its subjects, streams and queue group are derived from it.
 */
public class NatsServiceDescriptor<S : Any> @NatsGeneratedApi constructor(
    public val protoName: String,
    public val methods: List<NatsMethodDescriptor<*, *>>,
)
