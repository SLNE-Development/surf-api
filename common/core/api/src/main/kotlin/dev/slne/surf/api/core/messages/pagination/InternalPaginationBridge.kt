@file:OptIn(InternalSurfApi::class)

package dev.slne.surf.api.core.messages.pagination

import dev.slne.surf.api.di.surfBean
import dev.slne.surf.api.shared.api.util.InternalSurfApi

private val instance by surfBean<InternalPaginationBridge>()

@InternalSurfApi
interface InternalPaginationBridge {
    fun <T> createPaginationBuilder(): PaginationBuilder<T>
    fun <T> createPaginationBuilderSuspend(): SuspendPaginationBuilder<T>

    companion object {
        val INSTANCE get() = instance
    }
}
