package dev.slne.surf.api.core.messages.pagination

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.text.event.ClickEvent

fun interface PaginationClickEventProvider<T> {
    fun getCallback(targetPage: Int, pagination: Pagination<T>, content: Collection<T>): ClickEvent

    companion object {
        private object DEFAULT : PaginationClickEventProvider<Any> {
            override fun getCallback(
                targetPage: Int,
                pagination: Pagination<Any>,
                content: Collection<Any>,
            ): ClickEvent = ClickEvent.callback { clicker ->
                clicker.sendMessage(pagination.renderComponent(content, targetPage))
            }
        }

        fun <T> default(): PaginationClickEventProvider<T> {
            @Suppress("UNCHECKED_CAST")
            return DEFAULT as PaginationClickEventProvider<T>
        }
    }
}

fun interface SuspendPaginationClickEventProvider<T> {
    suspend fun CoroutineScope.getCallback(
        targetPage: Int,
        pagination: SuspendPagination<T>,
        content: Collection<T>
    ): ClickEvent

    companion object {
        private object DEFAULT : SuspendPaginationClickEventProvider<Any> {
            override suspend fun CoroutineScope.getCallback(
                targetPage: Int,
                pagination: SuspendPagination<Any>,
                content: Collection<Any>
            ): ClickEvent = ClickEvent.callback { clicker ->
                launch {
                    clicker.sendMessage(pagination.renderComponent(content, targetPage))
                }
            }
        }

        fun <T> default(): SuspendPaginationClickEventProvider<T> {
            @Suppress("UNCHECKED_CAST")
            return DEFAULT as SuspendPaginationClickEventProvider<T>
        }
    }
}