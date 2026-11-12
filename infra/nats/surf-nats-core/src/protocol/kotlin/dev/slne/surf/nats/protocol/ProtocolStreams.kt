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

    /**
     * Maximum length of a stream name accepted for a service, see [ServiceNames.problem].
     */
    const val MAX_NAME_LENGTH = 255

    /**
     * Name of the durable consumer that all responders of one service share on its work-queue stream.
     */
    const val DURABLE = "responders"

    /**
     * Returns the stream name suffix of the service full name [serviceName].
     */
    fun encode(serviceName: String): String = serviceName.replace("_", "__").replace('.', '_')

    /**
     * Returns the name of the work-queue stream that holds the persistent requests of [serviceName].
     */
    fun persist(serviceName: String): String = PERSIST_PREFIX + encode(serviceName)

    /**
     * Returns the name of the dead-letter stream of [serviceName].
     */
    fun dlq(serviceName: String): String = DLQ_PREFIX + encode(serviceName)
}
