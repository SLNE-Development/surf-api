package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.ViewRows.Companion.byRows
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.ViewRows.Companion.byRowsOrNull
import dev.slne.surf.surfapi.core.api.util.freeze
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.intellij.lang.annotations.MagicConstant

/**
 * Represents the number of rows in a chest-type inventory view, mapped to special-font glyphs
 * for the background texture.
 *
 * Each entry corresponds to one, two, three, four, five, or six inventory rows.
 * The [rows] property returns the integer row count (1–6), and [glyph] is the Unicode character
 * in the resource-pack font that renders the matching background texture.
 *
 * Use [byRows] or [byRowsOrNull] to look up a [ViewRows] by its row count, or read [rows]
 * to get the integer count from an existing entry.
 *
 * @property glyph the Unicode glyph character for the background texture of this row count
 * @see SurfViewSettings
 */
enum class ViewRows(val glyph: Char) {
    /** 1-row inventory. */
    ONE('ꐓ'),

    /** 2-row inventory. */
    TWO('ꐔ'),

    /** 3-row inventory. */
    THREE('ꐭ'),

    /** 4-row inventory. */
    FOUR('ꐮ'),

    /** 5-row inventory. */
    FIVE('ꐯ'),

    /** 6-row inventory. */
    SIX('ꐰ');

    /**
     * The integer row count for this entry (1 for [ONE], 2 for [TWO], …, 6 for [SIX]).
     * Derived from [ordinal] so it always matches the enum declaration order.
     */
    val rows: Int = ordinal + 1

    companion object {
        /**
         * Annotation to mark parameters that should be in the range 1..6.
         */
        @MagicConstant(intValues = [1, 2, 3, 4, 5, 6])
        annotation class Rows

        private val index = entries.associateByTo(Int2ObjectOpenHashMap()) { it.rows }.freeze()

        /**
         * Returns the [ViewRows] for the given [rows] count, or `null` if not in the range 1..6.
         *
         * @param rows the row count to look up (1..6)
         * @return the matching [ViewRows], or `null`
         */
        fun byRowsOrNull(@Rows rows: Int): ViewRows? = index.get(rows)

        /**
         * Returns the [ViewRows] for the given [rows] count.
         *
         * @param rows the row count to look up (1..6)
         * @return the matching [ViewRows]
         * @throws IllegalStateException if [rows] is not in the range 1..6
         */
        fun byRows(@Rows rows: Int): ViewRows = byRowsOrNull(rows) ?: error("Invalid layout value: $rows")
    }
}
