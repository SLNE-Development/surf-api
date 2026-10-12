package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

// TODO: 08.10.2026 17:43 - mention in @NatsPersistent kdocs

public class NatsPersistentContext @InternalNatsApi constructor(
    public val messageId: String,
    public val deliveryCount: Long,
) : AbstractCoroutineContextElement(Key) {
    public companion object Key : CoroutineContext.Key<NatsPersistentContext>
}

public suspend fun natsPersistentContextOrNull(): NatsPersistentContext? {
    return currentCoroutineContext()[NatsPersistentContext]
}

public suspend fun natsPersistentContext(): NatsPersistentContext {
    return currentCoroutineContext()[NatsPersistentContext]
        ?: error("NatsPersistentContext not found. Make sure you are in a persistent NatsService method.")
}