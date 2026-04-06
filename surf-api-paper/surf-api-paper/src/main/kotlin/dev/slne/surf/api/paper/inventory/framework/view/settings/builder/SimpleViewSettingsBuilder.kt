package dev.slne.surf.api.paper.inventory.framework.view.settings.builder

import dev.slne.surf.api.paper.inventory.framework.view.InventoryFrameworkDSL
import dev.slne.surf.api.paper.inventory.framework.view.settings.SimpleViewSettings
import dev.slne.surf.api.paper.inventory.framework.view.settings.SurfViewSettingsDefaults
import dev.slne.surf.api.paper.inventory.framework.view.settings.ViewRows

/**
 * DSL builder for [SimpleViewSettings].
 *
 * Extends [SurfViewSettingsBuilder] with a [rows] property that controls the number of
 * inventory rows displayed in the view.
 *
 * Create instances via [simpleViewSettings] or the `settings { }` DSL function in a
 * [surfView][dev.slne.surf.api.paper.api.inventory.framework.view.surfView] block.
 *
 * ```kotlin
 * surfView("My View") {
 *     settings {
 *         rows(ViewRows.FOUR)
 *         cancelAllInteractions()
 *     }
 * }
 * ```
 *
 * @see simpleViewSettings
 * @see SurfViewSettingsBuilder
 * @see SimpleViewSettings
 */
@InventoryFrameworkDSL
class SimpleViewSettingsBuilder @PublishedApi internal constructor() : SurfViewSettingsBuilder() {
    /**
     * The number of rows for the inventory.
     * Defaults to [SurfViewSettingsDefaults.DEFAULT_VIEW_ROWS].
     */
    var rows: ViewRows = SurfViewSettingsDefaults.DEFAULT_VIEW_ROWS
        private set

    /**
     * Sets the number of rows via a [ViewRows] value.
     *
     * @param rows the desired [ViewRows]
     */
    fun rows(rows: ViewRows) {
        this.rows = rows
    }

    /**
     * Sets the number of rows via an integer count.
     *
     * The integer is converted to a [ViewRows] via [ViewRows.Companion.byRows].
     *
     * @param count the row count (1..6)
     * @throws IllegalStateException if [count] is not in 1..6
     */
    fun rows(@ViewRows.Companion.Rows count: Int) {
        this.rows = ViewRows.byRows(count)
    }

    @PublishedApi
    override fun build(): SimpleViewSettings = SimpleViewSettings(
        font = font,
        headerTextAlignment = headerTextAlignment,
        cancelOnClick = cancelOnClick,
        cancelOnDrag = cancelOnDrag,
        cancelOnDrop = cancelOnDrop,
        cancelOnPickup = cancelOnPickup,
        navigateBackOnOutsideClick = navigateBackOnOutsideClick,
        rows = rows,
    )
}

/**
 * Creates a [SimpleViewSettings] instance using a [SimpleViewSettingsBuilder] DSL block.
 *
 * ```kotlin
 * val settings = simpleViewSettings {
 *     rows(ViewRows.THREE)
 *     cancelAllInteractions()
 *     navigateBackOnOutsideClick(false)
 * }
 * ```
 *
 * @param block configuration block applied to a [SimpleViewSettingsBuilder]
 * @return the built [SimpleViewSettings]
 */
inline fun simpleViewSettings(block: SimpleViewSettingsBuilder.() -> Unit): SimpleViewSettings =
    SimpleViewSettingsBuilder().apply(block).build()