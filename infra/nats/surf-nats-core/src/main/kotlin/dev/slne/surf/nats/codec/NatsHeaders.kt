package dev.slne.surf.nats.codec

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsConstants
import dev.slne.surf.nats.WireFormat
import dev.slne.surf.nats.connection.NatsConnectionManager
import io.nats.client.Message
import io.nats.client.impl.Headers
import kotlin.time.Duration

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
    const val ANSWER_UNKNOWN = "unknown"

    const val PRESENCE = "Surf-Presence"
    const val PRESENCE_JOIN = "join"
    const val PRESENCE_BEAT = "beat"
    const val PRESENCE_LEAVE = "leave"
    const val PRESENCE_QUERY = "query"

    val EMPTY_BODY = ByteArray(0)

    fun base(clientName: String, format: WireFormat): Headers {
        val headers = Headers()

        headers.add(CONTENT_TYPE, format.contentType)
        headers.add(SENDER_VERSION, NatsConstants.VERSION)
        headers.add(ORIGIN, origin(clientName))

        return headers
    }

    fun presence(clientName: String, kind: String): Headers {
        return base(clientName, WireFormat.CBOR).add(PRESENCE, kind)
    }

    fun origin(clientName: String) = "$clientName@${NatsConnectionManager.instanceIdString}"

    fun deadline(timeout: Duration): String {
        return (System.currentTimeMillis() + timeout.inWholeMilliseconds).toString()
    }
}

internal fun Message.header(name: String): String? = headers?.getFirst(name)

internal fun Message.presenceKind(): String? = header(NatsHeaders.PRESENCE)

internal fun Message.deadlineMillis(): Long? = header(NatsHeaders.DEADLINE)?.toLongOrNull()
