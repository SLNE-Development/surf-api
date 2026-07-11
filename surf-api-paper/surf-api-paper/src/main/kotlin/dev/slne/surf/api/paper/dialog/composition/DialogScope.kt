@file:Suppress("UnstableApiUsage")

package dev.slne.surf.api.paper.dialog.composition

import dev.slne.surf.api.paper.dialog.state.DialogState
import kotlinx.coroutines.CoroutineScope

class DialogScope<S : DialogState>(
    private val store: DialogStore<S>,
    private val scope: CoroutineScope,
) {
    fun state(): S = store.getState()

    suspend fun setState(transform: S.() -> S) {
        store.update(transform)
    }

    suspend fun <T> remember(
        vararg keys: Any?,
        block: suspend CoroutineScope.() -> T
    ): T {
        val key = keys.toList()
        if (store.hasRemembered(key)) {
            @Suppress("UNCHECKED_CAST")
            return store.recall<T>(key) as T
        }

        val value = block(scope)

        store.remember<T>(key, value)

        return value
    }
}
