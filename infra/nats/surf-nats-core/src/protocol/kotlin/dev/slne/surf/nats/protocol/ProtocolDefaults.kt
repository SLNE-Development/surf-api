package dev.slne.surf.nats.protocol

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Wire-level defaults and limits shared by the JVM client, the JS client and the code generator.
 *
 * Changing a value changes the protocol for every client that talks to the same services.
 */
internal object ProtocolDefaults {
    /**
     * Call timeout of a non-persistent rpc without `call_timeout_seconds`.
     */
    const val CORE_TIMEOUT_SECONDS = 5

    /**
     * Call timeout of a persistent rpc without `call_timeout_seconds`.
     */
    const val PERSISTENT_TIMEOUT_SECONDS = 60

    /**
     * Default JetStream duplicate window of the persist streams.
     *
     * Republished attempts of one persistent request are deduplicated only within this window, so it bounds the
     * timeout of a persistent rpc without a result and the publish phase of one with a result.
     */
    const val DEDUPE_WINDOW_SECONDS = 120

    /**
     * Upper bound of `Nats-TTL` and TTL of the [PERSISTENT_PAYLOAD_BUCKET] (7 days).
     */
    const val PERSIST_MAX_AGE_SECONDS = 7 * 24 * 60 * 60

    const val TRANSIENT_PAYLOAD_BUCKET = "surf-nats-payloads"
    const val TRANSIENT_PAYLOAD_TTL_SECONDS = 10 * 60
    const val PERSISTENT_PAYLOAD_BUCKET = "surf-nats-persistent-payloads"
    const val PERSISTENT_PAYLOAD_TTL_SECONDS = PERSIST_MAX_AGE_SECONDS

    val PERSIST_MAX_AGE: Duration = PERSIST_MAX_AGE_SECONDS.seconds

    /**
     * Bytes kept free for the `Surf-Payload-Ref` header when deciding whether a body is offloaded.
     */
    const val PAYLOAD_REF_RESERVE = 128

    /**
     * Maximum number of characters of the error message sent in the body of a `Surf-Error` reply.
     */
    const val MAX_ERROR_MESSAGE_LENGTH = 4096

    /**
     * Interval between the `beat` presence announcements of a responder.
     */
    const val PRESENCE_HEARTBEAT_SECONDS = 5

    /**
     * Time after the last presence announcement until a caller considers a responder gone.
     */
    const val PRESENCE_TTL_SECONDS = 15

    /**
     * Returns the `Nats-TTL` of a persistent RPC request in seconds.
     *
     * The value is [timeout] capped at [PERSIST_MAX_AGE], rounded up to whole seconds and at least 1.
     */
    fun natsTtlSeconds(timeout: Duration): Int {
        val millis = (if (timeout <= PERSIST_MAX_AGE) timeout else PERSIST_MAX_AGE).inWholeMilliseconds
        return ((millis + 999) / 1000).coerceAtLeast(1).toInt()
    }

    /**
     * Returns whether a body must be offloaded to the object store.
     *
     * [headerBytes] and [extraHeaderBytes] are the encoded header sizes, see [headerLineBytes]. The check also reserves
     * [PAYLOAD_REF_RESERVE] bytes for the `Surf-Payload-Ref` header that an offloaded message carries.
     */
    fun requiresOffload(bodyBytes: Int, headerBytes: Int, extraHeaderBytes: Int, maxPayload: Long): Boolean {
        return bodyBytes.toLong() + headerBytes + extraHeaderBytes + PAYLOAD_REF_RESERVE > maxPayload
    }

    /**
     * Returns the encoded size of one header line.
     *
     * [separatorBytes] are the bytes the client library writes besides the name and the value: 3 for jnats
     * (`name:value\r\n`) and 4 for nats.js (`\r\nname: value`). The size counts characters and matches the byte
     * count only for ASCII names and values.
     */
    fun headerLineBytes(name: String, value: String, separatorBytes: Int): Int {
        return name.length + value.length + separatorBytes
    }

    /**
     * Returns the error message for an event that exceeds [maxPayload]. Events are never offloaded.
     */
    fun eventTooLargeMessage(typeName: String, size: Int, maxPayload: Long, subject: String): String {
        return "Event $typeName is $size bytes, exceeding the NATS max payload of $maxPayload bytes " +
                "(subject $subject). Large events are not offloaded to the object store."
    }

    /**
     * Returns [message] truncated to [MAX_ERROR_MESSAGE_LENGTH] characters, or an empty string for `null`.
     */
    fun errorMessage(message: String?): String = message.orEmpty().take(MAX_ERROR_MESSAGE_LENGTH)
}
