@file:JvmName("InventoryFrameworkExtensions")

package dev.slne.surf.surfapi.bukkit.api.inventory.framework

import dev.slne.surf.surfapi.bukkit.api.builder.buildItem
import dev.slne.surf.surfapi.bukkit.api.builder.displayName
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.InventoryFrameworkDSL
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.OpenContext
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player

/**
 * Registers this [View] with the active [ViewFrame].
 *
 * After registration, the view can be opened for players via [open].
 *
 * ```kotlin
 * class MyView : View() { ... }
 * MyView().register()
 * ```
 *
 * @receiver the [View] to register
 * @see unregister
 * @see viewFrame
 */
fun View.register() {
    viewFrame.with(this)
}

/**
 * Unregisters this [View] from the active [ViewFrame].
 *
 * After unregistration the view can no longer be opened for players and any
 * currently open instances may be closed by the framework.
 *
 * @receiver the [View] to unregister
 * @see register
 */
fun View.unregister() {
    viewFrame.remove(this)
}

/**
 * Opens this [View] for the given [player] without any initial data.
 *
 * ```kotlin
 * myView.open(player)
 * ```
 *
 * @receiver the [View] to open
 * @param player the [Player] who should see the view
 * @see open
 */
fun View.open(player: Player) {
    viewFrame.open(javaClass, player)
}

/**
 * Opens this [View] for the given [player] with arbitrary initial [data].
 *
 * The [data] is passed as the view's initial state and can be retrieved inside
 * lifecycle callbacks via the `OpenContext`.
 *
 * ```kotlin
 * myView.open(player, myProfileObject)
 * ```
 *
 * @receiver the [View] to open
 * @param player the [Player] who should see the view
 * @param data arbitrary data to associate with the view session
 * @see open
 */
fun View.open(player: Player, data: Any) {
    viewFrame.open(javaClass, player, data)
}

/**
 * Opens this [View] for each [Player] in [players] without any initial data.
 *
 * ```kotlin
 * myView.open(onlinePlayers)
 * ```
 *
 * @receiver the [View] to open
 * @param players the collection of [Player]s to open the view for
 * @see open
 */
fun View.open(players: Collection<Player>) {
    viewFrame.open(javaClass, players)
}

/**
 * Opens this [View] for each [Player] in [players] with shared initial [data].
 *
 * ```kotlin
 * myView.open(teamPlayers, teamData)
 * ```
 *
 * @receiver the [View] to open
 * @param players the collection of [Player]s to open the view for
 * @param data arbitrary shared data to associate with each view session
 * @see open
 */
fun View.open(players: Collection<Player>, data: Any) {
    viewFrame.open(javaClass, players, data)
}

/**
 * Configures the title of this [ViewConfigBuilder] using a [SurfComponentBuilder] DSL block.
 *
 * ```kotlin
 * override fun onInit(config: ViewConfigBuilder) {
 *     config.titleBuilder {
 *         text("My View")
 *         color(NamedTextColor.GOLD)
 *     }
 * }
 * ```
 *
 * @receiver the [ViewConfigBuilder] whose title is being set
 * @param title DSL block used to construct the title [net.kyori.adventure.text.Component]
 * @return this [ViewConfigBuilder] for chaining
 */
inline fun ViewConfigBuilder.titleBuilder(title: @InventoryFrameworkDSL SurfComponentBuilder.() -> Unit): ViewConfigBuilder =
    this.title(SurfComponentBuilder(title))

/**
 * Modifies the [ViewConfigBuilder] of this [OpenContext] using a DSL block.
 *
 * This is a convenience wrapper around [OpenContext.modifyConfig] that applies
 * the [modifier] block directly on the returned builder.
 *
 * ```kotlin
 * onOpen { open ->
 *     open.modifyConfig {
 *         titleBuilder { text("Dynamic Title") }
 *     }
 * }
 * ```
 *
 * @receiver the [OpenContext] whose config is being modified
 * @param modifier DSL block applied to the [ViewConfigBuilder]
 */
inline fun OpenContext.modifyConfig(modifier: @InventoryFrameworkDSL ViewConfigBuilder.() -> Unit) =
    this.modifyConfig().apply(modifier)

val View.outlineItem
    get() = buildItem(Material.GRAY_STAINED_GLASS_PANE) {
        displayName(Component.empty())
    }