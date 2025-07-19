package dev.slne.surf.surfapi.core.api.messages.pagination

import dev.slne.surf.surfapi.core.api.util.InternalSurfApi
import dev.slne.surf.surfapi.core.api.util.requiredService

@InternalSurfApi
interface InternalPaginationBridge {
    fun <T> createPaginationBuilder(): PaginationBuilder<T>

    companion object {
        val instance = requiredService<InternalPaginationBridge>()
    }
}