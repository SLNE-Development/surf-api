package dev.slne.surf.nats.protocol

/**
 * Proto type names that the codecs and the code generator treat specially.
 */
internal object ProtocolFieldRules {
    /**
     * Package prefix of the protobuf well-known types.
     */
    const val WELL_KNOWN_PREFIX = "google.protobuf."

    /**
     * Request or response type of an rpc without an argument or a result.
     */
    const val EMPTY_TYPE = "google.protobuf.Empty"

    /**
     * Message type that carries a UUID as exactly [UUID_BYTES] bytes.
     */
    const val UUID_TYPE = "surf.types.v1.Uuid"
    const val UUID_BYTES = 16
}
