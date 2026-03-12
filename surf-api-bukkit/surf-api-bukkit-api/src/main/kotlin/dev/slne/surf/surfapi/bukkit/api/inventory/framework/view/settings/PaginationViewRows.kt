package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings

enum class PaginationViewRows(val actualRows: ViewRows, val paginationContentRows: IntRange) {
    ONE(ViewRows.THREE, 1..1),
    TWO(ViewRows.FOUR, 1..2),
    THREE(ViewRows.FIVE, 1..3),
    FOUR(ViewRows.SIX, 1..4);
}