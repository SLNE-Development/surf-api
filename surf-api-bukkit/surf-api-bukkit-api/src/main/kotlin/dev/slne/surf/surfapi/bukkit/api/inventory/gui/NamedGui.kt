package dev.slne.surf.surfapi.bukkit.api.inventory.gui

import net.kyori.adventure.text.Component

abstract class NamedGui internal constructor(
    title: Component,
    parent: Gui? = null,
) : Gui(parent) {

    var title: Component = Component.empty()
        set(value) {
            field = value
            titleDirty = true
        }

    protected var titleDirty = false

    init {
        this.title = title
    }
}