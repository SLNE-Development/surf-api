package dev.slne.surf.nats.protocol

/**
 * Names declared by `surf/nats/v1/options.proto`.
 *
 * [EVENT], [SERVICE] and [METHOD] are the extension fields. The remaining field constants are the proto field names of
 * the option messages.
 */
internal object ProtocolOptions {
    const val PACKAGE = "surf.nats.v1"
    const val PROTO_FILE = "surf/nats/v1/options.proto"

    const val EVENT = "event"
    const val SERVICE = "service"
    const val METHOD = "method"

    const val SERVICE_OPTIONS = "NatsServiceOptions"
    const val METHOD_OPTIONS = "NatsMethodOptions"

    const val CALL_TIMEOUT_SECONDS = "call_timeout_seconds"
    const val RECIPIENTS = "recipients"
    const val PERSISTENT = "persistent"

    /**
     * Returns [name] qualified with [PACKAGE].
     */
    fun qualified(name: String): String = "$PACKAGE.$name"
}
