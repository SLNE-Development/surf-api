package dev.slne.surf.nats.protocol

/**
 * Subjects, stream names and consumer names of a service, derived from its proto full name [service].
 *
 * The full name includes the versioned package, so the major versions of a service never share subjects or streams.
 * The constructor throws [IllegalArgumentException] when [problem] reports a violation.
 */
internal class ServiceNames(val service: String) {

    init {
        problem(service)?.let { throw IllegalArgumentException(it) }
    }

    val rpcBase = ProtocolSubjects.of(ProtocolSubjects.RPC, service)
    val persistBase = ProtocolSubjects.of(ProtocolSubjects.PERSIST, service)
    val presenceSubject = ProtocolSubjects.of(ProtocolSubjects.PRESENCE, service)
    val dlqSubject = ProtocolSubjects.of(ProtocolSubjects.DLQ, service)
    val streamFilter = "$persistBase.*"

    val stream = ProtocolStreams.persist(service)
    val dlqStream = ProtocolStreams.dlq(service)

    /**
     * Core queue group of the `ANY_INSTANCE` responders, shared by every client that serves the service.
     */
    val queueGroup: String get() = service

    val durable: String get() = ProtocolStreams.DURABLE

    /**
     * Returns the subject of the rpc [method], named exactly as in the `.proto`.
     *
     * Throws [IllegalArgumentException] when [methodProblem] reports a violation.
     */
    fun methodSubject(method: String, persistent: Boolean): String {
        methodProblem(service, method)?.let { throw IllegalArgumentException(it) }
        return "${if (persistent) persistBase else rpcBase}.$method"
    }

    companion object {
        /**
         * Returns why [service] cannot name a service, or `null`.
         *
         * The name must be a proto full name ([ProtocolNaming.isProtoName]) whose stream names stay within
         * [ProtocolStreams.MAX_NAME_LENGTH] characters.
         */
        fun problem(service: String): String? {
            ProtocolNaming.nameProblem("service", service)?.let { return it }
            val stream = ProtocolStreams.persist(service)
            if (stream.length > ProtocolStreams.MAX_NAME_LENGTH) {
                return "service $service: its stream name $stream has ${stream.length} characters, more than " +
                        "${ProtocolStreams.MAX_NAME_LENGTH}; shorten the package or the service name"
            }
            return null
        }

        /**
         * Returns why [method] cannot name an rpc of [service], or `null`.
         *
         * The name must be a single proto identifier ([ProtocolNaming.isProtoIdentifier]), so it stays one subject
         * token and the stream filter `surf.persist.<service>.*` matches its persistent subject.
         */
        fun methodProblem(service: String, method: String): String? {
            if (ProtocolNaming.isProtoIdentifier(method)) return null
            return ProtocolNaming.nameProblem("rpc", "$service.$method", method)
        }
    }
}
