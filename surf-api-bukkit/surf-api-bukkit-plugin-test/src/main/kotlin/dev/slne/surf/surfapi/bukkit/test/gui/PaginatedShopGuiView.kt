package dev.slne.surf.surfapi.bukkit.test.gui

import dev.slne.surf.surfapi.bukkit.api.builder.ItemStack
import dev.slne.surf.surfapi.bukkit.api.builder.displayName
import dev.slne.surf.surfapi.bukkit.api.gui.GuiItem
import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import dev.slne.surf.surfapi.bukkit.api.gui.component.PaginationComponent
import dev.slne.surf.surfapi.bukkit.api.gui.context.RenderContext
import dev.slne.surf.surfapi.bukkit.api.gui.dsl.dynamicComponent
import dev.slne.surf.surfapi.bukkit.api.gui.props.ViewerProp
import dev.slne.surf.surfapi.bukkit.api.gui.ref.Ref
import dev.slne.surf.surfapi.bukkit.api.gui.view.AbstractGuiView
import dev.slne.surf.surfapi.bukkit.api.gui.view.ViewConfig
import dev.slne.surf.surfapi.core.api.messages.adventure.sendText
import dev.slne.surf.surfapi.core.api.messages.adventure.text
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack

/**
 * Example GUI demonstrating ViewerMutableProp and PaginationComponent.
 * Each player has their own coins (viewer-specific state).
 */
object PaginatedShopGuiView : AbstractGuiView() {
    private val coinsProp = ViewerProp.Mutable("coins", 1000)
    private val coinsDisplayRef = Ref<Component>()

    // Shop items
    private val shopItems = listOf(
        ShopItem(Material.DIAMOND_SWORD, "Diamond Sword", 100),
        ShopItem(Material.DIAMOND_PICKAXE, "Diamond Pickaxe", 80),
        ShopItem(Material.DIAMOND_AXE, "Diamond Axe", 75),
        ShopItem(Material.DIAMOND_SHOVEL, "Diamond Shovel", 70),
        ShopItem(Material.DIAMOND_HOE, "Diamond Hoe", 65),
        ShopItem(Material.IRON_SWORD, "Iron Sword", 50),
        ShopItem(Material.IRON_PICKAXE, "Iron Pickaxe", 40),
        ShopItem(Material.IRON_AXE, "Iron Axe", 35),
        ShopItem(Material.IRON_SHOVEL, "Iron Shovel", 30),
        ShopItem(Material.IRON_HOE, "Iron Hoe", 25),
        ShopItem(Material.GOLDEN_SWORD, "Golden Sword", 80),
        ShopItem(Material.GOLDEN_PICKAXE, "Golden Pickaxe", 70),
        ShopItem(Material.GOLDEN_AXE, "Golden Axe", 65),
        ShopItem(Material.GOLDEN_SHOVEL, "Golden Shovel", 60),
        ShopItem(Material.GOLDEN_HOE, "Golden Hoe", 55),
        ShopItem(Material.BOW, "Bow", 50),
        ShopItem(Material.CROSSBOW, "Crossbow", 75),
        ShopItem(Material.SHIELD, "Shield", 45),
        ShopItem(Material.FISHING_ROD, "Fishing Rod", 30),
        ShopItem(Material.SHEARS, "Shears", 20),
        ShopItem(Material.FLINT_AND_STEEL, "Flint and Steel", 15),
        ShopItem(Material.COMPASS, "Compass", 25),
        ShopItem(Material.CLOCK, "Clock", 25),
        ShopItem(Material.SPYGLASS, "Spyglass", 40),
        ShopItem(Material.LEAD, "Lead", 20),
        ShopItem(Material.NAME_TAG, "Name Tag", 50),
        ShopItem(Material.SADDLE, "Saddle", 60),
        ShopItem(Material.ELYTRA, "Elytra", 500)
    )

    // Pagination component
    private val paginationComponent = PaginationComponent(
        items = { shopItems },
        pageSize = 28, // 4 rows of 7 items
        itemRenderer = { item, ctx ->
            GuiItem.of(ItemStack(item.material))
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
        context.renderComponent(
            Slot.at(4, 0), dynamicComponent(
                renderer = { ctx ->
                    val coins = coinsProp.get(ctx.player) ?: 0

                    GuiItem.of(ItemStack(Material.GOLD_INGOT) {
                        displayName {
                            gold("Coins: ")
                            yellow(coins)
                        }
                    })
                }
            ) {
                ref = coinsDisplayRef
            })

        // Render pagination component at rows 1-4 (slots 9-44)
        context.renderComponent(Slot.of(9), paginationComponent)

        // Previous page button
        context.renderComponent(
            Slot.at(2, 5),
            PaginationComponent.buildPreviousPageComponent(paginationComponent)
        )

        // Page indicator
        context.renderComponent(
            Slot.at(4, 5),
            PaginationComponent.buildPageIndicatorComponent(paginationComponent)
        )

        // Next page button
        context.renderComponent(
            Slot.at(6, 5),
            PaginationComponent.buildNextPageComponent(paginationComponent)
        )
    }

    private data class ShopItem(
        val material: Material,
        val name: String,
        val price: Int
    )
}
