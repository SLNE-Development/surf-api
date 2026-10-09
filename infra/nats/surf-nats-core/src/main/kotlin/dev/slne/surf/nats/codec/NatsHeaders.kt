package dev.slne.surf.nats.codec

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsConstants
import dev.slne.surf.nats.WireFormat
import dev.slne.surf.nats.connection.NatsConnectionManager
import io.nats.client.Message
import io.nats.client.impl.Headers

@InternalNatsApi
internal object NatsHeaders {

    const val CONTENT_TYPE = "Content-Type"
    const val ORIGIN = "Surf-Origin"
    const val SENDER_VERSION = "Surf-Sender-Version"
    const val DEADLINE = "Surf-Deadline"
    const val ERROR = "Surf-Error"
    const val PAYLOAD_REF = "Surf-Payload-Ref"
    const val REPLY_TO = "Surf-Reply-To"
    const val DEAD_SUBJECT = "Surf-Dead-Subject"

    const val ANSWER = "Surf-Answer"
    const val ANSWER_NONE = "none"

    const val PRESENCE = "Surf-Presence"
    const val PRESENCE_JOIN = "join"
    const val PRESENCE_BEAT = "beat"
    const val PRESENCE_LEAVE = "leave"
    const val PRESENCE_QUERY = "query"

    fun base(clientName: String, format: WireFormat): Headers {
        val headers = Headers()

        headers.add(CONTENT_TYPE, format.contentType)
        headers.add(SENDER_VERSION, NatsConstants.VERSION)
        headers.add(ORIGIN, origin(clientName))

        return headers
    }

    fun origin(clientName: String) = "$clientName@${NatsConnectionManager.instanceIdString}"
}

internal fun Message.presenceKind(): String? {
    return if (hasHeaders()) headers.getFirst(NatsHeaders.PRESENCE) else null
}