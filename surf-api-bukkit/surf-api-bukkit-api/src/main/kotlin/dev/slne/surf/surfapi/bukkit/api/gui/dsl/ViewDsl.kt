package dev.slne.surf.surfapi.bukkit.api.gui.dsl

import dev.slne.surf.surfapi.bukkit.api.gui.view.GuiView
import dev.slne.surf.surfapi.bukkit.api.gui.view.ViewConfig
import org.bukkit.event.inventory.InventoryType
import net.kyori.adventure.text.Component as AdventureComponent

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
    var type: InventoryType = InventoryType.CHEST
    var rows: Int
        get() = if (type == InventoryType.CHEST) size / 9 else 1
        set(value) {
            require(type == InventoryType.CHEST) {
                "Rows can only be set for CHEST type inventories. For other types, the size is determined by the inventory type."
            }
            require(value in 1..6) { "Rows must be between 1 and 6" }
            size = value * 9
        }
    var cancelOnClick: Boolean = true
    var closeOnClickOutside: Boolean = false

    internal fun build(): ViewConfig {
        return ViewConfig().apply {
            this.title = this@ViewConfigBuilder.title
            this.type = this@ViewConfigBuilder.type
            this.size = if (this.type == InventoryType.CHEST) {
                this@ViewConfigBuilder.size
            } else {
                this.type.defaultSize
            }
            this.cancelOnClick = this@ViewConfigBuilder.cancelOnClick
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
}
