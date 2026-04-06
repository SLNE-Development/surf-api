package dev.slne.surf.api.paper.dialog.query

import dev.slne.surf.api.paper.dialog.state.DialogState

fun interface DialogQuery<S : DialogState, T> {
    suspend fun execute(state: S): PageResult<T>
}

fun interface CursorDialogQuery<S : DialogState, T> {
    suspend fun execute(state: S): CursorResult<T>
}