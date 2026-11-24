package dev.slne.surf.api.spring.context

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Runs [action] on every element, passing a failure to [logFailure] instead of stopping.
 *
 * A [VirtualMachineError] is rethrown. When the calling coroutine is cancelled, a caught
 * [CancellationException] ends the loop with the caller's own cancellation exception; otherwise it
 * counts as the element's failure.
 */
internal suspend inline fun <T> Iterable<T>.forEachLoggingFailures(
    logFailure: (T, Throwable) -> Unit,
    action: (T) -> Unit,
) {
    for (element in this) {
        try {
            action(element)
        } catch (exception: VirtualMachineError) {
            throw exception
        } catch (exception: Throwable) {
            if (exception is CancellationException) {
                currentCoroutineContext().ensureActive()
            }

            logFailure(element, exception)
        }
    }
}
