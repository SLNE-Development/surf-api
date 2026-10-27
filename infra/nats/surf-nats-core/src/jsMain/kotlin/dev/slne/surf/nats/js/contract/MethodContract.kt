package dev.slne.surf.nats.js.contract

import dev.slne.surf.nats.descriptor.NatsResultKind
import dev.slne.surf.nats.js.external.protobuf.DescMessage
import dev.slne.surf.nats.protocol.RecipientsOption
import dev.slne.surf.nats.protocol.ResolvedMethod
import dev.slne.surf.nats.protocol.ResultKindOption
import dev.slne.surf.nats.rpc.Recipients
import dev.slne.surf.nats.runtime.rpc.CallFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class MethodContract(
    val service: ServiceContract,
    val protoName: String,
    resolved: ResolvedMethod,
    val input: DescMessage,
    val output: DescMessage,
) {
    val name: String = resolved.name
    val persistent: Boolean = resolved.persistent
    val defaultTimeout: Duration = resolved.effectiveCallTimeoutSeconds.seconds
    val label: String = "${service.typeName}.$protoName"

    val recipients: Recipients = when (resolved.recipients) {
        RecipientsOption.ALL_INSTANCES -> Recipients.ALL_INSTANCES
        else -> Recipients.ANY_INSTANCE
    }

    val resultKind: NatsResultKind = when (resolved.resultKind) {
        ResultKindOption.UNIT -> NatsResultKind.UNIT
        ResultKindOption.VALUE -> NatsResultKind.VALUE
        ResultKindOption.ANSWER -> NatsResultKind.ANSWER
    }

    val flow: CallFlow = CallFlow.of(persistent, resultKind)

    private val subjects = PerOwner(service.global, service.typeName) { owner ->
        service.names(owner).methodSubject(name, persistent)
    }

    fun subject(owner: String?): String = subjects[owner]
}
