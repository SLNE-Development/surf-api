package dev.slne.surf.nats.js.event

import dev.slne.surf.nats.js.api.ProtoMessageInit
import dev.slne.surf.nats.js.client.NatsRuntime
import dev.slne.surf.nats.js.codec.MessageCodec
import dev.slne.surf.nats.js.codec.OutgoingMessage
import dev.slne.surf.nats.js.contract.Contracts
import dev.slne.surf.nats.js.error.NatsCallTimeoutError
import dev.slne.surf.nats.js.external.protobuf.DescMessage
import dev.slne.surf.nats.js.interop.positiveTimeoutOrNull
import dev.slne.surf.nats.protocol.ProtocolDefaults
import kotlinx.coroutines.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

internal class EventPublisher(private val runtime: NatsRuntime) {
    private val connection get() = runtime.connection

    /**
     * Publishes the event and waits for the flush that confirms it; events are never offloaded.
     */
    suspend fun publish(schema: DescMessage, event: ProtoMessageInit?, owner: String?, timeoutMs: Int?) {
        val contract = Contracts.event(schema)
        val subject = contract.subject(owner)
        val timeout = positiveTimeoutOrNull(timeoutMs, "Timeout") ?: ProtocolDefaults.CORE_TIMEOUT_SECONDS.seconds
        val message = OutgoingMessage(runtime.headers(), MessageCodec.encode(schema, event))

        val size = message.body.length + message.headers.bytes
        val maxPayload = runtime.payloads.maxPayload
        require(size <= maxPayload) {
            ProtocolDefaults.eventTooLargeMessage(contract.typeName, size, maxPayload, subject)
        }

        connection.publish(subject, message.body, message.publishOptions())
        withTimeoutOrNull(timeout) {
            connection.flush().await()
        } ?: throw NatsCallTimeoutError(subject, timeout.inWholeMilliseconds.toInt())
    }
}
