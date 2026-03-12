package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.pagination

internal enum class PaginationButton(val column: Int) {
    LEFT(2),
    RIGHT(6);

    fun clickSlot(paginationRow: Int): Int {
        val effectiveRow = paginationRow - 1
        return column + effectiveRow * 9
    }
}