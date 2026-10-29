package dev.slne.surf.nats.js.rpc

import dev.slne.surf.nats.descriptor.NatsResultKind
import dev.slne.surf.nats.js.api.ProtoMessage
import dev.slne.surf.nats.js.api.ProtoMessageInit
import dev.slne.surf.nats.js.codec.NatsCodec
import dev.slne.surf.nats.js.descriptor.PerOwner
import dev.slne.surf.nats.js.external.protobuf.DescMessage
import dev.slne.surf.nats.js.external.protobuf.DescMethod
import dev.slne.surf.nats.protocol.RecipientsOption
import dev.slne.surf.nats.protocol.ResolvedMethod
import dev.slne.surf.nats.protocol.ResultKindOption
import dev.slne.surf.nats.rpc.Recipients
import dev.slne.surf.nats.runtime.rpc.CallFlow
import js.typedarrays.Uint8Array
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class MethodDescriptor(
    val desc: DescMethod,
    val service: ServiceDescriptor,
    resolved: ResolvedMethod,
) {
    val protoName: String get() = desc.name
    val name: String = resolved.name // kebab-case rpc name of the subject
    val persistent: Boolean = resolved.persistent
    val timeout: Duration = resolved.effectiveCallTimeoutSeconds.seconds
    val label: String = "${service.typeName}.$protoName"

    val input: DescMessage get() = desc.input
    val output: DescMessage get() = desc.output

    val recipients: Recipients = when (resolved.recipients) {
        RecipientsOption.ALL_INSTANCES -> Recipients.ALL_INSTANCES
        else -> Recipients.ANY_INSTANCE
    }

    val result: NatsResultKind = when (resolved.resultKind) {
        ResultKindOption.UNIT -> NatsResultKind.UNIT
        ResultKindOption.VALUE -> NatsResultKind.VALUE
        ResultKindOption.ANSWER -> NatsResultKind.ANSWER
    }

    val flow: CallFlow = CallFlow.of(persistent, result)

    private val subjects = PerOwner(service.global, service.typeName) { owner ->
        service.names(owner).methodSubject(name, persistent)
    }

    fun subject(owner: String?): String = subjects[owner] // <owner>.$rpc.v<N>.<service>.<method>

    // Work queue stream of persistent methods
    fun stream(owner: String?): String = service.names(owner).stream

    fun encodeRequest(request: ProtoMessageInit?): Uint8Array<*> = NatsCodec.encode(input, request)

    fun decodeResponse(data: Uint8Array<*>, contentType: String?, source: String): ProtoMessage {
        return NatsCodec.decode(output, data, contentType, source)
    }

    companion object {
        fun of(method: DescMethod): MethodDescriptor = ServiceDescriptor.of(method.parent).method(method)
    }
}
