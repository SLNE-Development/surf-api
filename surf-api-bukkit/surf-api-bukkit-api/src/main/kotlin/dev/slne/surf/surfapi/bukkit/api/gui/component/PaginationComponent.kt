package dev.slne.surf.surfapi.bukkit.api.gui.component

import dev.slne.surf.surfapi.bukkit.api.gui.GuiItem
import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.context.ClickContext
import dev.slne.surf.surfapi.bukkit.api.gui.context.ViewContext
import org.bukkit.entity.Player
import java.util.*

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
    private val currentPages = mutableMapOf<UUID, Int>()

    /**
     * Get the current page for a viewer.
     */
    fun getCurrentPage(viewer: Player): Int {
        return currentPages.getOrDefault(viewer.uniqueId, 0)
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
    fun getPageItems(viewer: Player): List<T> {
        val allItems = items()
        val currentPage = getCurrentPage(viewer)
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
    fun hasNextPage(viewer: Player): Boolean {
        val currentPage = getCurrentPage(viewer)

        return currentPage < getTotalPages() - 1
    }

    /**
     * Check if there is a previous page for a viewer.
     */
    fun hasPreviousPage(viewer: Player): Boolean {
        return getCurrentPage(viewer) > 0
    }

    /**
     * Go to the next page for a viewer.
     */
    fun nextPage(viewer: Player) {
        if (hasNextPage(viewer)) {
            currentPages[viewer.uniqueId] = getCurrentPage(viewer) + 1
        }
    }

    /**
     * Go to the previous page for a viewer.
     */
    fun previousPage(viewer: Player) {
        val currentPage = getCurrentPage(viewer)

        if (currentPage > 0) {
            currentPages[viewer.uniqueId] = currentPage - 1
        }
    }

    /**
     * Set a specific page for a viewer.
     */
    fun setPage(viewerId: UUID, page: Int) {
        if (page in 0 until getTotalPages()) {
            currentPages[viewerId] = page
        }
    }

    /**
     * Clear the page state for a viewer.
     */
    fun clearPage(viewerId: UUID) {
        currentPages.remove(viewerId)
    }

    override fun renderSlots(context: ViewContext): Map<Slot, GuiItem> {
        val pageItems = getPageItems(context.player)
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
            val pageItems = getPageItems(context.player)
            val slotIndex = context.slot.index

            if (slotIndex in pageItems.indices) {
                val item = pageItems[slotIndex]
                
                onItemClick.invoke(item, context)
            }
        }
    }
}
