package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Provides delivery metadata of a persistent message to its service handler.
 *
 * Present in the coroutine context of handlers of persistent methods, both fire calls and persistent RPC, and of a
 * [NatsPersistentInterceptor] wrapping them. Read it with [natsPersistentContext] or [natsPersistentContextOrNull].
 * Persistent messages can be delivered more than once, so handlers use [messageId] or a domain key to make repeated
 * deliveries harmless.
 *
 * ```kotlin
 * override suspend fun place(request: PlaceRequest) {
 *     val context = natsPersistentContext()
 *     if (context.deliveryCount > 1) println("Redelivery ${context.deliveryCount} of ${context.messageId}")
 * }
 * ```
 *
 * @property messageId the message identity, stable across redeliveries and dead-letter replays of the same message.
 * Separate calls receive different identities, also when their requests are equal, unless the caller repeats an
 * idempotency key ([withNatsIdempotencyKey], `CallOptions.idempotencyKey` in JavaScript), which is then the identity
 * of every repeat. Without a key, deduplicate equal requests using a domain key. Keep deduplication records at least
 * for [NatsPersistentLimits.REDELIVERY_HORIZON].
 * @property deliveryCount the number of times the message has been delivered, starting at one.
 */
public class NatsPersistentContext @InternalNatsApi constructor(
    public val messageId: String,
    public val deliveryCount: Long,
) : AbstractCoroutineContextElement(Key) {
    /**
     * Key for retrieving [NatsPersistentContext] from a coroutine context.
     */
    public companion object Key : CoroutineContext.Key<NatsPersistentContext>
}

/**
 * Returns the [NatsPersistentContext] of the current coroutine, or `null` outside a persistent handler.
 */
public suspend fun natsPersistentContextOrNull(): NatsPersistentContext? {
    return currentCoroutineContext()[NatsPersistentContext]
}

/**
 * Returns the [NatsPersistentContext] of the current persistent service handler.
 *
 * @throws IllegalStateException if the current coroutine is not running a persistent handler.
 */
public suspend fun natsPersistentContext(): NatsPersistentContext {
    return currentCoroutineContext()[NatsPersistentContext]
        ?: error("NatsPersistentContext not found. Make sure you are in a persistent service method.")
}