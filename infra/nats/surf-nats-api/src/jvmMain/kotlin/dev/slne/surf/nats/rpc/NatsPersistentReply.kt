package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi

/**
 * Holds the encoded reply of a persistent method in a form that can be stored and sent again later.
 *
 * Store [format] and [bytes] together and recreate the reply with [restore]. Replies come from
 * [NatsPersistentCall.proceed], [error] or [restore]. A reply whose kind does not fit the result of the method, such
 * as a stored reply of another method, is rejected: a fire call is moved to the dead-letter stream without another
 * delivery, and the caller of a persistent RPC receives a [NatsRemoteException]. A stored reply of another method
 * with a fitting kind is not detected, so key stored replies by method as well as by message ID.
 *
 * @property format an opaque, versioned ASCII identifier of the reply kind.
 * @property bytes the encoded response as sent to the caller. Neither the reply nor [restore] copies the array, so do
 * not modify it.
 */
public class NatsPersistentReply private constructor(
    public val format: String,
    public val bytes: ByteArray,
) {
    /**
     * Kind of reply, derived from [format].
     */
    @InternalNatsApi
    public enum class Kind(internal val format: String) {
        EMPTY("surf-nats.empty.v1"),
        VALUE("surf-nats.value.v1"),
        UNKNOWN_ANSWER("surf-nats.unknown-answer.v1"),
        ERROR("surf-nats.error.v1");

        internal companion object {
            val byFormat = entries.associateBy { it.format }
        }
    }

    @InternalNatsApi
    public val kind: Kind = Kind.byFormat.getValue(format)

    /**
     * The remote exception type of an error reply, or `null` for other replies.
     */
    @InternalNatsApi
    public val errorType: String?
        get() = if (kind == Kind.ERROR) bytes.decodeToString().substringBefore(ERROR_SEPARATOR) else null

    /**
     * The remote exception message of an error reply, or `null` for other replies.
     */
    @InternalNatsApi
    public val errorMessage: String?
        get() = if (kind == Kind.ERROR) bytes.decodeToString().substringAfter(ERROR_SEPARATOR) else null

    override fun toString(): String = "NatsPersistentReply(format=$format, size=${bytes.size})"

    public companion object {
        private const val ERROR_SEPARATOR = '\n'
        private const val MAX_ERROR_MESSAGE_LENGTH = 4096

        /**
         * Recreates a reply from a stored [format] and [bytes].
         *
         * @throws IllegalArgumentException if [format] is unknown or [bytes] do not fit it.
         */
        public fun restore(format: String, bytes: ByteArray): NatsPersistentReply {
            val kind = Kind.byFormat[format]
            requireNotNull(kind) { "Unknown persistent reply format '$format'" }

            when (kind) {
                Kind.EMPTY, Kind.UNKNOWN_ANSWER -> require(bytes.isEmpty()) {
                    "A persistent reply of format $format has no bytes, got ${bytes.size}"
                }

                Kind.ERROR -> require(ERROR_SEPARATOR in bytes.decodeToString()) {
                    "A persistent reply of format $format must contain the error type and message"
                }

                Kind.VALUE -> Unit
            }

            return NatsPersistentReply(format, bytes)
        }

        /**
         * Creates an error reply that the caller receives as a [NatsRemoteException] with the class name and message
         * of [error].
         *
         * The message is truncated to 4096 characters. Returned for a fire call, it moves the message to the
         * dead-letter stream without another delivery.
         */
        public fun error(error: Throwable): NatsPersistentReply {
            val message = error.message.orEmpty().take(MAX_ERROR_MESSAGE_LENGTH)
            val text = error.javaClass.name + ERROR_SEPARATOR + message
            return NatsPersistentReply(Kind.ERROR.format, text.encodeToByteArray())
        }

        @InternalNatsApi
        public fun empty(): NatsPersistentReply = NatsPersistentReply(Kind.EMPTY.format, ByteArray(0))

        @InternalNatsApi
        public fun value(bytes: ByteArray): NatsPersistentReply = NatsPersistentReply(Kind.VALUE.format, bytes)

        @InternalNatsApi
        public fun unknownAnswer(): NatsPersistentReply = NatsPersistentReply(Kind.UNKNOWN_ANSWER.format, ByteArray(0))
    }
}
