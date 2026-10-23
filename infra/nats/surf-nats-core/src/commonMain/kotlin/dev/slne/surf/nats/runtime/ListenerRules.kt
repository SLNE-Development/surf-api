package dev.slne.surf.nats.runtime

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.rpc.Recipients

@InternalNatsApi
internal object ListenerRules {
    val DEFAULT_RECIPIENTS: Recipients = Recipients.ALL_INSTANCES

    /**
     * Returns why a listener with these options is invalid, or `null`.
     *
     * A queue group delivers each event to one member; skipping own events there would lose them for the others.
     */
    fun problem(recipients: Recipients, ignoreFromThisClient: Boolean): String? {
        return if (ignoreFromThisClient && recipients != Recipients.ALL_INSTANCES) {
            "ignoreFromThisClient requires recipients = ALL_INSTANCES"
        } else {
            null
        }
    }

    /**
     * Returns the queue group of a listener of the client [clientName], or `null` for one without a group.
     */
    fun queueGroup(recipients: Recipients, clientName: String): String? {
        return if (recipients == Recipients.ANY_INSTANCE) clientName else null
    }
}
