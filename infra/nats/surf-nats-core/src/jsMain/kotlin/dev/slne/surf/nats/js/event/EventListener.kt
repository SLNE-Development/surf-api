package dev.slne.surf.nats.js.event

import dev.slne.surf.nats.js.api.EventContext
import dev.slne.surf.nats.js.api.ProtoMessage
import dev.slne.surf.nats.js.codec.MessageCodec
import dev.slne.surf.nats.js.codec.NatsHeaders
import dev.slne.surf.nats.js.external.nats.core.Msg
import dev.slne.surf.nats.js.external.protobuf.DescMessage
import dev.slne.surf.nats.protocol.ProtocolHeaders

internal typealias EventHandler = (ProtoMessage, EventContext) -> Any?

/**
 * Delivers the messages of one subscription to its handler.
 */
internal class EventListener(
    private val schema: DescMessage,
    private val subject: String,
    private val handler: EventHandler,
    private val ignoreFromThisClient: Boolean,
    private val clientOrigin: String,
    private val runningHandlers: RunningHandlers,
) {

    fun onMessage(error: Throwable?, message: Msg) {
        if (error != null) {
            console.warn("surf-nats: listener for ${schema.typeName} on $subject stopped", error)
        } else {
            deliver(message)
        }
    }

    private fun deliver(message: Msg) {
        val origin = NatsHeaders.get(message, ProtocolHeaders.ORIGIN)
        val fromThisClient = origin == clientOrigin
        if (fromThisClient && ignoreFromThisClient) return

        val event = decodeOrNull(message) ?: return
        val result = try {
            handler(event, EventContext(origin = origin, isFromThisClient = fromThisClient))
        } catch (e: Throwable) {
            logFailure(e)
            return
        }

        runningHandlers.track(result, ::logFailure)
    }

    private fun decodeOrNull(message: Msg): ProtoMessage? {
        return try {
            val contentType = NatsHeaders.get(message, ProtocolHeaders.CONTENT_TYPE)
            MessageCodec.decode(schema, message.data, contentType, "event on ${message.subject}")
        } catch (e: Throwable) {
            console.warn(
                "surf-nats: dropping undecodable ${schema.typeName} on ${message.subject}",
                e
            )
            null
        }
    }

    private fun logFailure(error: Any?) {
        console.error("surf-nats: listener for ${schema.typeName} failed on $subject", error)
    }
}
