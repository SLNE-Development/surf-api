package dev.slne.surf.surfapi.core.api.messages.pagination

import kotlinx.coroutines.CoroutineScope
import net.kyori.adventure.text.Component

fun interface PaginationRowRenderer<T> {
    fun renderRow(value: T, index: Int): Collection<Component>

    fun interface Simple<T> : PaginationRowRenderer<T> {
        fun render(value: T): Component
        override fun renderRow(value: T, index: Int): Collection<Component> = listOf(render(value))
    }
}

fun interface SuspendPaginationRowRenderer<T> {
    suspend fun CoroutineScope.renderRow(value: T, index: Int): Collection<Component>

    fun interface Simple<T> : SuspendPaginationRowRenderer<T> {
        suspend fun CoroutineScope.render(value: T): Component
        override suspend fun CoroutineScope.renderRow(value: T, index: Int): Collection<Component> =
            listOf(render(value))
    }
}