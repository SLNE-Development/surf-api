package dev.slne.surf.surfapi.bukkit.api.gui.props

/**
 * Default pagination prop for managing paginated content in GUIs.
 * Viewer-specific pagination state.
 */
class PaginationProp<T>(
    override val name: String = "pagination",
    private val items: () -> List<T>,
    private val pageSize: Int = 9
) : Prop<PaginationState<T>> {
    
    private val currentPages = mutableMapOf<java.util.UUID, Int>()
    
    override fun get(): PaginationState<T> {
        // Default state when no viewer context
        return getState(0)
    }
    
    fun get(viewerId: java.util.UUID): PaginationState<T> {
        val currentPage = currentPages.getOrPut(viewerId) { 0 }
        return getState(currentPage)
    }
    
    private fun getState(currentPage: Int): PaginationState<T> {
        val allItems = items()
        val totalPages = (allItems.size + pageSize - 1) / pageSize
        
        val startIndex = currentPage * pageSize
        val endIndex = minOf(startIndex + pageSize, allItems.size)
        val pageItems = if (startIndex < allItems.size) {
            allItems.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
        
        return PaginationState(
            items = pageItems,
            currentPage = currentPage,
            totalPages = totalPages,
            pageSize = pageSize,
            totalItems = allItems.size,
            hasNextPage = currentPage < totalPages - 1,
            hasPreviousPage = currentPage > 0
        )
    }
    
    fun nextPage(viewerId: java.util.UUID) {
        val current = currentPages.getOrPut(viewerId) { 0 }
        val state = getState(current)
        if (state.hasNextPage) {
            currentPages[viewerId] = current + 1
        }
    }
    
    fun previousPage(viewerId: java.util.UUID) {
        val current = currentPages.getOrPut(viewerId) { 0 }
        if (current > 0) {
            currentPages[viewerId] = current - 1
        }
    }
    
    fun setPage(viewerId: java.util.UUID, page: Int) {
        val state = getState(0) // Get state to check total pages
        if (page in 0 until state.totalPages) {
            currentPages[viewerId] = page
        }
    }
    
    fun clear(viewerId: java.util.UUID) {
        currentPages.remove(viewerId)
    }
}

/**
 * State object for pagination.
 */
data class PaginationState<T>(
    val items: List<T>,
    val currentPage: Int,
    val totalPages: Int,
    val pageSize: Int,
    val totalItems: Int,
    val hasNextPage: Boolean,
    val hasPreviousPage: Boolean
)
