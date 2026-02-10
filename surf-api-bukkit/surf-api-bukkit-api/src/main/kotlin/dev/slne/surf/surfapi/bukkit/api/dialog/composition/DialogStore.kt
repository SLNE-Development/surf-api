package dev.slne.surf.surfapi.bukkit.api.dialog.composition

import dev.slne.surf.surfapi.bukkit.api.dialog.state.DialogState
import dev.slne.surf.surfapi.bukkit.api.extensions.server
import io.papermc.paper.dialog.Dialog
import kotlinx.coroutines.CoroutineScope
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class DialogStore<S : DialogState>(
    initialState: S,
    private val playerUuid: UUID,
    private val renderer: suspend DialogScope<S>.() -> Dialog,
    private val scope: CoroutineScope,
) {
    private var currentState: S = initialState
    private var mounted = true

    private val memory = ConcurrentHashMap<List<Any?>, Any?>()

    suspend fun open() {
        rerender()
    }

    suspend fun update(transform: S.() -> S) {
        currentState = currentState.transform()
        rerender()
    }

    fun getState(): S = currentState

    internal suspend fun rerender() {
        if (!mounted) return

        val dialog = DialogScope(this, scope).renderer()
        findPlayer()?.showDialog(dialog)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> remember(key: List<Any?>, value: Any?) {
        memory[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> recall(key: List<Any?>): T? {
        return memory[key] as? T
    }

    private fun findPlayer() = server.getPlayer(playerUuid)
}