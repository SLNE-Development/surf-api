package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.bytecode.ValueClassParameter
import dev.slne.surf.nats.codec.NatsCodec
import dev.slne.surf.nats.descriptor.NatsMethodDescriptor
import dev.slne.surf.nats.descriptor.NatsResultKind
import dev.slne.surf.nats.runtime.rpc.CallFlow
import kotlinx.serialization.KSerializer
import java.lang.reflect.Method
import kotlin.time.Duration

@InternalNatsApi
internal class MethodDescriptor(
    val generated: NatsMethodDescriptor<Any, *>,
    val method: Method, // interface method, JVM name mangled for value class parameters
    val index: Int,
    val subject: String, // <prefix>.$rpc.v<N>.<service>.<method>
    val timeout: Duration,
    val valueClasses: List<ValueClassParameter?>,
    val label: String, // <interface>.<kotlinName>
) {
    val kotlinName: String = generated.kotlinName
    val recipients: Recipients = generated.recipients
    val persistent: Boolean = generated.persistent
    val result: NatsResultKind = generated.resultKind
    val flow: CallFlow = CallFlow.of(persistent, result)

    val request: KSerializer<Any> = generated.requestSerializer

    @Suppress("UNCHECKED_CAST")
    val response: KSerializer<Any>? = generated.responseSerializer as KSerializer<Any>? // null exactly for UNIT

    fun encodeRequest(arguments: Array<Any?>): ByteArray {
        return NatsCodec.encode(request, generated.toRequest(arguments))
    }

    fun decodeArguments(data: ByteArray, contentType: String?): Array<Any?> {
        return generated.toArguments(NatsCodec.decode(request, data, contentType))
    }

    // Only for results other than UNIT; ServiceDescriptor.of checks that they have a response serializer
    fun encodeResponse(value: Any): ByteArray = NatsCodec.encode(response!!, value)

    fun decodeResponse(data: ByteArray, contentType: String?): Any = NatsCodec.decode(response!!, data, contentType)
}
