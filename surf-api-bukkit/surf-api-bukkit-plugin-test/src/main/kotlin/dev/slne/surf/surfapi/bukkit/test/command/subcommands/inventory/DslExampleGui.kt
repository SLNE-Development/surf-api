package dev.slne.surf.surfapi.bukkit.test.command.subcommands.inventory

import dev.slne.surf.surfapi.bukkit.api.builder.buildItem
import dev.slne.surf.surfapi.bukkit.api.builder.displayName
import dev.slne.surf.surfapi.bukkit.server.inventory.openGui
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player

/**
 * Example GUI using Kotlin DSL approach.
 * This demonstrates the fluent DSL API for creating GUIs quickly.
 */
suspend fun Player.openDslExampleGui() {
    var clickCount = 0

    openGui {
        title = Component.text("DSL-Based GUI Example", NamedTextColor.GOLD)
        rows = 3

        // Add border
        border(buildItem(Material.GRAY_STAINED_GLASS_PANE) {
            displayName { text("") }
        })

        // Add diamond item in center
        item(13, buildItem(Material.DIAMOND) {
            displayName {
                text("Click me!", NamedTextColor.AQUA)
            }
        }) {
            onClick { player, _ ->
                clickCount++
                player.sendMessage(
                    Component.text(
                        "You clicked the diamond! Total clicks: $clickCount",
                        NamedTextColor.GREEN
                    )
                )
            }
        }

        // Add emerald item
        item(11, buildItem(Material.EMERALD) {
            displayName {
                text("Emerald", NamedTextColor.GREEN)
            }
        }) {
            onClick { player, _ ->
                player.sendMessage(
                    Component.text(
                        "You clicked the emerald!",
                        NamedTextColor.GREEN
                    )
                )
            }
        }

        // Add gold item
        item(15, buildItem(Material.GOLD_INGOT) {
            displayName {
                text("Gold", NamedTextColor.YELLOW)
            }
        }) {
            onClick { player, _ ->
                player.sendMessage(
                    Component.text(
                        "You clicked the gold!",
                        NamedTextColor.YELLOW
                    )
                )
            }
        }

        // Add close button
        item(26, buildItem(Material.BARRIER) {
            displayName {
                text("Close", NamedTextColor.RED)
            }
        }) {
            onClick { player, _ ->
                player.closeInventory()
            }
        }
    }
}
