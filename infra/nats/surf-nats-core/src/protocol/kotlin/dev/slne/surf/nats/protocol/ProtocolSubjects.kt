package dev.slne.surf.nats.protocol

internal object ProtocolSubjects {
    const val RPC = $$"$rpc"
    const val PERSIST = $$"$persist"
    const val PRESENCE = $$"$presence"
    const val DLQ = $$"$dlq"

    fun versionToken(majorVersion: Int): String {
        require(majorVersion >= 1) { "Major version must be at least 1, was $majorVersion" }
        return "v$majorVersion"
    }

    /**
     * Builds the versioned subject of an event.
     *
     * [client] is the owning client, or `null` for a global event. The version token follows the first token of
     * the full subject and is appended to a single-token subject.
     */
    fun event(client: String?, subject: String, majorVersion: Int): String {
        val full = if (client == null) subject else "$client.$subject"
        val version = versionToken(majorVersion)
        val dot = full.indexOf('.')
        return if (dot < 0) "$full.$version" else "${full.substring(0, dot)}.$version${full.substring(dot)}"
    }
}
