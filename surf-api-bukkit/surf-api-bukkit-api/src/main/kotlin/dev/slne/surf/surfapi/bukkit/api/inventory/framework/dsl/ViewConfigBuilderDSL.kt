package dev.slne.surf.surfapi.bukkit.api.inventory.framework.dsl

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.InventoryFramworkDSL
import me.devnatan.inventoryframework.ViewConfigBuilder

/**
 * A DSL builder for composing inventory layout strings row by row.
 *
 * Each call to [row] (or the [unaryPlus] operator) appends a 9-character string that
 * describes one row of the inventory layout. The characters in the string correspond to
 * named slots: whitespace `' '` marks an unregistered slot, while any other character
 * can be referenced in [RenderContext][me.devnatan.inventoryframework.context.RenderContext]
 * via `layoutSlot(char)`.
 *
 * ```kotlin
 * override fun onInit(config: ViewConfigBuilder) {
 *     config.layout {
 *         +"XXXXXXXXX"
 *         +"X       X"
 *         +"X       X"
 *         +"XXXXXXXXX"
 *     }
 * }
 * // Then in render:
 * onFirstRender {
 *     layoutSlot('X') { withItem(Material.STONE) }
 * }
 * ```
 *
 * @see layout
 */
@InventoryFramworkDSL
class LayoutBuilder @PublishedApi internal constructor() {
    @PublishedApi
    internal val rows = mutableListOf<String>()

    /**
     * Appends a row to the layout with the given [pattern].
     *
     * The pattern must be exactly **9 characters** long (one per column).
     *
     * @param pattern a 9-character string representing one inventory row
     * @throws IllegalArgumentException if [pattern] is not exactly 9 characters
     */
    fun row(pattern: String) {
        require(pattern.length == 9) {
            "Layout row must be exactly 9 characters, got ${pattern.length}: \"$pattern\""
        }
        rows.add(pattern)
    }

    /**
     * Shorthand operator for [row].
     *
     * ```kotlin
     * layout {
     *     +"XXXXXXXXX"
     *     +"X       X"
     * }
     * ```
     *
     * @receiver the 9-character row pattern to append
     */
    operator fun String.unaryPlus() {
        row(this)
    }

    /**
     * Appends a row where every column is set to the given [char].
     *
     * ```kotlin
     * layout { fill('X') } // produces "XXXXXXXXX"
     * ```
     *
     * @param char the character to fill all 9 columns with
     */
    fun fill(char: Char) {
        row(char.toString().repeat(9))
    }

    /**
     * Appends an empty row (all 9 columns are spaces).
     *
     * Empty rows are not mapped to any named layout slot.
     */
    fun empty() {
        row(" ".repeat(9))
    }

    /**
     * Appends a border row: [char] in the first and last columns, spaces in between.
     *
     * ```kotlin
     * layout { border('B') } // produces "B       B"
     * ```
     *
     * @param char the character placed in the first and last columns
     */
    fun border(char: Char) {
        row("$char${" ".repeat(7)}$char")
    }

    @PublishedApi
    internal fun build(): Array<String> {
        return rows.toTypedArray()
    }
}

/**
 * Configures the inventory layout using a [LayoutBuilder] DSL block.
 *
 * Builds a layout pattern from the rows added in [block] and passes them to
 * [ViewConfigBuilder.layout].
 *
 * ```kotlin
 * override fun onInit(config: ViewConfigBuilder) {
 *     config.layout {
 *         +"XXXXXXXXX"
 *         +"X       X"
 *         +"XXXXXXXXX"
 *     }
 * }
 * ```
 *
 * @receiver the [ViewConfigBuilder] whose layout is being configured
 * @param block configuration block applied to a new [LayoutBuilder]
 * @see LayoutBuilder
 */
inline fun ViewConfigBuilder.layout(block: @InventoryFramworkDSL LayoutBuilder.() -> Unit) {
    layout(*LayoutBuilder().apply(block).build())
}