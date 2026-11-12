package dev.slne.surf.nats.internal

import dev.slne.surf.nats.InternalNatsApi
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Carries the idempotency key of `withNatsIdempotencyKey` to the persistent calls made in its block.
 */
@InternalNatsApi
public class NatsIdempotencyKeyElement(
    public val value: String,
) : AbstractCoroutineContextElement(Key) {
    public companion object Key : CoroutineContext.Key<NatsIdempotencyKeyElement>
}
