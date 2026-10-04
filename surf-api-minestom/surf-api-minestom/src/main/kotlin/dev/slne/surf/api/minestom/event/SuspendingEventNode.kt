package dev.slne.surf.api.minestom.event

import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import org.jetbrains.annotations.ApiStatus
import kotlin.coroutines.cancellation.CancellationException

class SuspendingEventNode<E : Any>(private val name: String) {

    companion object {
        private val LOGGER = ComponentLogger.logger()
    }

    private val lock = Any()

    @Volatile
    private var listeners: List<Registration<E>> = emptyList()


    fun addListener(priority: Int = 0, listener: suspend (E) -> Unit): Registration<E> {
        val registration = Registration(this, priority, listener)

        synchronized(lock) {
            listeners = (listeners + registration).sortedBy { it.priority }
        }

        return registration
    }


    @ApiStatus.Internal
    suspend fun call(event: E) {
        for (registration in listeners) {
            try {
                registration.listener(event)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Throwable) {
                LOGGER.error("Listener for '{}' failed, continuing with the next one", name, failure)
            }
        }
    }

    class Registration<E : Any> internal constructor(
        private val node: SuspendingEventNode<E>,
        internal val priority: Int,
        internal val listener: suspend (E) -> Unit
    ) : AutoCloseable {
        override fun close() {
            synchronized(node.lock) {
                node.listeners -= this
            }
        }
    }
}
