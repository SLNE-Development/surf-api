package dev.slne.surf.surfapi.bukkit.api.inventory.gui.handlers

import dev.slne.surf.surfapi.bukkit.api.inventory.dsl.GuiDsl
import org.bukkit.event.Event
import org.jetbrains.annotations.ApiStatus.OverrideOnly

@GuiDsl
interface GuiHandler<E : Event> {
    @OverrideOnly
    fun handle(event: E)
}