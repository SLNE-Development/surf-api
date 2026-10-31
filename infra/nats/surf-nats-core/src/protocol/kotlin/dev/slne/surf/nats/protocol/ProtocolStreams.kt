package dev.slne.surf.nats.protocol

/**
 * JetStream stream and consumer names of persistent services.
 *
 * A stream name is a fixed prefix followed by the service full name with `_` replaced by `__` and then `.` by `_`.
 * The encoding preserves case and is injective because every component of a valid full name starts with a letter
 * ([ProtocolNaming.isProtoName]), so `a.b_c` and `a_b.c` map to `a_b__c` and `a__b_c`.
 */
internal object ProtocolStreams {
    const val PERSIST_PREFIX = "SURF_PERSIST_"
    const val DLQ_PREFIX = "SURF_DLQ_"

    const val MAX_NAME_LENGTH = 255

    /**
     * Name of the durable consumer that all responders of one service share on its work-queue stream.
     */
    const val DURABLE = "responders"

    fun encode(serviceName: String): String = serviceName.replace("_", "__").replace('.', '_')

    fun persist(serviceName: String): String = PERSIST_PREFIX + encode(serviceName)

    fun dlq(serviceName: String): String = DLQ_PREFIX + encode(serviceName)
}
