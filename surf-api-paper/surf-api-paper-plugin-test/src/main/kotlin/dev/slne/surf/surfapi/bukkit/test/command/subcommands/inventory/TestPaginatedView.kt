package dev.slne.surf.surfapi.bukkit.test.command.subcommands.inventory

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.adventure.text
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.api.paper.inventory.framework.dsl.withItem
import dev.slne.surf.api.paper.inventory.framework.view.container.dsl.header
import dev.slne.surf.api.paper.inventory.framework.view.container.dsl.rowText
import dev.slne.surf.api.paper.inventory.framework.view.containerDefaults
import dev.slne.surf.api.paper.inventory.framework.view.layoutTarget
import dev.slne.surf.api.paper.inventory.framework.view.paginatedSurfView
import dev.slne.surf.api.paper.inventory.framework.view.pagination.pagination
import dev.slne.surf.api.paper.inventory.framework.view.settings
import dev.slne.surf.api.paper.inventory.framework.view.settings.PaginationViewRows
import dev.slne.surf.api.paper.inventory.framework.view.settings.align.TextAlignment
import org.bukkit.inventory.ItemType

/** Genug Einträge für mehrere Seiten, damit die Navigations-Buttons beide Zustände zeigen. */
private val testPaginationEntries = List(40) { index -> "Eintrag ${index + 1}" }

val testPaginatedViewDsl = paginatedSurfView("Test Pagination") {
    settings {
        paginationViewRows(PaginationViewRows.THREE)
        paginationButtonsAtBottom()
    }

    layoutTarget('I')

    containerDefaults {
        header { darkSpacer("Paginated Inventory Test") }

        rowText(
            1,
            text("${testPaginationEntries.size} Einträge", Colors.GRAY),
            TextAlignment.RIGHT,
            columns = 5..8
        )
    }

    pagination<String> {
        source { testPaginationEntries }
        itemFactory { entry ->
            withItem(ItemType.PAPER) {
                displayName { text(entry) }
            }
        }
    }
}
