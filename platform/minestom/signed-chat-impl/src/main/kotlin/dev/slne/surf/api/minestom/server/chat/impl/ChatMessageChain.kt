package dev.slne.surf.api.minestom.server.chat.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import kotlin.coroutines.cancellation.CancellationException

internal class ChatMessageChain(private val scope: CoroutineScope) : AutoCloseable {

    companion object {
        private val LOGGER = ComponentLogger.logger()
    }

    private val lock = Any()

    private var head: Job = Job().apply { complete() }

    @Volatile
    private var closed = false

    fun append(task: suspend () -> Unit) {
        if (closed) return

        synchronized(lock) {
            val previous = head

            head = scope.launch {
                previous.join()
                if (closed) return@launch

                try {
                    task()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (failure: Throwable) {
                    LOGGER.error("Chain link failed, continuing to next one", failure)
                }
            }
        }
    }

    override fun close() {
        closed = true
    }
}
