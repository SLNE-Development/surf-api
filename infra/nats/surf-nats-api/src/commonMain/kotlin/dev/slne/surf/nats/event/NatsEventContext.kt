package dev.slne.surf.nats.event

import dev.slne.surf.nats.InternalNatsApi
import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Provides publisher metadata in a listener's coroutine context.
 *
 * Available to suspending listeners through [natsEventContext] or [natsEventContextOrNull].
 *
 * @property origin the publisher's identity as `client@instanceId`, with a random instance ID per
 * JVM, or null if the publisher supplied no origin.
 * @property isFromThisClient whether the event was published by this client in this JVM.
 * Events from other clients in the same JVM are treated as coming from a different client.
 */
public class NatsEventContext @InternalNatsApi constructor(

    public val origin: String?,

    public val isFromThisClient: Boolean,
) : AbstractCoroutineContextElement(Key) {
    /**
     * Key for retrieving [NatsEventContext] from a coroutine context.
     */
    public companion object Key : CoroutineContext.Key<NatsEventContext>
}

/**
 * Returns the current listener's [NatsEventContext], or null outside its coroutine context.
 */
public suspend fun natsEventContextOrNull(): NatsEventContext? = currentCoroutineContext()[NatsEventContext]

/**
 * Returns the current listener's [NatsEventContext].
 *
 * @throws IllegalStateException if the current coroutine has no listener context.
 */
public suspend fun natsEventContext(): NatsEventContext = currentCoroutineContext()[NatsEventContext]
    ?: error("NatsEventContext not found. Make sure you are in a NatsListener context.")