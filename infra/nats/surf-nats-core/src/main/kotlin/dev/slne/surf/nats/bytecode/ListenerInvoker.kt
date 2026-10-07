package dev.slne.surf.nats.bytecode

import kotlin.coroutines.Continuation

fun interface ListenerInvoker {
    fun invoke(event: Any, continuation: Continuation<Unit>): Any?
}