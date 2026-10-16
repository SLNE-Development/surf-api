package dev.slne.surf.api.minestom.coroutine

import kotlinx.coroutines.*
import net.minestom.server.ServerProcess
import net.minestom.server.thread.Acquirable
import net.minestom.server.thread.AcquirableCollection
import net.minestom.server.thread.AcquirableSource
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

val Int.ticks: Duration get() = (this * 50L - 25).milliseconds

fun ServerProcess.launch(
    context: CoroutineContext = MinestomDispatchers.Main,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job {
    if (!minestomScope.isActive) {
        return Job()
    }

    return minestomScope.launch(context, start, block)
}

@Suppress("UnstableApiUsage")
suspend fun <T, R> AcquirableSource<T>.asyncSuspend(block: (T) -> R): R =
    acquirable().asyncSuspend(block)

/**
 * Runs [block] with the element while the thread owning it is held, on the IO dispatcher.
 *
 * [block] may return `null` when [R] is nullable.
 */
@Suppress("UnstableApiUsage")
suspend fun <T, R> Acquirable<T>.asyncSuspend(block: (T) -> R): R {
    val acquire = this
    return withContext(Dispatchers.IO) {
        var result: Any? = UNSET
        acquire.sync { element ->
            result = block.invoke(element)
        }

        check(result !== UNSET) { "Acquirable did not run the block" }
        @Suppress("UNCHECKED_CAST")
        result as R
    }
}

/**
 * Runs [block] with the elements of this collection on the IO dispatcher.
 *
 * The elements are acquired and released one at a time before [block] runs. [block] therefore
 * runs after the acquisition ended and is not synchronized with the tick threads of the elements.
 */
@Suppress("UnstableApiUsage")
suspend fun <T, R> Collection<Acquirable<T>>.asyncSuspend(block: (Collection<T>) -> R): R {
    val acquirableCollection = AcquirableCollection(this)
    return withContext(Dispatchers.IO) {
        val resolved = ArrayList<T>(size)
        acquirableCollection.acquireSync {
            resolved.add(it)
        }
        block.invoke(resolved)
    }
}

private val UNSET = Any()
