package dev.slne.surf.nats.bytecode

import dev.slne.surf.nats.InternalNatsApi
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

@InternalNatsApi
fun interface ServiceInvoker {
    fun invoke(args: Array<Any?>, continuation: Continuation<Any?>): Any?
}

@InternalNatsApi
suspend fun ServiceInvoker.call(args: Array<Any?>): Any? {
    return suspendCoroutineUninterceptedOrReturn { invoke(args, it) }
}