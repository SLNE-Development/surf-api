package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.WireFormat
import dev.slne.surf.nats.bytecode.ValueClassParameter
import dev.slne.surf.nats.codec.WireSerializers
import java.lang.reflect.Method
import kotlin.time.Duration

@InternalNatsApi
internal class MethodDescriptor(
    val method: Method,
    val index: Int,
    val subject: String, // <prefix>.$rpc.<service>.<method>
    val recipients: Recipients,
    val timeout: Duration,
    val arguments: WireSerializers<Array<Any?>>,
    val result: ResultKind,
    val persistent: Boolean,
    val format: WireFormat,
    val valueClasses: List<ValueClassParameter?>,
)