package dev.slne.surf.nats.descriptor

import dev.slne.surf.nats.NatsGeneratedApi

public class NatsServiceDescriptor<S : Any> @NatsGeneratedApi constructor(
    public val protoName: String,
    public val name: String,
    public val global: Boolean,
    public val majorVersion: Int,
    public val methods: List<NatsMethodDescriptor<*, *>>,
)
