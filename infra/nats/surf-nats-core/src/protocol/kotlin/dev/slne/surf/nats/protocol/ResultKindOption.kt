package dev.slne.surf.nats.protocol

/**
 * Result of an rpc: `UNIT` for a `google.protobuf.Empty` response, `ANSWER` for `ALL_INSTANCES` methods with a
 * response, `VALUE` otherwise.
 */
internal enum class ResultKindOption {
    UNIT,
    VALUE,
    ANSWER;

    companion object {
        fun of(emptyResponse: Boolean, recipients: RecipientsOption): ResultKindOption = when {
            emptyResponse -> UNIT
            recipients == RecipientsOption.ALL_INSTANCES -> ANSWER
            else -> VALUE
        }
    }
}
