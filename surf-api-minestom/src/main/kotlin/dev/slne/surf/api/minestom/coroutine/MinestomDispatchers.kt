package dev.slne.surf.api.minestom.coroutine

import dev.slne.surf.api.minestom.extension.SchedulerManager
import kotlinx.coroutines.*
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule
import java.lang.Runnable
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

object MinestomDispatchers {

    val Blocking: CoroutineDispatcher = Executors.newThreadPerTaskExecutor(
        Thread.ofVirtual().name("surf-blocking-", 0).factory()
    ).asCoroutineDispatcher()

    @OptIn(InternalCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    val Main: CoroutineDispatcher = object : CoroutineDispatcher(), Delay {
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            SchedulerManager.scheduleNextTick(block)
        }

        override fun scheduleResumeAfterDelay(
            timeMillis: Long,
            continuation: CancellableContinuation<Unit>
        ) {
            val task = schedule(timeMillis) {
                with(continuation) {
                    resumeUndispatched(Unit)
                }
            }

            continuation.invokeOnCancellation {
                task.cancel()
            }
        }

        override fun invokeOnTimeout(
            timeMillis: Long,
            block: Runnable,
            context: CoroutineContext
        ): DisposableHandle {
            val task = schedule(timeMillis, block)

            return DisposableHandle {
                task.cancel()
            }
        }

        private fun schedule(timeMillis: Long, block: Runnable): Task {
            val delay = when {
                timeMillis <= 0L -> TaskSchedule.immediate()
                else -> TaskSchedule.millis(timeMillis)
            }

            return SchedulerManager.scheduleTask(
                block,
                delay,
                TaskSchedule.stop()
            )
        }

        override fun toString(): String = "MinestomDispatchers.Main"
    }
}