@file:JvmName("InventoryExtensions")

package dev.slne.surf.surfapi.bukkit.server.inventory

import dev.slne.surf.surfapi.bukkit.api.inventory.component.GuiComponent
import dev.slne.surf.surfapi.bukkit.api.inventory.dsl.GuiBuilder
import dev.slne.surf.surfapi.bukkit.api.inventory.dsl.gui
import dev.slne.surf.surfapi.bukkit.server.inventory.component.GuiComponentFactoryImpl
import org.bukkit.entity.Player

/**
 * The global GUI component factory.
 */
val guiFactory = GuiComponentFactoryImpl

/**
 * Creates a GUI using Kotlin DSL.
 */
fun createGui(block: GuiBuilder.() -> Unit): GuiComponent {
    return gui(guiFactory, block)
}

/**
 * Creates and opens a GUI using Kotlin DSL.
 */
suspend fun Player.openGui(block: GuiBuilder.() -> Unit) {
    val gui = createGui(block)
    gui.onMount()
    gui.open(this)
}
