@file:Suppress("UnstableApiUsage")

package dev.slne.surf.surfapi.bukkit.api.dialog.composition

import dev.slne.surf.surfapi.bukkit.api.dialog.state.DialogState
import io.papermc.paper.dialog.Dialog
import kotlinx.coroutines.CoroutineScope
import org.bukkit.entity.Player

suspend fun <S : DialogState> composableDialog(
    player: Player,
    initialState: S,
    scope: CoroutineScope,
    content: suspend DialogScope<S>.() -> Dialog
) {
    val store = DialogStore(
        initialState = initialState,
        playerUuid = player.uniqueId,
        renderer = content,
        scope = scope
    )

    store.open()
}