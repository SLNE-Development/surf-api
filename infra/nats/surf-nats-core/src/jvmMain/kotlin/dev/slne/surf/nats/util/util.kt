package dev.slne.surf.nats.util

import java.lang.reflect.Method
import kotlin.coroutines.Continuation

/**
 * Checks whether this method is a compiled Kotlin `suspend` function, judged by a trailing [Continuation] parameter.
 */
internal fun Method.isSuspend(): Boolean {
    val params = this.parameterTypes
    if (params.size == 0) {
        return false
    }

    val lastParam = params.last()
    return Continuation::class.java.isAssignableFrom(lastParam)
}

/**
 * Rethrows this throwable when it is a [VirtualMachineError] other than [StackOverflowError], which the process
 * cannot recover from.
 */
internal fun Throwable.rethrowIfFatal() {
    if (this is VirtualMachineError && this !is StackOverflowError) throw this
}
