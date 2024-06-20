package dev.slne.surf.surfapi.bukkit.api.inventory.types

import com.github.stefvanschie.inventoryframework.adventuresupport.ComponentHolder
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.gui.type.util.NamedGui
import dev.slne.surf.surfapi.bukkit.api.inventory.SinglePlayerGui
import dev.slne.surf.surfapi.bukkit.api.inventory.SurfGui
import dev.slne.surf.surfapi.bukkit.api.inventory.dsl.MenuMarker
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.jetbrains.annotations.Range

@MenuMarker
open class SurfChestGui internal constructor(
    title: Component,
    rows: @Range(from = 2, to = 6) Int = 6,
    override val parent: SurfGui? = null
) :
    ChestGui(rows, ComponentHolder.of(title)), SurfGui {
    override val gui: NamedGui
        get() = this

    init {
        check(rows in 2..6) { "Rows must be between 2 and 6" }

        this.setOnBottomClick { event -> event.isCancelled = true }
        this.setOnBottomDrag { event -> event.isCancelled = true }
        this.setOnTopClick { event -> event.isCancelled = true }
        this.setOnTopDrag { event -> event.isCancelled = true }
    }
}

@MenuMarker
class SurfChestSinglePlayerGui internal constructor(
    title: Component,
    override val player: Player,
    rows: @Range(from = 2, to = 6) Int = 6,
    override val parent: SurfGui? = null,
) :
    SurfChestGui(title, rows, parent), SinglePlayerGui