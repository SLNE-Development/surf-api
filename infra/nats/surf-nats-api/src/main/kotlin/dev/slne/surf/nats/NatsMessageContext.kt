package dev.slne.surf.nats

import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

// TODO: 08.10.2026 17:43 - mention in @Persistent kdocs

class NatsMessageContext @InternalNatsApi constructor(
    val messageId: String,
    val deliveryCount: Long,
) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<NatsMessageContext>
}

suspend fun natsMessageContext() = currentCoroutineContext()[NatsMessageContext]