package dev.slne.surf.surfapi.bukkit.api.inventory.pane

import dev.slne.surf.surfapi.bukkit.api.inventory.gui.handlers.ClickHandler
import dev.slne.surf.surfapi.bukkit.api.inventory.gui.handlers.ClickHandlerDsl
import dev.slne.surf.surfapi.bukkit.api.inventory.item.GuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.Priority
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.Slot
import it.unimi.dsi.fastutil.objects.ObjectList
import org.jetbrains.annotations.Unmodifiable
import java.util.*

interface Pane : Cloneable {
    var slot: Slot
    val uuid: UUID
    val items: @Unmodifiable ObjectList<out GuiItem>
    val panes: @Unmodifiable ObjectList<out Pane>

    var length: Int
    var height: Int
    var priority: Priority
    var visible: Boolean

    fun onClick(handler: ClickHandler)
    fun onClick(handler: ClickHandlerDsl)

    fun clear()

    public override fun clone(): Pane
}