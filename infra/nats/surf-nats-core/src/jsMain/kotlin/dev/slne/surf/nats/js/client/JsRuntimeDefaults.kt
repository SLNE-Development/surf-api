package dev.slne.surf.nats.js.client

import dev.slne.surf.nats.runtime.RuntimeDefaults
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Runtime values of the JS client next to the shared [RuntimeDefaults]; only tests change the `var`s.
 */
internal object JsRuntimeDefaults {
    /**
     * Bound for running listener handlers in `close()`, the default drain timeout of the JVM client.
     */
    var drainTimeout: Duration = RuntimeDefaults.DRAIN_TIMEOUT_SECONDS.seconds

    /**
     * Timeout of one JetStream publish attempt; `null` keeps the nats.js default.
     */
    var jetStreamPublishTimeout: Duration? = null
}
