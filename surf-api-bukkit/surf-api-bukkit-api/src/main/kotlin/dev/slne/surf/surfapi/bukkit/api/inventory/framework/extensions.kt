@file:JvmName("InventoryFrameworkExtensions")

package dev.slne.surf.surfapi.bukkit.api.inventory.framework

import dev.slne.surf.surfapi.bukkit.api.builder.ItemDsl
import dev.slne.surf.surfapi.bukkit.api.builder.buildItem
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder
import me.devnatan.inventoryframework.context.OpenContext
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType

fun View.register() {
    viewFrame.with(this)
}

fun View.unregister() {
    viewFrame.remove(this)
}

fun View.open(player: Player) {
    viewFrame.open(javaClass, player)
}

fun View.open(player: Player, data: Any) {
    viewFrame.open(javaClass, player, data)
}

fun View.open(players: Collection<Player>) {
    viewFrame.open(javaClass, players)
}

fun View.open(players: Collection<Player>, data: Any) {
    viewFrame.open(javaClass, players, data)
}

inline fun ViewConfigBuilder.titleBuilder(title: SurfComponentBuilder.() -> Unit): ViewConfigBuilder =
    this.title(SurfComponentBuilder(title))

inline fun OpenContext.modifyConfig(modifier: ViewConfigBuilder.() -> Unit) =
    this.modifyConfig().apply(modifier)

inline fun BukkitItemComponentBuilder.withItem(
    type: ItemType,
    amount: Int = 1,
    init: (@ItemDsl ItemStack).() -> Unit
): BukkitItemComponentBuilder = this.withItem(buildItem(type, amount, init))

inline fun BukkitItemComponentBuilder.withItem(
    material: Material,
    amount: Int = 1,
    init: (@ItemDsl ItemStack).() -> Unit
): BukkitItemComponentBuilder = this.withItem(buildItem(material, amount, init))