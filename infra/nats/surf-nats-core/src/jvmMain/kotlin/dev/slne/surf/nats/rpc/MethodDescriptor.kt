package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.codec.NatsCodec
import dev.slne.surf.nats.descriptor.NatsMethodDescriptor
import dev.slne.surf.nats.descriptor.NatsResultKind
import dev.slne.surf.nats.runtime.rpc.CallFlow
import kotlinx.serialization.KSerializer
import java.lang.reflect.Method
import kotlin.time.Duration

/**
 * Runtime model of one rpc of a service: the generated descriptor, the resolved interface method and the wire subject.
 *
 * [timeout] is the effective call timeout of the method, used when the caller sets none. [flow] follows from
 * [persistent] and [result].
 */
@InternalNatsApi
internal class MethodDescriptor(
    val generated: NatsMethodDescriptor<Any, *>,
    val method: Method, // interface method: (request, Continuation), or (Continuation) for an Empty request
    val index: Int,
    val subject: String, // surf.rpc|persist.<service>.<rpc>
    val timeout: Duration,
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

    // Unit for an Empty request
    fun encodeRequest(value: Any): ByteArray = NatsCodec.encode(request, value)

    fun decodeRequest(data: ByteArray, contentType: String?): Any = NatsCodec.decode(request, data, contentType)

    // Only for results other than UNIT. ServiceDescriptor.of checks that they have a response serializer
    fun encodeResponse(value: Any): ByteArray = NatsCodec.encode(response!!, value)

    fun decodeResponse(data: ByteArray, contentType: String?): Any = NatsCodec.decode(response!!, data, contentType)

    /**
     * Encodes a handler's return [value] in the reply format of [result].
     */
    fun encodeReply(value: Any?): NatsPersistentReply = when (result) {
        NatsResultKind.UNIT -> NatsPersistentReply.empty()
        NatsResultKind.VALUE -> NatsPersistentReply.value(encodeResponse(value as Any))
        NatsResultKind.ANSWER -> when (val answer = value as Answer<*>) {
            is Answer.Known -> NatsPersistentReply.value(encodeResponse(answer.value as Any))
            Answer.Unknown -> NatsPersistentReply.unknownAnswer()
        }
    }
}
