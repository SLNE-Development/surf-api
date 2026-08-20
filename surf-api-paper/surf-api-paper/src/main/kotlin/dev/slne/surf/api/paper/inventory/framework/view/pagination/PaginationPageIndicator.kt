package dev.slne.surf.api.paper.inventory.framework.view.pagination

import dev.slne.surf.api.core.messages.adventure.text
import net.kyori.adventure.text.Component

/**
 * Renders the page counter that a paginated view shows between its navigation buttons.
 *
 * Called every time the pagination state changes, with the one-based number of the page currently
 * shown and the number of pages in total. Returning `null` hides the counter for that state — for
 * example to show nothing while there is only a single page.
 *
 * ```kotlin
 * paginatedSurfView("Shop") {
 *     settings {
 *         paginationPageIndicator { current, total ->
 *             if (total <= 1) null else text("Seite $current von $total", Colors.GRAY)
 *         }
 *     }
 * }
 * ```
 *
 * @see Default
 * @see dev.slne.surf.api.paper.inventory.framework.view.settings.PaginatedViewSettings.paginationPageIndicator
 */
fun interface PaginationPageIndicator {

    /**
     * Returns the component to render as the page counter, or `null` to render nothing.
     *
     * @param currentPage the one-based number of the page currently shown
     * @param totalPages the total number of pages
     */
    fun render(currentPage: Int, totalPages: Int): Component?

    companion object {
        /** Renders the counter as `current/total`, e.g. `2/7`. */
        val Default = PaginationPageIndicator { currentPage, totalPages ->
            text("$currentPage/$totalPages")
        }
    }
}
