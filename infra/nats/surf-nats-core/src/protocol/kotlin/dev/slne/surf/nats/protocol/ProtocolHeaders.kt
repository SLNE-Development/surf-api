package dev.slne.surf.nats.protocol

import kotlin.time.Duration

/**
 * Header names and values of the surf-nats wire protocol, with their validation and parsing rules.
 *
 * Both clients look up header names case-sensitively, so an external producer must write them exactly as declared
 * here. A message with `content-type` instead of `Content-Type` fails the content type check.
 */
internal object ProtocolHeaders {
    /**
     * Content type of the body, sent on every message. Receivers accept only [PROTOBUF_CONTENT_TYPE].
     */
    const val CONTENT_TYPE = "Content-Type"

    /**
     * Sender identity in the form `<client name>@<instance id>`, see [origin].
     */
    const val ORIGIN = "Surf-Origin"

    /**
     * surf-nats version of the sender.
     */
    const val SENDER_VERSION = "Surf-Sender-Version"

    /**
     * Absolute deadline of a request in epoch milliseconds of the caller's clock, see [deadline].
     *
     * Responders compare it with their own clock, so caller and responder hosts need synchronized clocks.
     */
    const val DEADLINE = "Surf-Deadline"

    /**
     * Type name of the exception that failed a call.
     *
     * On a reply the body holds the error message, see [ProtocolDefaults.errorMessage]. On a dead-lettered
     * message it names the exception of the last failed delivery.
     */
    const val ERROR = "Surf-Error"

    /**
     * Object name of an offloaded body in its [PayloadBucket]. A message with this header has an empty body.
     */
    const val PAYLOAD_REF = "Surf-Payload-Ref"

    /**
     * Subject that receives the reply of a persistent RPC.
     *
     * The caller writes its inbox here. The responder publishes the reply only for a delivery it acknowledges, and
     * publishes it before the ack. A delivery that ends in a nak, a term, a dead letter or a timeout gets no reply.
     * A request without this header gets no reply.
     */
    const val REPLY_TO = "Surf-Reply-To"

    /**
     * Original subject of a dead-lettered message.
     */
    const val DEAD_SUBJECT = "Surf-Dead-Subject"

    /**
     * ID of a published event, validated by [eventIdProblem].
     */
    const val EVENT_ID = "Surf-Event-Id"

    /**
     * Identity of a persistent message that takes precedence over its `Nats-Msg-Id`: the caller's idempotency key, or
     * the original identity carried over by a dead-letter replay.
     */
    const val MESSAGE_ID = "Surf-Message-Id"
    const val NATS_MSG_ID = "Nats-Msg-Id"
    const val NATS_TTL = "Nats-TTL"
    const val NATS_EXPECTED_STREAM = "Nats-Expected-Stream"

    /**
     * Marks a reply of a [ResultKindOption.ANSWER] rpc that carries no value. The only value is [ANSWER_UNKNOWN].
     */
    const val ANSWER = "Surf-Answer"
    const val ANSWER_UNKNOWN = "unknown"

    /**
     * Kind of a presence message on the presence subject of a service: [PRESENCE_JOIN], [PRESENCE_BEAT] and
     * [PRESENCE_LEAVE] from responders, [PRESENCE_QUERY] from callers that ask responders to announce themselves.
     */
    const val PRESENCE = "Surf-Presence"
    const val PRESENCE_JOIN = "join"
    const val PRESENCE_BEAT = "beat"
    const val PRESENCE_LEAVE = "leave"
    const val PRESENCE_QUERY = "query"

    const val PROTOBUF_CONTENT_TYPE = "application/protobuf"

    const val MAX_EVENT_ID_LENGTH = 128
    const val MAX_IDEMPOTENCY_KEY_LENGTH = 128

    /**
     * Returns whether [contentType] is [PROTOBUF_CONTENT_TYPE], ignoring case and any `;` parameters.
     */
    fun isProtobufContentType(contentType: String?): Boolean {
        return contentType != null &&
                contentType.substringBefore(';').trim().equals(PROTOBUF_CONTENT_TYPE, ignoreCase = true)
    }

    /**
     * Throws [IllegalArgumentException] when [contentType] is missing or not [isProtobufContentType].
     */
    fun requireProtobufContentType(contentType: String?) {
        require(contentType != null) { "Missing $CONTENT_TYPE header, expected $PROTOBUF_CONTENT_TYPE" }
        require(isProtobufContentType(contentType)) {
            "Unsupported content type '$contentType', expected $PROTOBUF_CONTENT_TYPE"
        }
    }

    /**
     * Returns the [ORIGIN] value of the instance [instanceId] of the client [client].
     */
    fun origin(client: String, instanceId: String): String = "$client@$instanceId"

    /**
     * Returns why [eventId] cannot be a `Surf-Event-Id`, or `null`: it must have 1 to [MAX_EVENT_ID_LENGTH] visible
     * ASCII characters.
     */
    fun eventIdProblem(eventId: String): String? = identifierProblem("Event ID", eventId, MAX_EVENT_ID_LENGTH)

    /**
     * Returns why [key] cannot be the idempotency key of a persistent call, sent as [MESSAGE_ID], or `null`: it must
     * have 1 to [MAX_IDEMPOTENCY_KEY_LENGTH] visible ASCII characters.
     */
    fun idempotencyKeyProblem(key: String): String? =
        identifierProblem("Idempotency key", key, MAX_IDEMPOTENCY_KEY_LENGTH)

    private fun identifierProblem(what: String, value: String, maxLength: Int): String? {
        if (value.isEmpty() || value.length > maxLength) {
            return "$what must have 1 to $maxLength characters, got ${value.length}"
        }
        if (value.any { it !in '!'..'~' }) {
            return "$what '$value' must consist of visible ASCII characters"
        }
        return null
    }

    /**
     * Returns the [DEADLINE] value [nowMillis] plus [timeout] as a decimal string.
     */
    fun deadline(nowMillis: Long, timeout: Duration): String {
        return (nowMillis + timeout.inWholeMilliseconds).toString()
    }

    /**
     * Parses a [DEADLINE] value, or returns `null` when it is missing or not a number.
     */
    fun deadlineMillis(value: String?): Long? = value?.toLongOrNull()

    /**
     * Returns a received header value, or `null` when the header is missing or empty.
     */
    fun valueOrNull(raw: String?): String? = if (raw.isNullOrEmpty()) null else raw
}
