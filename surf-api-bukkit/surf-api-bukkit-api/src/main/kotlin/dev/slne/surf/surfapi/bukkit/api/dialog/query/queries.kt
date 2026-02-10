package dev.slne.surf.surfapi.bukkit.api.dialog.query

import dev.slne.surf.surfapi.bukkit.api.dialog.state.DialogState

fun interface DialogQuery<S : DialogState, T> {
    suspend fun execute(state: S): PageResult<T>
}

fun interface CursorDialogQuery<S : DialogState, T> {
    suspend fun execute(state: S): CursorResult<T>
}