package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.internal.NatsIdempotencyKeyElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext

/**
 * Runs [block] with [key] as the idempotency key of every persistent call made in it.
 *
 * The handler receives [key] as [NatsPersistentContext.messageId] instead of an identity per call. A call repeated
 * with the same key, for example after a timeout or a lost reply, reaches a responder that deduplicates by message
 * ID, such as one with a [NatsPersistentInterceptor] that stores replies, as the same operation, and receives the
 * stored reply. Every repeat is stored as a new message, so it reaches the responder also after the two-minute
 * duplicate window of the stream.
 *
 * Use a key that is unique per logical action, such as one submitted form, and send an equal request with every
 * repeat. A responder may reject a reused key that arrives with a different request. All calls in one [block] carry
 * the same key, so calls to the same method there count as repeats of one action. Calls to methods that are not
 * persistent ignore the key.
 *
 * ```kotlin
 * suspend fun placeOrder(orders: OrderService, formId: String, orderId: Long) =
 *     withNatsIdempotencyKey("order-$formId") {
 *         orders.place(PlaceRequest(orderId))
 *     }
 * ```
 *
 * @return the result of [block].
 * @throws IllegalArgumentException from a persistent call in [block] if [key] does not have 1 to 128 visible ASCII
 * characters.
 */
public suspend fun <T> withNatsIdempotencyKey(
    key: String,
    block: suspend CoroutineScope.() -> T
): T = withContext(NatsIdempotencyKeyElement(key), block)
