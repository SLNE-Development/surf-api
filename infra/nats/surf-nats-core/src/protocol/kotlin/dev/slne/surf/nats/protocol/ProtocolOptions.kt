package dev.slne.surf.nats.protocol

/**
 * Names declared by `surf/nats/v1/options.proto`; option fields are named by their proto names.
 */
internal object ProtocolOptions {
    const val PACKAGE = "surf.nats.v1"
    const val PROTO_FILE = "surf/nats/v1/options.proto"

    const val EVENT = "event"
    const val SERVICE = "service"
    const val METHOD = "method"
    const val NULLABLE = "nullable"

    const val EVENT_OPTIONS = "NatsEventOptions"
    const val SERVICE_OPTIONS = "NatsServiceOptions"
    const val METHOD_OPTIONS = "NatsMethodOptions"

    const val SUBJECT = "subject"
    const val GLOBAL = "global"
    const val NAME = "name"
    const val CALL_TIMEOUT_SECONDS = "call_timeout_seconds"
    const val RECIPIENTS = "recipients"
    const val PERSISTENT = "persistent"

    fun qualified(name: String): String = "$PACKAGE.$name"
}
