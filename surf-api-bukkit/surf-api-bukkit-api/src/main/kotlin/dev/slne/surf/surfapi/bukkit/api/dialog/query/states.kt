package dev.slne.surf.surfapi.bukkit.api.dialog.query

import dev.slne.surf.surfapi.bukkit.api.dialog.state.DialogState

data class PageState(
    val page: Int = 1,
    val limit: Int = 10,
    val search: String? = null
) : DialogState

data class CursorState(
    val cursor: String? = null,
    val history: List<String?> = emptyList(),
    val limit: Int = 10,
    val search: String? = null
) : DialogState