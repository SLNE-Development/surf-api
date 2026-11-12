package dev.slne.surf.nats.internal

import dev.slne.surf.nats.InternalNatsApi
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

/**
 * Carries the call timeout of `withNatsCallTimeout` to the service calls made in its block.
 *
 * Takes precedence over the timeout of the method descriptor.
 */
@InternalNatsApi
public class NatsTimeoutElement(
    public val timeout: Duration,
) : AbstractCoroutineContextElement(Key) {
    public companion object Key : CoroutineContext.Key<NatsTimeoutElement>
}