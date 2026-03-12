package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings

import dev.slne.surf.surfapi.core.api.util.freeze
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.intellij.lang.annotations.MagicConstant

enum class ViewRows(val glyph: Char) {
    ONE('ꐓ'),
    TWO('ꐔ'),
    THREE('ꐭ'),
    FOUR('ꐮ'),
    FIVE('ꐯ'),
    SIX('ꐰ');

    val rows: Int = ordinal + 1

    companion object {
        @MagicConstant(intValues = [1, 2, 3, 4, 5, 6])
        annotation class Rows

        private val index = entries.associateByTo(Int2ObjectOpenHashMap()) { it.rows }.freeze()

        fun byRowsOrNull(@Rows rows: Int): ViewRows? = index.get(rows)
        fun byRows(@Rows rows: Int): ViewRows = byRowsOrNull(rows) ?: error("Invalid layout value: $rows")
    }
}
