package dev.slne.surf.nats.js.event

import dev.slne.surf.nats.js.api.ListenOptions
import dev.slne.surf.nats.rpc.Recipients
import dev.slne.surf.nats.runtime.ListenerRules

internal class ListenerSettings(
    val recipients: Recipients,
    val ignoreFromThisClient: Boolean,
) {
    val problem: String? = ListenerRules.problem(recipients, ignoreFromThisClient)

    fun queueGroup(clientName: String): String? = ListenerRules.queueGroup(recipients, clientName)

    companion object {
        fun of(options: ListenOptions?) = ListenerSettings(
            recipients = parseRecipients(options?.recipients),
            ignoreFromThisClient = options?.ignoreFromThisClient == true,
        )

        private fun parseRecipients(value: String?): Recipients {
            if (value == null) return ListenerRules.DEFAULT_RECIPIENTS
            val found = Recipients.entries.find { it.name == value }

            require(found != null) {
                "Unknown recipients '$value': use ${Recipients.entries.joinToString(" or ")}"
            }

            return found
        }
    }
}
