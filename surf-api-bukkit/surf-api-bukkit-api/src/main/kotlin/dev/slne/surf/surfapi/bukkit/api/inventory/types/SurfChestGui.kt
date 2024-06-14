package dev.slne.surf.surfapi.bukkit.api.inventory.types

import com.github.stefvanschie.inventoryframework.adventuresupport.ComponentHolder
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.gui.type.util.NamedGui
import dev.slne.surf.surfapi.bukkit.api.inventory.SurfGui
import net.kyori.adventure.text.Component
import org.jetbrains.annotations.Range

open class SurfChestGui(
    title: Component,
    rows: @Range(from = 2, to = 6) Int = 6,
    override val parent: SurfGui? = null
) :
    ChestGui(rows, ComponentHolder.of(title)), SurfGui {
    override val gui: NamedGui
        get() = this

    init {
        check(rows in 2..6) { "Rows must be between 2 and 6" }

        setOnBottomClick { event -> event.isCancelled = true }
        setOnBottomDrag { event -> event.isCancelled = true }
        setOnTopClick { event -> event.isCancelled = true }
        setOnTopDrag { event -> event.isCancelled = true }
    }
}