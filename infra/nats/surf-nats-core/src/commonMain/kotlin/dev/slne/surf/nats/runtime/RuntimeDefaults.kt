package dev.slne.surf.nats.runtime

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.protocol.ProtocolDefaults
import kotlin.concurrent.Volatile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Runtime values of the JVM and the JS client; only tests change the `var`s.
 */
@InternalNatsApi
internal object RuntimeDefaults {
    const val DRAIN_TIMEOUT_SECONDS = 10

    /**
     * Upper bound of persistent fire timeouts and of the publish phase of persistent RPCs; must not exceed the duplicate
     * window of the streams.
     */
    @Volatile
    var dedupeWindow: Duration = ProtocolDefaults.DEDUPE_WINDOW_SECONDS.seconds
}
