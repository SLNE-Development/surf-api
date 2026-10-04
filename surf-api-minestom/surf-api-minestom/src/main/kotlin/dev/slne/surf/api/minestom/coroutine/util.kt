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

@Suppress("UnstableApiUsage")
suspend fun <T, R> Acquirable<T>.asyncSuspend(block: (T) -> R): R {
    val acquire = this
    return withContext(Dispatchers.IO) {
        var result: R? = null
        acquire.sync { element ->
            result = block.invoke(element)
        }
        result!!
    }
}

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