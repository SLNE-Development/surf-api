package dev.slne.surf.nats.bytecode

import dev.slne.surf.nats.InternalNatsApi
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

@InternalNatsApi
internal fun interface ListenerInvoker {
    fun invoke(event: Any, continuation: Continuation<Unit>): Any?
}

@InternalNatsApi
internal suspend fun ListenerInvoker.call(event: Any) {
    suspendCoroutineUninterceptedOrReturn { invoke(event, it) }
}