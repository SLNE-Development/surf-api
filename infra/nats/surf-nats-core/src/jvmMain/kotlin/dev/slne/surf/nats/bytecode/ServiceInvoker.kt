package dev.slne.surf.nats.bytecode

import dev.slne.surf.nats.InternalNatsApi
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

@InternalNatsApi
internal fun interface ServiceInvoker {
    fun invoke(request: Any, continuation: Continuation<Any?>): Any?
}

@InternalNatsApi
internal suspend fun ServiceInvoker.call(request: Any): Any? {
    return suspendCoroutineUninterceptedOrReturn { invoke(request, it) }
}
