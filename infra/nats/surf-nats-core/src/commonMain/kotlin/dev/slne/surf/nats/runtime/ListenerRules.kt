package dev.slne.surf.nats.runtime

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.rpc.Recipients

/**
 * Recipient rules of event listeners, shared by the JVM and the JS client.
 */
@InternalNatsApi
internal object ListenerRules {
    /**
     * Recipients of a listener that sets none: every instance receives every event.
     */
    val DEFAULT_RECIPIENTS: Recipients = Recipients.ALL_INSTANCES

    /**
     * Returns why a listener with these options is invalid, or `null`.
     *
     * A queue group delivers each event to one member. Skipping own events there would lose them for the others.
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
     *
     * A [Recipients.ANY_INSTANCE] listener joins the queue group [clientName], so each event reaches one instance of
     * the same client, not one instance across all clients.
     */
    fun queueGroup(recipients: Recipients, clientName: String): String? {
        return if (recipients == Recipients.ANY_INSTANCE) clientName else null
    }
}
