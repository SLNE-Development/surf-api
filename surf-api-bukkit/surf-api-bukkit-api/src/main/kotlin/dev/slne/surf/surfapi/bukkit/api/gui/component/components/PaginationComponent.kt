package dev.slne.surf.surfapi.bukkit.api.gui.component.components

import dev.slne.surf.surfapi.bukkit.api.builder.ItemStack
import dev.slne.surf.surfapi.bukkit.api.builder.buildLore
import dev.slne.surf.surfapi.bukkit.api.builder.displayName
import dev.slne.surf.surfapi.bukkit.api.gui.GuiItem
import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.area.ComponentArea
import dev.slne.surf.surfapi.bukkit.api.gui.area.CuboidArea
import dev.slne.surf.surfapi.bukkit.api.gui.component.ComponentPriority
import dev.slne.surf.surfapi.bukkit.api.gui.context.ClickContext
import dev.slne.surf.surfapi.bukkit.api.gui.context.LifecycleContext
import dev.slne.surf.surfapi.bukkit.api.gui.context.ViewContext
import dev.slne.surf.surfapi.bukkit.api.gui.dsl.component
import dev.slne.surf.surfapi.bukkit.api.gui.dsl.dynamicComponent
import dev.slne.surf.surfapi.bukkit.api.gui.props.ViewerProp
import dev.slne.surf.surfapi.core.api.util.mutableObject2ObjectMapOf
import dev.slne.surf.surfapi.core.api.util.objectListOf
import dev.slne.surf.surfapi.core.api.util.toObjectList
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.ObjectList
import org.bukkit.Material
import org.bukkit.entity.Player

/**
 * Component for paginated content with built-in navigation buttons.
 * Renders multiple items across specified slots based on current page.
 * The last row is reserved for navigation buttons (previous, page indicator, next).
 * 
 * Minimum dimensions: width ≥ 3, height ≥ 2
 */
