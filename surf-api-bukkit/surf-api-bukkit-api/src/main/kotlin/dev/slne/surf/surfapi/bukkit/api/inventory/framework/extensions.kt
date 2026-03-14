@file:JvmName("InventoryFrameworkExtensions")

package dev.slne.surf.surfapi.bukkit.api.inventory.framework

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.InventoryFramworkDSL
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.OpenContext
import org.bukkit.entity.Player

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

inline fun ViewConfigBuilder.titleBuilder(title: @InventoryFramworkDSL SurfComponentBuilder.() -> Unit): ViewConfigBuilder =
    this.title(SurfComponentBuilder(title))

inline fun OpenContext.modifyConfig(modifier: @InventoryFramworkDSL ViewConfigBuilder.() -> Unit) =
    this.modifyConfig().apply(modifier)