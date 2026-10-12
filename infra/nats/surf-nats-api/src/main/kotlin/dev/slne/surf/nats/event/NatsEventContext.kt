package dev.slne.surf.nats.event

import dev.slne.surf.nats.InternalNatsApi
import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

public class NatsEventContext @InternalNatsApi constructor(

    /**
     * <client>@<instanceId> of the publisher, if known.
     */
    public val origin: String?,

    /**
     * If true, this event was published by this client on this instance. Other clients of the same
     * instance do not count.
     */
    public val isFromThisClient: Boolean,
) : AbstractCoroutineContextElement(Key) {
    public companion object Key : CoroutineContext.Key<NatsEventContext>
}

public suspend fun natsEventContextOrNull(): NatsEventContext? = currentCoroutineContext()[NatsEventContext]

public suspend fun natsEventContext(): NatsEventContext = currentCoroutineContext()[NatsEventContext]
    ?: error("NatsEventContext not found. Make sure you are in a NatsListener context.")