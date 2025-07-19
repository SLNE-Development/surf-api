package dev.slne.surf.surfapi.core.api.messages.pagination

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