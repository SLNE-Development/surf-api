package dev.slne.surf.nats.bytecode

import dev.slne.surf.nats.InternalNatsApi
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

/**
 * Calls one listener method with the calling convention of a compiled `suspend` function.
 *
 * [invoke] returns the result or `COROUTINE_SUSPENDED`. Instances are hidden classes created by
 * [ListenerInvokerFactory].
 */
@InternalNatsApi
internal fun interface ListenerInvoker {
    fun invoke(event: Any, continuation: Continuation<Unit>): Any?
}

/**
 * Calls the listener method with [event] and suspends until it completes.
 */
@InternalNatsApi
internal suspend fun ListenerInvoker.call(event: Any) {
    suspendCoroutineUninterceptedOrReturn { invoke(event, it) }
}