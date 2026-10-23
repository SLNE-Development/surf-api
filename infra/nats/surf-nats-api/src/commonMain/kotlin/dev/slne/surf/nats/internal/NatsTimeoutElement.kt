package dev.slne.surf.nats.internal

import dev.slne.surf.nats.InternalNatsApi
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

@InternalNatsApi
public class NatsTimeoutElement(
    public val timeout: Duration,
) : AbstractCoroutineContextElement(Key) {
    public companion object Key : CoroutineContext.Key<NatsTimeoutElement>
}