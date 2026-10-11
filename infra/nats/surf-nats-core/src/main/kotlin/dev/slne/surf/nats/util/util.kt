package dev.slne.surf.nats.util

import java.lang.reflect.Method
import kotlin.coroutines.Continuation

internal fun Method.isSuspend(): Boolean {
    val params = this.parameterTypes
    if (params.size == 0) {
        return false
    }

    val lastParam = params.last()
    return Continuation::class.java.isAssignableFrom(lastParam)
}

internal fun Throwable.rethrowIfFatal() {
    if (this is VirtualMachineError && this !is StackOverflowError) throw this
}
