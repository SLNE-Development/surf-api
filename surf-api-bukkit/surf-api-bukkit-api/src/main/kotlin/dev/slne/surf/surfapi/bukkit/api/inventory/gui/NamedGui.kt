package dev.slne.surf.surfapi.bukkit.api.inventory.gui

import dev.slne.surf.surfapi.bukkit.api.inventory.dsl.GuiDsl
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.Component

@GuiDsl
interface NamedGui : Gui {
    val title: Component

    fun title(title: Component)
    fun title(builder: SurfComponentBuilder.() -> Unit)

    override fun clone(): NamedGui
}