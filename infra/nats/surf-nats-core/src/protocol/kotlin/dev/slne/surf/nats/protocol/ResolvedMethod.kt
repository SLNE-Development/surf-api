package dev.slne.surf.nats.protocol

/**
 * An rpc with its options resolved against its service; [callTimeoutSeconds] is `null` when the default applies.
 */
internal class ResolvedMethod(
    val name: String,
    val persistent: Boolean,
    val callTimeoutSeconds: Int?,
    val recipients: RecipientsOption,
    val resultKind: ResultKindOption,
    val streaming: Boolean,
) {
    val effectiveCallTimeoutSeconds: Int
        get() = ProtocolResolution.effectiveCallTimeoutSeconds(callTimeoutSeconds, persistent)

    /**
     * Returns the rule violations of the rpc [label]; a streaming rpc reports only that it is streaming.
     */
    fun problems(label: String): List<String> {
        if (streaming) return listOf("rpc $label: streaming rpcs are not supported")

        val problems = ArrayList<String>(0)
        if (!ProtocolNaming.isValidNameToken(name)) {
            problems += "rpc $label resolves to the method name '$name', which is not a single subject token " +
                    "(no '.', '*', '>', whitespace, no leading '$')"
        }
        if (persistent && recipients == RecipientsOption.ALL_INSTANCES) {
            problems += "rpc $label: persistent methods cannot address ALL_INSTANCES"
        }
        val timeout = effectiveCallTimeoutSeconds
        if (persistent && resultKind == ResultKindOption.UNIT && timeout > ProtocolDefaults.DEDUPE_WINDOW_SECONDS) {
            problems += "rpc $label: the call timeout of a persistent fire method (${timeout}s) must not exceed " +
                    "the ${ProtocolDefaults.DEDUPE_WINDOW_SECONDS}s duplicate window"
        }
        return problems
    }
}
