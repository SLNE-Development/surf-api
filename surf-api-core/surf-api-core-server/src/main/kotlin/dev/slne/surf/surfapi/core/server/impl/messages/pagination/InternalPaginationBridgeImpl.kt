package dev.slne.surf.surfapi.core.server.impl.messages.pagination

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.core.api.messages.pagination.InternalPaginationBridge
import dev.slne.surf.surfapi.core.api.messages.pagination.PaginationBuilder
import dev.slne.surf.surfapi.core.api.messages.pagination.SuspendPaginationBuilder
import dev.slne.surf.surfapi.core.server.impl.messages.pagination.suspend.SuspendPaginationBuilderImpl

@AutoService(InternalPaginationBridge::class)
class InternalPaginationBridgeImpl : InternalPaginationBridge {
    override fun <T> createPaginationBuilder(): PaginationBuilder<T> {
        return PaginationBuilderImpl()
    }

    override fun <T> createPaginationBuilderSuspend(): SuspendPaginationBuilder<T> {
        return SuspendPaginationBuilderImpl()
    }
}