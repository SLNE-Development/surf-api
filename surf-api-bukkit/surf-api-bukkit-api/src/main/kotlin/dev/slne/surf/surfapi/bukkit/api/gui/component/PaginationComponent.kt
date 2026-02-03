package dev.slne.surf.surfapi.bukkit.api.gui.component

import dev.slne.surf.surfapi.bukkit.api.gui.GuiItem
import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.context.ClickContext
import dev.slne.surf.surfapi.bukkit.api.gui.context.ViewContext

/**
 * Component for paginated content.
 * Renders multiple items across specified slots based on current page.
 */
class PaginationComponent<T>(
    private val items: () -> List<T>,
    private val pageSize: Int = 9,
    private val itemRenderer: (T, ViewContext) -> GuiItem?,
    private val onItemClick: ((T, ClickContext) -> Unit)? = null
) : ContainerComponent() {
    
    private val currentPages = mutableMapOf<java.util.UUID, Int>()
    
    /**
     * Get the current page for a viewer.
     */
    fun getCurrentPage(viewerId: java.util.UUID): Int {
        return currentPages.getOrDefault(viewerId, 0)
    }
    
    /**
     * Get the total number of pages.
     */
    fun getTotalPages(): Int {
        val allItems = items()
        return (allItems.size + pageSize - 1) / pageSize
    }
    
    /**
     * Get the items for the current page of a viewer.
     */
    fun getPageItems(viewerId: java.util.UUID): List<T> {
        val allItems = items()
        val currentPage = getCurrentPage(viewerId)
        val startIndex = currentPage * pageSize
        val endIndex = minOf(startIndex + pageSize, allItems.size)
        
        return if (startIndex < allItems.size) {
            allItems.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
    }
    
    /**
     * Check if there is a next page for a viewer.
     */
    fun hasNextPage(viewerId: java.util.UUID): Boolean {
        val currentPage = getCurrentPage(viewerId)
        return currentPage < getTotalPages() - 1
    }
    
    /**
     * Check if there is a previous page for a viewer.
     */
    fun hasPreviousPage(viewerId: java.util.UUID): Boolean {
        return getCurrentPage(viewerId) > 0
    }
    
    /**
     * Go to the next page for a viewer.
     */
    fun nextPage(viewerId: java.util.UUID) {
        if (hasNextPage(viewerId)) {
            currentPages[viewerId] = getCurrentPage(viewerId) + 1
        }
    }
    
    /**
     * Go to the previous page for a viewer.
     */
    fun previousPage(viewerId: java.util.UUID) {
        val currentPage = getCurrentPage(viewerId)
        if (currentPage > 0) {
            currentPages[viewerId] = currentPage - 1
        }
    }
    
    /**
     * Set a specific page for a viewer.
     */
    fun setPage(viewerId: java.util.UUID, page: Int) {
        if (page in 0 until getTotalPages()) {
            currentPages[viewerId] = page
        }
    }
    
    /**
     * Clear the page state for a viewer.
     */
    fun clearPage(viewerId: java.util.UUID) {
        currentPages.remove(viewerId)
    }
    
    override fun renderSlots(context: ViewContext): Map<Slot, GuiItem> {
        val pageItems = getPageItems(context.player.uniqueId)
        val renderedSlots = mutableMapOf<Slot, GuiItem>()
        
        pageItems.forEachIndexed { index, item ->
            val guiItem = itemRenderer(item, context)
            if (guiItem != null) {
                renderedSlots[Slot.of(index)] = guiItem
            }
        }
        
        return renderedSlots
    }
    
    override fun onClick(context: ClickContext) {
        if (onItemClick != null) {
            val pageItems = getPageItems(context.player.uniqueId)
            val slotIndex = context.slot.index
            if (slotIndex in pageItems.indices) {
                val item = pageItems[slotIndex]
                onItemClick.invoke(item, context)
            }
        }
    }
}
