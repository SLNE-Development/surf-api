package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.internal.NatsIdempotencyKeyElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext

/**
 * Sends [key] as the idempotency key of every persistent call made in [block].
 *
 * The responder receives [key] as [NatsPersistentContext.messageId] instead of an identity per call. A call repeated
 * with the same key, for example after a timeout or a lost reply, therefore reaches a responder that deduplicates by
 * message ID, such as a transactional service, as the same operation and receives its stored reply. Every repeat is
 * stored as a new message, so it reaches the responder within and after the two-minute duplicate window of JetStream.
 *
 * The key must be unique per logical action, such as one submitted form, and every repeat must send an equal request;
 * a transactional service answers a reused key with another request with an error. Calls to the same method in one
 * [block] count as repeats of one action. Core calls ignore the key.
 *
 * @throws IllegalArgumentException from a persistent call in [block] if [key] does not have 1 to 128 visible ASCII
 * characters.
 */
public suspend fun <T> withNatsIdempotencyKey(
    key: String,
    block: suspend CoroutineScope.() -> T
): T = withContext(NatsIdempotencyKeyElement(key), block)
