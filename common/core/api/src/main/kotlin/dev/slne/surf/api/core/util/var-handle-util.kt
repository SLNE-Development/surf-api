package dev.slne.surf.api.core.util

import java.lang.invoke.VarHandle

/**
 * Sets the value at the specified variable handle to the new value if the new value is greater than the current value.
 *
 * This method ensures that the value is updated only when the provided `newValue` is strictly greater
 * than the current value stored at the variable handle. The operation is performed atomically.
 *
 * @param receiver the object on which the variable handle operates
 * @param newValue the new value to set if greater than the current value
 * @return `true` if the value was updated to `newValue`, `false` if `newValue` was not greater and no update occurred
 */
fun VarHandle.setIfGreater(receiver: Any, newValue: Int): Boolean {
    while (true) {
        val current = getVolatile(receiver) as Int

        if (newValue <= current) {
            return false
        }

        if (compareAndSet(receiver, current, newValue)) {
            return true
        }
    }
}