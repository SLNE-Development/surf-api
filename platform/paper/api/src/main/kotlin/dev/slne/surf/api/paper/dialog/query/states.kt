package dev.slne.surf.api.paper.dialog.query

import dev.slne.surf.api.paper.dialog.state.DialogState

data class PageState(
    val page: Int = 1,
    val limit: Int = 10,
    val search: String? = null
) : DialogState {
    init {
        require(page >= 1) { "page must be >= 1, but was $page" }
        require(limit > 0) { "limit must be > 0, but was $limit" }
    }
}

data class CursorState(
    val cursor: String? = null,
    val history: List<String?> = emptyList(),
    val limit: Int = 10,
    val search: String? = null
) : DialogState