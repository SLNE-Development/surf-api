package dev.slne.surf.nats.runtime.rpc

import dev.slne.surf.nats.InternalNatsApi
import kotlin.time.Duration

/**
 * Call-time bounds of persistent calls; [window] is the duplicate window of the streams.
 */
@InternalNatsApi
internal object PersistentRules {

    /**
     * Requires the [timeout] of a fire to fit into [window]: republished attempts are deduplicated only within it.
     */
    inline fun requireFireTimeout(timeout: Duration, window: Duration, label: () -> String) {
        require(timeout <= window) {
            "Timeout $timeout for persistent ${label()} without a result exceeds the JetStream duplicate window of $window"
        }
    }

    /**
     * Returns how long a persistent RPC republishes its request: [timeout], at most [window].
     */
    fun publishPhase(timeout: Duration, window: Duration): Duration = if (timeout <= window) timeout else window
}
