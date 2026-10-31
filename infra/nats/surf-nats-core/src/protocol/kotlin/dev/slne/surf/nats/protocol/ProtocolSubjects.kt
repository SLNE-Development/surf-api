package dev.slne.surf.nats.protocol

/**
 * Subject scheme `surf.<kind>.<proto full name>`; the full name keeps the spelling of the `.proto`.
 */
internal object ProtocolSubjects {
    const val ROOT = "surf"

    const val RPC = "rpc"
    const val PERSIST = "persist"
    const val PRESENCE = "presence"
    const val DLQ = "dlq"
    const val EVENT = "evt"

    /**
     * Returns the subject `surf.evt.<messageName>` of an event message.
     *
     * Throws [IllegalArgumentException] when [messageName] is no proto full name ([ProtocolNaming.isProtoName]).
     */
    fun event(messageName: String): String {
        ProtocolNaming.requireProtoName(messageName, "event")
        return of(EVENT, messageName)
    }

    /**
     * Returns `surf.<kind>.<name>` without validating [name].
     */
    fun of(kind: String, name: String): String = "$ROOT.$kind.$name"
}
