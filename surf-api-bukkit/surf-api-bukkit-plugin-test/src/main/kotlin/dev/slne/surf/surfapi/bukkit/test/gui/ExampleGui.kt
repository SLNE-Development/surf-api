package dev.slne.surf.surfapi.bukkit.test.gui

import dev.slne.surf.surfapi.bukkit.api.builder.ItemStack
import dev.slne.surf.surfapi.bukkit.api.builder.displayName
import dev.slne.surf.surfapi.bukkit.api.extensions.server
import dev.slne.surf.surfapi.bukkit.api.inventory.dsl.menu
import dev.slne.surf.surfapi.bukkit.api.inventory.dsl.paginatedPane
import dev.slne.surf.surfapi.bukkit.api.inventory.dsl.pagingButtons
import dev.slne.surf.surfapi.bukkit.api.inventory.dsl.staticPane
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.slot
import dev.slne.surf.surfapi.bukkit.test.BukkitPluginMain
import dev.slne.surf.surfapi.core.api.messages.adventure.sendText
import dev.slne.surf.surfapi.core.api.messages.adventure.text
import dev.slne.surf.surfapi.core.api.util.toObjectList
import org.bukkit.Material

fun exampleGui() = menu(text("Test")) {
    onGlobalDrag { it.isCancelled = true }
    onGlobalClick { it.isCancelled = true }

    staticPane(slot(1, 0), 1) {
        updatableItem(slot(0, 0), ItemStack(Material.BARRIER) {
            displayName {
                primary("Tolles Item")
            }
        }) {
            onClick {
                it.whoClicked.sendText {
                    text("You clicked the test item!")
                }
            }

            onUpdate {
                visible = !visible
                true
            }
        }

        item(slot(1, 0), ItemStack(Material.DIAMOND) {
            displayName {
                primary("Diamond")
            }
        }) {
            onClick {
                it.whoClicked.sendText {
                    text("You clicked the diamond!")
                }
            }
        }
    }

    val paginatedPane = paginatedPane(slot(0, 1), 5) {
        val items = (1..500).map { index ->
            ItemStack(Material.STONE) {
                displayName {
                    primary("Stone Item $index")
                }
            }
        }.toObjectList()

        populateWithItemStacks(items)
    }

    pagingButtons(slot(0, 0), paginatedPane) {
        setBackwardsButton(ItemStack(Material.RED_CONCRETE) {
            displayName {
                primary("Previous Page")
            }
        })

        setForwardsButton(ItemStack(Material.GREEN_CONCRETE) {
            displayName {
                primary("Next Page")
            }
        })
    }

    server.scheduler.runTaskTimer(BukkitPluginMain.getInstance(), Runnable {
//        update()
    }, 20, 20)
}