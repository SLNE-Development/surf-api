package dev.slne.surf.nats.js.rpc

import dev.slne.surf.nats.descriptor.NatsResultKind
import dev.slne.surf.nats.js.api.ProtoMessage
import dev.slne.surf.nats.js.codec.MessageCodec
import dev.slne.surf.nats.js.codec.NatsHeaders
import dev.slne.surf.nats.js.codec.decodeUtf8
import dev.slne.surf.nats.js.error.NatsRemoteError
import dev.slne.surf.nats.js.external.nats.core.Msg
import dev.slne.surf.nats.js.external.protobuf.DescMessage
import dev.slne.surf.nats.protocol.ProtocolHeaders
import js.typedarrays.Uint8Array

internal typealias ReplyBody = suspend (Msg) -> Uint8Array<*>

/**
 * Decodes the replies of one method; [body] resolves a reply body that may be offloaded.
 */
internal class ReplyDecoder(
    private val output: DescMessage,
    private val resultKind: NatsResultKind,
    private val body: ReplyBody,
) {
    /**
     * Returns `undefined` for UNIT and the decoded message for VALUE; throws [NatsRemoteError] for a `Surf-Error`
     * reply. ANSWER replies are collected by [AllAnswers].
     */
    suspend fun decode(reply: Msg): ProtoMessage? {
        remoteErrorOrNull(reply)?.let { throw it }

        return when (resultKind) {
            NatsResultKind.UNIT -> undefined
            NatsResultKind.VALUE -> decodeBody(reply)
            NatsResultKind.ANSWER -> error("ANSWER replies are collected by AllAnswers")
        }
    }

    suspend fun decodeBody(reply: Msg): ProtoMessage {
        val data = body(reply)
        val contentType = NatsHeaders.get(reply, ProtocolHeaders.CONTENT_TYPE)

        return MessageCodec.decode(output, data, contentType, "reply on ${reply.subject}")
    }

    fun remoteErrorOrNull(reply: Msg): NatsRemoteError? {
        val type = NatsHeaders.get(reply, ProtocolHeaders.ERROR) ?: return null

        return NatsRemoteError(type, reply.data.decodeUtf8())
    }
}
