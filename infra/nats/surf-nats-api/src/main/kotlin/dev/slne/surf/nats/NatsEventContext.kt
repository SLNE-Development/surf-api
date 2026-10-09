package dev.slne.surf.nats

import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class NatsEventContext @InternalNatsApi constructor(

    /**
     * <client>@<instanceId> of the publisher, if known.
     */
    val origin: String?,

    /**
     * If true, this event was published by this instance.
     */
    val isOwn: Boolean,
) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<NatsEventContext>
}

suspend fun natsEventContextOrNull() = currentCoroutineContext()[NatsEventContext]

suspend fun natsEventContext() = currentCoroutineContext()[NatsEventContext]
    ?: error("NatsEventContext not found. Make sure you are in a NatsListener context.")