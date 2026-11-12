package dev.slne.surf.nats.descriptor

import dev.slne.surf.nats.NatsGeneratedApi

/**
 * Describes a generated service interface.
 *
 * Only generated code creates instances. Every generated service interface exposes its descriptor as the static
 * field `NATS_DESCRIPTOR`.
 *
 * @property protoName the proto full name of the service, such as `surf.demo.v1.StatusService`. Its subjects, streams
 * and queue group derive from it, so renaming the service or its package in the `.proto` breaks the protocol.
 * @property methods the rpcs of the service, one for each abstract method of the interface.
 */
public class NatsServiceDescriptor<@Suppress("unused") S : Any> @NatsGeneratedApi constructor(
    public val protoName: String,
    public val methods: List<NatsMethodDescriptor<*, *>>,
)
