package dev.slne.surf.surfapi.bukkit.api.gui.props

/**
 * Default pagination prop for managing paginated content in GUIs.
 */
class PaginationProp<T>(
    override val name: String = "pagination",
    private val items: () -> List<T>,
    private val pageSize: Int = 9,
    private val scope: PropScope = PropScope.VIEWER
) : Prop<PaginationState<T>> {
    
    private val currentPages = mutableMapOf<java.util.UUID, Int>()
    
    override fun get(context: PropContext): PaginationState<T> {
        val currentPage = currentPages.getOrPut(context.viewerId) { 0 }
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
    
    fun nextPage(context: PropContext) {
        val current = currentPages.getOrPut(context.viewerId) { 0 }
        val state = get(context)
        if (state.hasNextPage) {
            currentPages[context.viewerId] = current + 1
        }
    }
    
    fun previousPage(context: PropContext) {
        val current = currentPages.getOrPut(context.viewerId) { 0 }
        if (current > 0) {
            currentPages[context.viewerId] = current - 1
        }
    }
    
    fun setPage(context: PropContext, page: Int) {
        val state = get(context)
        if (page in 0 until state.totalPages) {
            currentPages[context.viewerId] = page
        }
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
