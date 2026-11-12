package dev.slne.surf.nats.protocol

/**
 * An rpc with its options resolved against its service, see [ProtocolResolution.method].
 *
 * [callTimeoutSeconds] is `null` when the default applies, [effectiveCallTimeoutSeconds] is the timeout in effect.
 */
internal class ResolvedMethod(
    val persistent: Boolean,
    val callTimeoutSeconds: Int?,
    val recipients: RecipientsOption,
    val resultKind: ResultKindOption,
    val streaming: Boolean,
) {
    val effectiveCallTimeoutSeconds: Int
        get() = ProtocolResolution.effectiveCallTimeoutSeconds(callTimeoutSeconds, persistent)

    /**
     * Returns the rule violations of the rpc [label], or an empty list.
     *
     * A streaming rpc reports only that streaming is unsupported. A persistent rpc must not address
     * [RecipientsOption.ALL_INSTANCES], and a persistent rpc without a result must not wait longer than
     * [ProtocolDefaults.DEDUPE_WINDOW_SECONDS].
     */
    fun problems(label: String): List<String> {
        if (streaming) return listOf("rpc $label: streaming rpcs are not supported")

        val problems = ArrayList<String>(0)
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
