package dev.slne.surf.nats.runtime

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.protocol.ProtocolDefaults
import kotlin.concurrent.Volatile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Runtime values of the JVM and the JS client. Only tests change the `var`s.
 */
@InternalNatsApi
internal object RuntimeDefaults {
    const val DRAIN_TIMEOUT_SECONDS = 10

    /**
     * JetStream duplicate window of the persist streams.
     *
     * The JVM client provisions the streams with this window. Both clients bound the timeout of a persistent call
     * without a result and the publish phase of a persistent RPC by it, see
     * [dev.slne.surf.nats.runtime.rpc.PersistentRules].
     */
    @Volatile
    var dedupeWindow: Duration = ProtocolDefaults.DEDUPE_WINDOW_SECONDS.seconds
}
