package dev.slne.surf.surfapi.bukkit.test.gui

import dev.slne.surf.surfapi.bukkit.api.gui.GuiItem
import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.context.RenderContext
import dev.slne.surf.surfapi.bukkit.api.gui.dsl.component
import dev.slne.surf.surfapi.bukkit.api.gui.dsl.dynamicComponent
import dev.slne.surf.surfapi.bukkit.api.gui.props.Prop
import dev.slne.surf.surfapi.bukkit.api.gui.ref.Ref
import dev.slne.surf.surfapi.bukkit.api.gui.view.ViewConfig
import dev.slne.surf.surfapi.bukkit.server.gui.view.BukkitGuiView
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryType

/**
 * Example GUI demonstrating MutableProp usage with a counter.
 * The counter is shared across all viewers (global state).
 */
object CounterGuiView : BukkitGuiView() {
    // Global counter state (shared across all viewers)
    private val counterProp = Prop.Mutable("counter", 0)
    private val counterDisplayRef = Ref<dev.slne.surf.surfapi.bukkit.api.gui.component.Component>()

    override fun onInit(config: ViewConfig) {
        config.type = InventoryType.CHEST
        config.rows = 3
        config.title = Component.text("Counter GUI", NamedTextColor.GOLD, TextDecoration.BOLD)
    }

    override fun onFirstRender(context: RenderContext) {
        // Counter display at top center
        context.renderComponent(Slot.at(4, 0), dynamicComponent(
            renderer = { ctx ->
                val count = runBlocking { counterProp.get() } ?: 0
                GuiItem.of(Material.DIAMOND).copyWith(
                    displayName = Component.text("Counter: $count", NamedTextColor.AQUA, TextDecoration.BOLD),
                    lore = listOf(
                        Component.text("This is a global counter", NamedTextColor.GRAY),
                        Component.text("shared across all viewers", NamedTextColor.GRAY)
                    )
                )
            }
        ) {
            ref = counterDisplayRef
        })

        // Increment +1 button
        context.renderComponent(Slot.at(2, 1), component(
            GuiItem.of(Material.LIME_CONCRETE).copyWith(
                displayName = Component.text("+1", NamedTextColor.GREEN, TextDecoration.BOLD),
                lore = listOf(Component.text("Click to increment by 1", NamedTextColor.GRAY))
            )
        ) {
            onClick = {
                val current = runBlocking { counterProp.get() } ?: 0
                counterProp.set(current + 1)
                counterDisplayRef.update()
            }
        })

        // Increment +10 button
        context.renderComponent(Slot.at(3, 1), component(
            GuiItem.of(Material.GREEN_CONCRETE).copyWith(
                displayName = Component.text("+10", NamedTextColor.GREEN, TextDecoration.BOLD),
                lore = listOf(Component.text("Click to increment by 10", NamedTextColor.GRAY))
            )
        ) {
            onClick = {
                val current = runBlocking { counterProp.get() } ?: 0
                counterProp.set(current + 10)
                counterDisplayRef.update()
            }
        })

        // Decrement -1 button
        context.renderComponent(Slot.at(5, 1), component(
            GuiItem.of(Material.ORANGE_CONCRETE).copyWith(
                displayName = Component.text("-1", NamedTextColor.RED, TextDecoration.BOLD),
                lore = listOf(Component.text("Click to decrement by 1", NamedTextColor.GRAY))
            )
        ) {
            onClick = {
                val current = runBlocking { counterProp.get() } ?: 0
                counterProp.set(current - 1)
                counterDisplayRef.update()
            }
        })

        // Decrement -10 button
        context.renderComponent(Slot.at(6, 1), component(
            GuiItem.of(Material.RED_CONCRETE).copyWith(
                displayName = Component.text("-10", NamedTextColor.RED, TextDecoration.BOLD),
                lore = listOf(Component.text("Click to decrement by 10", NamedTextColor.GRAY))
            )
        ) {
            onClick = {
                val current = runBlocking { counterProp.get() } ?: 0
                counterProp.set(current - 10)
                counterDisplayRef.update()
            }
        })

        // Reset button
        context.renderComponent(Slot.at(4, 2), component(
            GuiItem.of(Material.BARRIER).copyWith(
                displayName = Component.text("Reset", NamedTextColor.YELLOW, TextDecoration.BOLD),
                lore = listOf(Component.text("Click to reset counter to 0", NamedTextColor.GRAY))
            )
        ) {
            onClick = {
                counterProp.set(0)
                counterDisplayRef.update()
            }
        })
    }
}
