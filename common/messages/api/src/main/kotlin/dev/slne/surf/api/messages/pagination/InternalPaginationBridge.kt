package dev.slne.surf.api.messages.pagination

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.spring.surfBean

@InternalSurfApi
interface InternalPaginationBridge {
    fun <T> createPaginationBuilder(): PaginationBuilder<T>
    fun <T> createPaginationBuilderSuspend(): SuspendPaginationBuilder<T>

    @InternalSurfApi
    companion object {
        internal val instance by surfBean<InternalPaginationBridge>()
    }
}
