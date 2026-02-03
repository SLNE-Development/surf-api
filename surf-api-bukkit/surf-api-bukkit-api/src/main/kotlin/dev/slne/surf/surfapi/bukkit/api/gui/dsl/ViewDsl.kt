package dev.slne.surf.surfapi.bukkit.api.gui.dsl

import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import dev.slne.surf.surfapi.bukkit.api.gui.context.RenderContext
import dev.slne.surf.surfapi.bukkit.api.gui.view.GuiView
import dev.slne.surf.surfapi.bukkit.api.gui.view.ViewConfig
import net.kyori.adventure.text.Component as AdventureComponent
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * DSL marker for view building.
 */
@DslMarker
annotation class ViewDsl

/**
 * Builder for creating view configurations.
 */
@ViewDsl
class ViewConfigBuilder {
    var title: AdventureComponent = AdventureComponent.text("GUI")
    var size: Int = 54
    var rows: Int
        get() = size / 9
        set(value) {
            size = value * 9
        }
    var cancelOnClick: Boolean = true
    var closeOnClickOutside: Boolean = false
    
    internal fun build(): ViewConfig {
        return ViewConfig().apply {
            this.title = this@ViewConfigBuilder.title
            this.size = this@ViewConfigBuilder.size
            this.cancelOnClick = this@ViewConfigBuilder.cancelOnClick
            this.closeOnClickOutside = this@ViewConfigBuilder.closeOnClickOutside
        }
    }
}

/**
 * Configure a view using DSL.
 */
@ViewDsl
fun GuiView.configure(builder: ViewConfigBuilder.() -> Unit) {
    val configBuilder = ViewConfigBuilder()
    configBuilder.builder()
    val builtConfig = configBuilder.build()
    
    config.title = builtConfig.title
    config.size = builtConfig.size
    config.cancelOnClick = builtConfig.cancelOnClick
    config.closeOnClickOutside = builtConfig.closeOnClickOutside
}

/**
 * Fill a range of slots with the same component.
 */
@ComponentDsl
fun RenderContext.fillSlots(slots: IntRange, component: Component) {
    slots.forEach { slot ->
        renderComponent(slot, component)
    }
}

/**
 * Fill a range of slots with the same item.
 */
@ComponentDsl
fun RenderContext.fillSlots(slots: IntRange, item: ItemStack) {
    slots.forEach { slot ->
        setItem(slot, item)
    }
}

/**
 * Fill a row with items.
 */
@ComponentDsl
fun RenderContext.fillRow(row: Int, component: Component) {
    val startSlot = row * 9
    fillSlots(startSlot until startSlot + 9, component)
}

/**
 * Fill a row with items.
 */
@ComponentDsl
fun RenderContext.fillRow(row: Int, item: ItemStack) {
    val startSlot = row * 9
    fillSlots(startSlot until startSlot + 9, item)
}

/**
 * Fill borders with a component.
 */
@ComponentDsl
fun RenderContext.fillBorders(component: Component) {
    val rows = view.config.size / 9
    
    // Top and bottom rows
    fillRow(0, component)
    fillRow(rows - 1, component)
    
    // Left and right columns
    for (row in 1 until rows - 1) {
        renderComponent(row * 9, component)
        renderComponent(row * 9 + 8, component)
    }
}

/**
 * Fill borders with an item.
 */
@ComponentDsl
fun RenderContext.fillBorders(item: ItemStack) {
    val rows = view.config.size / 9
    
    // Top and bottom rows
    fillRow(0, item)
    fillRow(rows - 1, item)
    
    // Left and right columns
    for (row in 1 until rows - 1) {
        setItem(row * 9, item)
        setItem(row * 9 + 8, item)
    }
}

/**
 * Create a glass pane item (commonly used for borders/fillers).
 */
fun glassPane(material: Material = Material.GRAY_STAINED_GLASS_PANE): ItemStack {
    return ItemStack.of(material)
}

/**
 * Place component at row and column position.
 */
@ComponentDsl
fun RenderContext.slotAt(row: Int, column: Int, component: Component) {
    val slot = row * 9 + column
    renderComponent(slot, component)
}

/**
 * Place item at row and column position.
 */
@ComponentDsl
fun RenderContext.slotAt(row: Int, column: Int, item: ItemStack) {
    val slot = row * 9 + column
    setItem(slot, item)
}

/**
 * Center a component horizontally in a row.
 */
@ComponentDsl
fun RenderContext.centerInRow(row: Int, component: Component) {
    val slot = row * 9 + 4
    renderComponent(slot, component)
}

/**
 * Place multiple components in a centered pattern.
 */
@ComponentDsl
fun RenderContext.centerComponents(row: Int, components: List<Component>) {
    val centerOffset = 4 - ((components.size - 1) / 2)
    components.forEachIndexed { index, component ->
        val slot = row * 9 + centerOffset + index
        renderComponent(slot, component)
    }
}
