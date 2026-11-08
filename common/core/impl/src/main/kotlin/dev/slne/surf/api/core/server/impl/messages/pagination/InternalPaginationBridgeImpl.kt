package dev.slne.surf.api.core.server.impl.messages.pagination

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.messages.pagination.InternalPaginationBridge
import dev.slne.surf.api.core.messages.pagination.PaginationBuilder
import dev.slne.surf.api.core.messages.pagination.SuspendPaginationBuilder
import dev.slne.surf.api.core.server.impl.messages.pagination.suspend.SuspendPaginationBuilderImpl

@AutoService(InternalPaginationBridge::class)
class InternalPaginationBridgeImpl : InternalPaginationBridge {
    override fun <T> createPaginationBuilder(): PaginationBuilder<T> {
        return PaginationBuilderImpl()
    }

    override fun <T> createPaginationBuilderSuspend(): SuspendPaginationBuilder<T> {
        return SuspendPaginationBuilderImpl()
    }
}