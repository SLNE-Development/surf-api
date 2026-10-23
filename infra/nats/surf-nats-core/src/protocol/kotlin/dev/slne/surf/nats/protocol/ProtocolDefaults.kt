package dev.slne.surf.nats.protocol

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal object ProtocolDefaults {
    const val CORE_TIMEOUT_SECONDS = 5
    const val PERSISTENT_TIMEOUT_SECONDS = 60
    const val DEDUPE_WINDOW_SECONDS = 120
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

    const val MAX_ERROR_MESSAGE_LENGTH = 4096

    const val PRESENCE_HEARTBEAT_SECONDS = 5
    const val PRESENCE_TTL_SECONDS = 15

    /**
     * Returns the `Nats-TTL` of a persistent RPC request: the timeout capped at [PERSIST_MAX_AGE_SECONDS], rounded
     * up to whole seconds from milliseconds, at least 1.
     */
    fun natsTtlSeconds(timeout: Duration): Int {
        val millis = (if (timeout <= PERSIST_MAX_AGE) timeout else PERSIST_MAX_AGE).inWholeMilliseconds
        return ((millis + 999) / 1000).coerceAtLeast(1).toInt()
    }

    fun requiresOffload(bodyBytes: Int, headerBytes: Int, extraHeaderBytes: Int, maxPayload: Long): Boolean {
        return bodyBytes.toLong() + headerBytes + extraHeaderBytes + PAYLOAD_REF_RESERVE > maxPayload
    }

    /**
     * Returns the encoded size of one header line; [separatorBytes] are the bytes the client library adds around the
     * name and the value (jnats `name:value
` = 3, nats.js `
name: value` = 4).
     */
    fun headerLineBytes(name: String, value: String, separatorBytes: Int): Int {
        return name.length + value.length + separatorBytes
    }

    fun eventTooLargeMessage(typeName: String, size: Int, maxPayload: Long, subject: String): String {
        return "Event $typeName is $size bytes, exceeding the NATS max payload of $maxPayload bytes " +
                "(subject $subject). Large events are not offloaded to the object store."
    }

    fun errorMessage(message: String?): String = message.orEmpty().take(MAX_ERROR_MESSAGE_LENGTH)
}
