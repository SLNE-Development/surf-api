package dev.slne.surf.surfapi.bukkit.api.gui.component

import dev.slne.surf.surfapi.bukkit.api.builder.ItemStack
import dev.slne.surf.surfapi.bukkit.api.builder.buildLore
import dev.slne.surf.surfapi.bukkit.api.builder.displayName
import dev.slne.surf.surfapi.bukkit.api.gui.GuiItem
import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.area.ComponentArea
import dev.slne.surf.surfapi.bukkit.api.gui.area.CuboidArea
import dev.slne.surf.surfapi.bukkit.api.gui.context.ClickContext
import dev.slne.surf.surfapi.bukkit.api.gui.context.ViewContext
import dev.slne.surf.surfapi.bukkit.api.gui.dsl.component
import dev.slne.surf.surfapi.bukkit.api.gui.dsl.dynamicComponent
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.*

/**
 * Component for paginated content.
 * Renders multiple items across specified slots based on current page.
 */
class PaginationComponent<T>(
    startSlot: Slot,
    endSlot: Slot,
    private val items: () -> List<T>,
    private val itemRenderer: (T, ViewContext) -> GuiItem?,
    override val priority: ComponentPriority = ComponentPriority.NORMAL,
    private val onItemClick: ((T, ClickContext) -> Unit)? = null
) : ContainerComponent(CuboidArea(startSlot, endSlot), priority) {
    private val currentPages = mutableMapOf<UUID, Int>()
    
    /**
     * Calculate page size from the area.
     */
    private val pageSize: Int = width * height
    
    /**
     * Start slot of the area (convenience accessor).
     */
    private val startSlot: Slot
        get() = (area as CuboidArea).startSlot

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
                // Calculate slot position within the area
                val row = index / width
                val col = index % width
                val slot = Slot.at(startSlot.column + col, startSlot.row + row)
                renderedSlots[slot] = guiItem
            }
        }

        return renderedSlots
    }

    override fun onClick(context: ClickContext) {
        if (onItemClick != null) {
            val pageItems = getPageItems(context.player)
            
            // Calculate the index within the pagination area
            val relativeCol = context.slot.column - startSlot.column
            val relativeRow = context.slot.row - startSlot.row
            val index = relativeRow * width + relativeCol

            if (index in pageItems.indices) {
                val item = pageItems[index]
                onItemClick.invoke(item, context)
            }
        }
    }

    companion object {
        val NEXT_PAGE_ITEM = GuiItem(ItemStack(Material.ARROW) {
            displayName {
                info("Next Page")
            }

            buildLore {
                line {
                    gray("Click to go to the next page")
                }
            }
        })

        fun buildNextPageComponent(paginationComponent: PaginationComponent<*>, slot: Slot) =
            component(slot, NEXT_PAGE_ITEM) {
                onClick = {
                    paginationComponent.nextPage(player)
                    view.update()
                }
            }

        val PREVIOUS_PAGE_ITEM = GuiItem(ItemStack(Material.ARROW) {
            displayName {
                info("Previous Page")
            }

            buildLore {
                line {
                    gray("Click to go to the previous page")
                }
            }
        })

        fun buildPreviousPageComponent(paginationComponent: PaginationComponent<*>, slot: Slot) =
            component(slot, PREVIOUS_PAGE_ITEM) {
                onClick = {
                    paginationComponent.previousPage(player)
                    view.update()
                }
            }

        fun buildPageIndicatorItem(pageNumber: Int, maxPages: Int) =
            GuiItem(ItemStack(Material.PAPER) {
                displayName {
                    info("Page $pageNumber")
                }

                buildLore {
                    line {
                        gray("Page $pageNumber of $maxPages")
                    }
                }
            })

        fun buildPageIndicatorComponent(paginationComponent: PaginationComponent<*>, slot: Slot) =
            dynamicComponent(slot, renderer = { ctx ->
                val currentPage = paginationComponent.getCurrentPage(ctx.player) + 1
                val totalPages = paginationComponent.getTotalPages()

                buildPageIndicatorItem(currentPage, totalPages)
            })
    }
}
