package dev.slne.surf.surfapi.bukkit.api.inventory.gui.handlers

import org.bukkit.event.Event
import org.jetbrains.annotations.ApiStatus.OverrideOnly

interface GuiHandler<E : Event> {
    @OverrideOnly
    fun handle(event: E)
}