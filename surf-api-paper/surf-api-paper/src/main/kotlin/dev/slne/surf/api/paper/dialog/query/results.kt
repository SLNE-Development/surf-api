package dev.slne.surf.api.paper.dialog.query

data class PageResult<T>(
    val items: List<T>,
    val page: Int,
    val totalPages: Int,
    val totalItems: Int? = null,
)

data class CursorResult<T>(
    val items: List<T>,
    val nextCursor: String?,
    val hasMore: Boolean
)