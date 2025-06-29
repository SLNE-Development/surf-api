package dev.slne.surf.surfapi.core.api.messages.pagination

import net.kyori.adventure.text.Component

fun interface PaginationRowRenderer<T> {
    fun renderRow(value: T?, index: Int): Collection<Component>
}
