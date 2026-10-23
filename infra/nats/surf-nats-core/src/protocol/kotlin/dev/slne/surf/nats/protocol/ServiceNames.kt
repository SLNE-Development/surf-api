package dev.slne.surf.nats.protocol

/**
 * Subjects, stream names and keys of one major version of a service.
 *
 * [client] is the owning client, or `null` for a global service.
 */
internal class ServiceNames(val client: String?, val service: String, val majorVersion: Int) {
    val rpcBase = base(ProtocolSubjects.RPC)
    val persistBase = base(ProtocolSubjects.PERSIST)
    val presenceSubject = base(ProtocolSubjects.PRESENCE)
    val dlqSubject = base(ProtocolSubjects.DLQ)
    val streamFilter = "$persistBase.*"

    val stream = (if (client == null) "SURF__${service}_V$majorVersion" else "SURF_${client}_${service}_V$majorVersion")
        .uppercase()
        .replace('-', '_')
    val dlqStream = "${stream}_DLQ"

    val responderGroup = client ?: service
    val registrationKey = (if (client == null) "global:" else "client:") + service + ":v" + majorVersion

    fun methodSubject(method: String, persistent: Boolean): String {
        return "${if (persistent) persistBase else rpcBase}.$method"
    }

    private fun base(kind: String): String {
        val version = ProtocolSubjects.versionToken(majorVersion)
        return if (client == null) "$service.$kind.$version" else "$client.$kind.$version.$service"
    }
}
