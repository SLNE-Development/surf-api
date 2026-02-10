package dev.slne.surf.surfapi.core.api.messages.pagination

import dev.slne.surf.surfapi.core.api.util.requiredService
import dev.slne.surf.surfapi.shared.api.util.InternalSurfApi

@InternalSurfApi
interface InternalPaginationBridge {
    fun <T> createPaginationBuilder(): PaginationBuilder<T>
    fun <T> createPaginationBuilderSuspend(): SuspendPaginationBuilder<T>

    companion object {
        val instance = requiredService<InternalPaginationBridge>()
    }
}