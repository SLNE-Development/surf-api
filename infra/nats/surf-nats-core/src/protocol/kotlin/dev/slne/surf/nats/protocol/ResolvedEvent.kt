package dev.slne.surf.nats.protocol

internal class ResolvedEvent(val subject: String, val global: Boolean) {

    /**
     * Returns the rule violations of the event [label] (its proto type name).
     */
    fun problems(label: String): List<String> {
        if (ProtocolNaming.isValidEventSubject(subject)) return emptyList()
        return listOf(
            "event $label has an invalid subject '$subject' (tokens must not be empty, contain '.', '*', '>' " +
                    "or whitespace, or start with '$')"
        )
    }
}
