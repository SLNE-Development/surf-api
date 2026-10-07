package dev.slne.surf.nats.bytecode

import dev.slne.surf.nats.InternalNatsApi
import kotlin.coroutines.Continuation

@InternalNatsApi
fun interface ListenerInvoker {
    fun invoke(event: Any, continuation: Continuation<Unit>): Any?
}