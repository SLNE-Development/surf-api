package dev.slne.surf.surfapi.bukkit.test.command.subcommands.inventory

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.surfapi.bukkit.api.builder.buildItem
import dev.slne.surf.surfapi.bukkit.api.builder.displayName
import dev.slne.surf.surfapi.bukkit.api.inventory.component.AbstractGuiComponent
import dev.slne.surf.surfapi.bukkit.api.inventory.openGui
import dev.slne.surf.surfapi.bukkit.server.inventory.component.ItemComponentImpl
import dev.slne.surf.surfapi.bukkit.server.plugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

/**
 * Example GUI using class-based approach.
 * This demonstrates extending AbstractGuiComponent to create custom GUIs.
 */
class ClassBasedExampleGui(private val player: Player) : AbstractGuiComponent(
    title = Component.text("Class-Based GUI Example", NamedTextColor.GOLD),
    rows = 3
) {
    private var clickCount = 0

    override suspend fun setup() {
        // Add a diamond item that increments click count
        val diamondItem = ItemComponentImpl(
            slot = 13, // Center slot
            itemStack = buildItem(Material.DIAMOND) {
                displayName {
                    text("Click me! (Clicks: $clickCount)", NamedTextColor.AQUA)
                }
            },
            canTake = false,
            clickHandler = { p, _ ->
                clickCount++
                onDiamondClick(p)
            }
        )
        addItem(diamondItem)

        // Add a close button
        val closeButton = ItemComponentImpl(
            slot = 26, // Bottom right
            itemStack = buildItem(Material.BARRIER) {
                displayName {
                    text("Close", NamedTextColor.RED)
                }
            },
            canTake = false,
            clickHandler = { p, _ ->
                plugin.launch(p) {
                    close(p)
                }
            }
        )
        addItem(closeButton)

        // Add border items
        val borderSlots = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 19, 20, 21, 22, 23, 24, 25)
        borderSlots.forEach { slot ->
            val borderItem = ItemComponentImpl(
                slot = slot,
                itemStack = buildItem(Material.GRAY_STAINED_GLASS_PANE) {
                    displayName {
                        text("")
                    }
                },
                canTake = false
            )
            addItem(borderItem)
        }
    }

    private suspend fun onDiamondClick(player: Player) {
        // Update the diamond item with new click count
        player.sendMessage(Component.text("You clicked the diamond! Total clicks: $clickCount", NamedTextColor.GREEN))
        
        // Update the GUI
        updateFor(player)
    }

    override suspend fun onUpdate() {
        // Find and update the diamond item
        children.filterIsInstance<ItemComponentImpl>().find { it.slot == 13 }?.let { item ->
            val newItem = ItemComponentImpl(
                slot = 13,
                itemStack = buildItem(Material.DIAMOND) {
                    displayName {
                        text("Click me! (Clicks: $clickCount)", NamedTextColor.AQUA)
                    }
                },
                canTake = false,
                clickHandler = { p, _ ->
                    clickCount++
                    onDiamondClick(p)
                }
            )
            removeChild(item)
            addItem(newItem)
        }
    }
}

/**
 * Opens a class-based example GUI for a player.
 */
suspend fun Player.openClassBasedExampleGui() {
    val gui = ClassBasedExampleGui(this)
    gui.setup()
    openGui(gui)
}
