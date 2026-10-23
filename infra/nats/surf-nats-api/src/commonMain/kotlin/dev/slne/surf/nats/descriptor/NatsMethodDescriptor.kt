package dev.slne.surf.nats.descriptor

import dev.slne.surf.nats.NatsGeneratedApi
import dev.slne.surf.nats.rpc.Recipients
import kotlinx.serialization.KSerializer

public class NatsMethodDescriptor<Req : Any, Res> @NatsGeneratedApi constructor(
    public val protoName: String,
    public val name: String,
    public val kotlinName: String,
    public val persistent: Boolean,
    public val callTimeoutSeconds: Int?,
    public val recipients: Recipients,
    public val resultKind: NatsResultKind,
    public val parameters: List<NatsParameter>,
    public val requestSerializer: KSerializer<Req>,
    public val responseSerializer: KSerializer<*>?,
    public val toRequest: (Array<Any?>) -> Req,
    public val toArguments: (Req) -> Array<Any?>,
)
