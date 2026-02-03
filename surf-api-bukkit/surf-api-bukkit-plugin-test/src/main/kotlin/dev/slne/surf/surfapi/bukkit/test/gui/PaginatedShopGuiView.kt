package dev.slne.surf.surfapi.bukkit.test.gui

import dev.slne.surf.surfapi.bukkit.api.builder.ItemStack
import dev.slne.surf.surfapi.bukkit.api.builder.buildLore
import dev.slne.surf.surfapi.bukkit.api.builder.displayName
import dev.slne.surf.surfapi.bukkit.api.gui.GuiItem
import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import dev.slne.surf.surfapi.bukkit.api.gui.component.PaginationComponent
import dev.slne.surf.surfapi.bukkit.api.gui.context.RenderContext
import dev.slne.surf.surfapi.bukkit.api.gui.dsl.component
import dev.slne.surf.surfapi.bukkit.api.gui.dsl.dynamicComponent
import dev.slne.surf.surfapi.bukkit.api.gui.dsl.slot
import dev.slne.surf.surfapi.bukkit.api.gui.props.ViewerProp
import dev.slne.surf.surfapi.bukkit.api.gui.ref.Ref
import dev.slne.surf.surfapi.bukkit.api.gui.view.AbstractGuiView
import dev.slne.surf.surfapi.bukkit.api.gui.view.ViewConfig
import dev.slne.surf.surfapi.core.api.messages.adventure.sendText
import dev.slne.surf.surfapi.core.api.messages.adventure.text
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryType

/**
 * Example GUI demonstrating ViewerMutableProp and PaginationComponent.
 * Each player has their own coins (viewer-specific state).
 */
object PaginatedShopGuiView : AbstractGuiView() {
    private val coinsProp = ViewerProp.Mutable("coins", 1000)
    private val coinsDisplayRef = Ref<Component>()

    // Shop items
    private val shopItems = Material.entries.filter {
        it.isItem && !it.isAir
    }.map { ShopItem(it, it.name, (10..500).random()) }.shuffled()

    // Pagination component - 4 rows of 7 items (columns 1-7, rows 1-4)
    private val paginationComponent = PaginationComponent(
        startSlot = Slot.at(1, 1),  // Column 1, Row 1
        endSlot = Slot.at(7, 4),     // Column 7, Row 4 (28 slots total)
        items = { shopItems },
        itemRenderer = { item, ctx ->
            GuiItem.of(ItemStack(item.material) {
                displayName {
                    gold(item.name)
                }
                buildLore {
                    line {
                        gray("Price: ")
                        yellow("${item.price} coins")
                    }
                    line {
                        gray("Click to purchase")
                    }
                }
            })
        },
        onItemClick = { item, ctx ->
            val coins = coinsProp.get(ctx.player)
            if (coins != null && coins >= item.price) {
                coinsProp.set(ctx.player, coins - item.price)
                ctx.player.sendText {
                    success("Purchased ")
                    variableValue(item.name)
                    success(" for ")
                    gold("${item.price} coins")
                }
                coinsDisplayRef.update()
            } else {
                ctx.player.sendText {
                    error("You do not have enough coins to purchase ")
                    variableValue(item.name)
                    error(".")
                }
            }
        }
    )

    override fun onInit(config: ViewConfig) {
        config.type = InventoryType.CHEST
        config.rows = 6
        config.title = text("Shop")
    }

    override fun onFirstRender(context: RenderContext) {
        // Coins display at top center
        context.slot(
            dynamicComponent(
                Slot.at(4, 0),
                renderer = { ctx ->
                    val coins = coinsProp.get(ctx.player) ?: 0

                    GuiItem.of(ItemStack(Material.GOLD_INGOT) {
                        displayName {
                            gold("Coins: ")
                            yellow("$coins")
                        }
                    })
                }
            ) {
                ref = coinsDisplayRef
            })

        // Render pagination component
        context.slot(paginationComponent)

        // Previous page button
        context.slot(
            component(
                Slot.at(2, 5),
                GuiItem.of(ItemStack(Material.ARROW) {
                    displayName {
                        info("Previous Page")
                    }
                    buildLore {
                        line {
                            gray("Click to go to the previous page")
                        }
                    }
                })
            ) {
                onClick = {
                    paginationComponent.previousPage(player)
                    view.update()
                }
            })

        // Page indicator
        context.slot(
            dynamicComponent(
                Slot.at(4, 5),
                renderer = { ctx ->
                    val currentPage = paginationComponent.getCurrentPage(ctx.player) + 1
                    val totalPages = paginationComponent.getTotalPages()

                    GuiItem.of(ItemStack(Material.PAPER) {
                        displayName {
                            info("Page $currentPage / $totalPages")
                        }
                    })
                }
            ))

        // Next page button
        context.slot(
            component(
                Slot.at(6, 5),
                GuiItem.of(ItemStack(Material.ARROW) {
                    displayName {
                        info("Next Page")
                    }
                    buildLore {
                        line {
                            gray("Click to go to the next page")
                        }
                    }
                })
            ) {
                onClick = {
                    paginationComponent.nextPage(player)
                    view.update()
                }
            })
    }

    private data class ShopItem(
        val material: Material,
        val name: String,
        val price: Int
    )
}
