package dev.slne.surf.surfapi.bukkit.api.inventory.framework.dsl

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.InventoryFramworkDSL
import me.devnatan.inventoryframework.ViewConfigBuilder

@InventoryFramworkDSL
class LayoutBuilder @PublishedApi internal constructor() {
    @PublishedApi
    internal val rows = mutableListOf<String>()

    fun row(pattern: String) {
        require(pattern.length == 9) {
            "Layout row must be exactly 9 characters, got ${pattern.length}: \"$pattern\""
        }
        rows.add(pattern)
    }

    operator fun String.unaryPlus() {
        row(this)
    }

    fun fill(char: Char) {
        row(char.toString().repeat(9))
    }

    fun empty() {
        row(" ".repeat(9))
    }

    fun border(char: Char) {
        row("$char${" ".repeat(7)}$char")
    }

    @PublishedApi
    internal fun build(): Array<String> {
        return rows.toTypedArray()
    }
}

inline fun ViewConfigBuilder.layout(block: @InventoryFramworkDSL LayoutBuilder.() -> Unit) {
    layout(*LayoutBuilder().apply(block).build())
}