class PaginationComponent<T>(
    startSlot: Slot,
    endSlot: Slot,
    private val items: () -> ObjectList<T>,
    private val itemRenderer: (T, ViewContext) -> GuiItem?,
    override val priority: ComponentPriority = ComponentPriority.NORMAL,
    private val onItemClick: ((T, ClickContext) -> Unit)? = null,
    private val previousButtonSlot: Slot? = null,
    private val nextButtonSlot: Slot? = null,
    private val pageIndicatorSlot: Slot? = null,
    override val area: ComponentArea = CuboidArea(startSlot, endSlot)
) : ContainerComponent(area, priority) {
    private val currentPages = ViewerProp.Mutable("pagination_current_page", 0)

    init {
        require(area.width >= 3) { "PaginationComponent width must be at least 3 (current: ${area.width})" }
        require(area.height >= 2) { "PaginationComponent height must be at least 2 (current: ${area.height})" }

        props.add(currentPages)
    }

    /**
     * Start slot of the area (convenience accessor).
     */
    private val startSlot: Slot
        get() = area.first()

    /**
     * Calculate page size from the area.
     * Items use height - 1 rows (last row is for buttons).
     */
    private val itemsHeight: Int = height - 1
    private val pageSize: Int = width * itemsHeight

    /**
     * Calculated button slots (centered in last row by default).
     */
    private val calculatedPreviousButtonSlot: Slot
        get() = previousButtonSlot ?: run {
            val lastRowY = startSlot.row + height - 1
            val centerX = startSlot.column + (width - 3) / 2

            Slot.at(centerX, lastRowY)
        }

    private val calculatedPageIndicatorSlot: Slot
        get() = pageIndicatorSlot ?: run {
            val lastRowY = startSlot.row + height - 1
            val centerX = startSlot.column + (width - 3) / 2 + 1

            Slot.at(centerX, lastRowY)
        }

    private val calculatedNextButtonSlot: Slot
        get() = nextButtonSlot ?: run {
            val lastRowY = startSlot.row + height - 1
            val centerX = startSlot.column + (width - 3) / 2 + 2

            Slot.at(centerX, lastRowY)
        }

    private val previousButtonComponent = createPreviousButtonComponent()
    private val nextButtonComponent = createNextButtonComponent()
    private val pageIndicatorComponent = createPageIndicatorComponent()

    init {
        addChild(previousButtonComponent)
        addChild(pageIndicatorComponent)
        addChild(nextButtonComponent)
    }

    private fun createPreviousButtonComponent() = component(
        slot = calculatedPreviousButtonSlot,
        item = GuiItem(ItemStack(Material.ARROW) {
            displayName { info("Previous Page") }
            buildLore {
                line { gray("Click to go to the previous page") }
            }
        }),
    ) {
        initComponent = {
            if (!hasPreviousPage(player)) {
                disabled = true
                hidden = true
            }
        }

        onClick = {
            previousPage(player)
        }
    }

    private fun createPageIndicatorComponent() = dynamicComponent(
        slot = calculatedPageIndicatorSlot,
        renderer = { ctx ->
            val currentPage = getCurrentPage(ctx.player) + 1
            val totalPages = getTotalPages()

            GuiItem(ItemStack(Material.PAPER) {
                displayName { info("Page $currentPage") }
                buildLore {
                    line { gray("Page $currentPage of $totalPages") }
                }
            })
        },
    )

    private fun createNextButtonComponent() = component(
        slot = calculatedNextButtonSlot,
        item = GuiItem(ItemStack(Material.ARROW) {
            displayName { info("Next Page") }
            buildLore {
                line { gray("Click to go to the next page") }
            }
        }),
    ) {
        initComponent = {
            if (!hasNextPage(player)) {
                disabled = true
                hidden = true
            }
        }
        onClick = {
            nextPage(player)
        }
    }

    /**
     * Get the current page for a viewer.
     */
    fun getCurrentPage(viewer: Player): Int {
        return currentPages.getOrDefault(viewer, 0)
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
    fun getPageItems(viewer: Player): ObjectList<T> {
        val allItems = items()
        val currentPage = getCurrentPage(viewer)
        val startIndex = currentPage * pageSize
        val endIndex = minOf(startIndex + pageSize, allItems.size)

        return if (startIndex < allItems.size) {
            allItems.subList(startIndex, endIndex).toObjectList()
        } else {
            objectListOf()
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
     * Update the state of navigation buttons based on available pages.
     */
    private fun updateNavigationButtonsState(viewer: Player) {
        val hasPrev = hasPreviousPage(viewer)
        val hasNext = hasNextPage(viewer)

        previousButtonComponent.hidden = !hasPrev
        previousButtonComponent.disabled = !hasPrev

        nextButtonComponent.hidden = !hasNext
        nextButtonComponent.disabled = !hasNext
    }

    /**
     * Go to the next page for a viewer.
     */
    fun nextPage(viewer: Player) {
        val currentPage = getCurrentPage(viewer)

        if (hasNextPage(viewer)) {
            setPage(viewer, currentPage + 1)
            this@PaginationComponent.update(viewer)
        }
    }

    /**
     * Go to the previous page for a viewer.
     */
    fun previousPage(viewer: Player) {
        val currentPage = getCurrentPage(viewer)

        if (hasPreviousPage(viewer)) {
            setPage(viewer, currentPage - 1)
            this@PaginationComponent.update(viewer)
        }
    }

    /**
     * Set a specific page for a viewer.
     */
    fun setPage(viewer: Player, page: Int) {
        if (page in 0 until getTotalPages()) {
            currentPages.set(viewer, page)
        }
    }

    /**
     * Clear the page state for a viewer.
     */
    fun clearPage(viewer: Player) {
        currentPages.clear(viewer)
    }

    override fun onUpdate(context: LifecycleContext) {
        updateNavigationButtonsState(context.player)
    }

    override fun renderSlots(context: ViewContext): Object2ObjectMap<Slot, GuiItem> {
        val pageItems = getPageItems(context.player)
        val renderedSlots = mutableObject2ObjectMapOf<Slot, GuiItem>()

        pageItems.forEachIndexed { index, item ->
            val guiItem = itemRenderer(item, context)

            if (guiItem != null) {
                // Calculate slot position within the items area (height - 1)
                val row = index / width
                val col = index % width

                // Only render if within items area (not in button row)
                if (row < itemsHeight) {
                    val slot = Slot.at(startSlot.column + col, startSlot.row + row)

                    renderedSlots[slot] = guiItem
                }
            }
        }

        return renderedSlots
    }

    override fun onClick(context: ClickContext) {
        // Check if click is in the items area (not button row)
        val relativeRow = context.slot.row - startSlot.row

        if (relativeRow >= itemsHeight) {
            // Click is in button row, let children handle it
            return
        }

        if (onItemClick != null) {
            val pageItems = getPageItems(context.player)

            // Calculate the index within the pagination area
            val relativeCol = context.slot.column - startSlot.column
            val index = relativeRow * width + relativeCol

            if (index in pageItems.indices) {
                val item = pageItems[index]

                onItemClick.invoke(item, context)
            }
        }
    }

    override fun toString(): String {
        return "PaginationComponent(items=$items, itemRenderer=$itemRenderer, priority=$priority, onItemClick=$onItemClick, previousButtonSlot=$previousButtonSlot, nextButtonSlot=$nextButtonSlot, pageIndicatorSlot=$pageIndicatorSlot, area=$area, currentPages=$currentPages, startSlot=$startSlot, itemsHeight=$itemsHeight, pageSize=$pageSize, calculatedPreviousButtonSlot=$calculatedPreviousButtonSlot, calculatedPageIndicatorSlot=$calculatedPageIndicatorSlot, calculatedNextButtonSlot=$calculatedNextButtonSlot, previousButtonComponent=$previousButtonComponent, nextButtonComponent=$nextButtonComponent, pageIndicatorComponent=$pageIndicatorComponent) ${super.toString()}"
    }
}
