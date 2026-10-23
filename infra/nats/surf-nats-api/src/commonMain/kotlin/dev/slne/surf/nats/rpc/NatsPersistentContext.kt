package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Provides delivery metadata in a persistent service handler's coroutine context.
 *
 * Accessible from suspending code through [natsPersistentContext] or [natsPersistentContextOrNull]
 * for both persistent fire calls and persistent RPC.
 *
 * @property messageId the message identity, stable across redeliveries of the same message.
 * Separate calls with equal arguments receive different identities. Deduplicate them using a domain key.
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
 * Returns the current [NatsPersistentContext], or null outside a persistent handler's coroutine context.
 */
public suspend fun natsPersistentContextOrNull(): NatsPersistentContext? {
    return currentCoroutineContext()[NatsPersistentContext]
}

/**
 * Returns the current persistent service handler's [NatsPersistentContext].
 *
 * @throws IllegalStateException if the current coroutine has no persistent handler context.
 */
public suspend fun natsPersistentContext(): NatsPersistentContext {
    return currentCoroutineContext()[NatsPersistentContext]
        ?: error("NatsPersistentContext not found. Make sure you are in a persistent service method.")
}