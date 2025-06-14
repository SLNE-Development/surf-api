package dev.slne.surf.surfapi.bukkit.server.impl.inventory.gui

import dev.slne.surf.surfapi.bukkit.api.inventory.gui.NamedGui
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.Component

abstract class AbstractNamedGui(title: Component, parent: AbstractGui? = null) :
    AbstractGui(parent), NamedGui {

    protected var titleDirty = false

    override var title: Component = title
        set(value) {
            field = value
            titleDirty = true
        }

    override fun title(title: Component) {
        this.title = title
    }

    override fun title(builder: SurfComponentBuilder.() -> Unit) =
        title(SurfComponentBuilder(builder))
}