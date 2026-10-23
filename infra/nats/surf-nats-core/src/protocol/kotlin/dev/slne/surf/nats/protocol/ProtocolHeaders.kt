package dev.slne.surf.nats.protocol

import kotlin.time.Duration

internal object ProtocolHeaders {
    const val CONTENT_TYPE = "Content-Type"
    const val ORIGIN = "Surf-Origin"
    const val SENDER_VERSION = "Surf-Sender-Version"
    const val DEADLINE = "Surf-Deadline"
    const val ERROR = "Surf-Error"
    const val PAYLOAD_REF = "Surf-Payload-Ref"
    const val REPLY_TO = "Surf-Reply-To"
    const val DEAD_SUBJECT = "Surf-Dead-Subject"
    const val NATS_MSG_ID = "Nats-Msg-Id"
    const val NATS_TTL = "Nats-TTL"
    const val NATS_EXPECTED_STREAM = "Nats-Expected-Stream"

    const val ANSWER = "Surf-Answer"
    const val ANSWER_UNKNOWN = "unknown"

    const val PRESENCE = "Surf-Presence"
    const val PRESENCE_JOIN = "join"
    const val PRESENCE_BEAT = "beat"
    const val PRESENCE_LEAVE = "leave"
    const val PRESENCE_QUERY = "query"

    const val PROTOBUF_CONTENT_TYPE = "application/protobuf"

    fun isProtobufContentType(contentType: String?): Boolean {
        return contentType != null &&
                contentType.substringBefore(';').trim().equals(PROTOBUF_CONTENT_TYPE, ignoreCase = true)
    }

    fun requireProtobufContentType(contentType: String?) {
        require(contentType != null) { "Missing $CONTENT_TYPE header, expected $PROTOBUF_CONTENT_TYPE" }
        require(isProtobufContentType(contentType)) {
            "Unsupported content type '$contentType', expected $PROTOBUF_CONTENT_TYPE"
        }
    }

    fun origin(client: String, instanceId: String): String = "$client@$instanceId"

    fun deadline(nowMillis: Long, timeout: Duration): String {
        return (nowMillis + timeout.inWholeMilliseconds).toString()
    }

    fun deadlineMillis(value: String?): Long? = value?.toLongOrNull()

    /**
     * Returns a received header value, or `null` when the header is missing or empty.
     */
    fun valueOrNull(raw: String?): String? = if (raw.isNullOrEmpty()) null else raw
}
