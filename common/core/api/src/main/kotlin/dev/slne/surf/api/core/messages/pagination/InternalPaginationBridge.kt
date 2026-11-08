package dev.slne.surf.api.core.messages.pagination

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.shared.api.util.InternalSurfApi

private val instance = requiredService<InternalPaginationBridge>()

@InternalSurfApi
interface InternalPaginationBridge {
    fun <T> createPaginationBuilder(): PaginationBuilder<T>
    fun <T> createPaginationBuilderSuspend(): SuspendPaginationBuilder<T>

    companion object : InternalPaginationBridge by instance {
        val INSTANCE get() = instance
    }
}