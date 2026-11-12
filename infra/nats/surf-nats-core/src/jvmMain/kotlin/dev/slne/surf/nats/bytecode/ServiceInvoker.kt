package dev.slne.surf.nats.bytecode

import dev.slne.surf.nats.InternalNatsApi
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

/**
 * Calls one method of a service implementation with the calling convention of a compiled `suspend` function.
 *
 * [invoke] returns the result or `COROUTINE_SUSPENDED`. Instances are hidden classes created by
 * [ServiceInvokerFactory].
 */
@InternalNatsApi
internal fun interface ServiceInvoker {
    fun invoke(request: Any, continuation: Continuation<Any?>): Any?
}

/**
 * Calls the service method with [request] and returns its result.
 */
@InternalNatsApi
internal suspend fun ServiceInvoker.call(request: Any): Any? {
    return suspendCoroutineUninterceptedOrReturn { invoke(request, it) }
}
