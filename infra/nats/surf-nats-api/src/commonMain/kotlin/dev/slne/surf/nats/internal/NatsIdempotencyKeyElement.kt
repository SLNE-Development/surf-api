package dev.slne.surf.nats.internal

import dev.slne.surf.nats.InternalNatsApi
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

@InternalNatsApi
public class NatsIdempotencyKeyElement(
    public val value: String,
) : AbstractCoroutineContextElement(Key) {
    public companion object Key : CoroutineContext.Key<NatsIdempotencyKeyElement>
}
