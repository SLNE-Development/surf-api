package dev.slne.surf.api.paper.dialog.composition

import dev.slne.surf.api.paper.dialog.state.DialogState
import dev.slne.surf.api.paper.extensions.server
import io.papermc.paper.dialog.Dialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

private val NULL_MEMORY_VALUE = Any()

class DialogStore<S : DialogState>(
    initialState: S,
    private val playerUuid: UUID,
    private val renderer: suspend DialogScope<S>.() -> Dialog,
    private val scope: CoroutineScope,
) {
    @Volatile
    private var currentState: S = initialState
    private var mounted = true
    private val stateMutex = Mutex()
    private val stateVersion = AtomicLong()

    private val memory = ConcurrentHashMap<List<Any?>, Any>()

    suspend fun open() {
        rerender()
    }

    suspend fun update(transform: S.() -> S) {
        stateMutex.withLock {
            currentState = currentState.transform()
            stateVersion.incrementAndGet()
        }
        rerender()
    }

    fun getState(): S = currentState

    internal suspend fun rerender() {
        if (!mounted) return

        val version = stateVersion.get()
        val dialog = DialogScope(this, scope).renderer()
        if (mounted && version == stateVersion.get()) {
            findPlayer()?.showDialog(dialog)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> remember(key: List<Any?>, value: Any?) {
        memory[key] = value ?: NULL_MEMORY_VALUE
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> recall(key: List<Any?>): T? {
        val value = memory[key]
        return if (value === NULL_MEMORY_VALUE) null else value as? T
    }

    internal fun hasRemembered(key: List<Any?>): Boolean = memory.containsKey(key)

    private fun findPlayer() = server.getPlayer(playerUuid)

}
