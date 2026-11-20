package dev.slne.surf.api.messages.pagination

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.kyori.adventure.text.event.ClickEvent

fun interface PaginationClickEventProvider<T> {
    fun getCallback(
        targetPage: Int,
        pagination: Pagination<T>,
        content: Collection<T>
    ): ClickEvent<*>

    companion object {
        private object DEFAULT : PaginationClickEventProvider<Any> {
            override fun getCallback(
                targetPage: Int,
                pagination: Pagination<Any>,
                content: Collection<Any>,
            ): ClickEvent<*> = ClickEvent.callback { clicker ->
                clicker.sendMessage(pagination.renderComponent(content, targetPage))
            }
        }

        fun <T> default(): PaginationClickEventProvider<T> {
            @Suppress("UNCHECKED_CAST")
            return DEFAULT as PaginationClickEventProvider<T>
        }
    }
}

/**
 * Builds the click event behind a page button of a suspending pagination.
 *
 * **The [CoroutineScope] receiver has to outlive the message, not just the render.** It is handed
 * over while the component is being built, and a click arrives whenever the player gets round to
 * it — possibly minutes later. A scope that ends when the render returns, which is what a command
 * executor's scope does, is already finished by then, and work launched on it is dropped. Pass a
 * scope that lives as long as the plugin.
 */
fun interface SuspendPaginationClickEventProvider<T> {
    suspend fun CoroutineScope.getCallback(
        targetPage: Int,
        pagination: SuspendPagination<T>,
        content: Collection<T>
    ): ClickEvent<*>

    companion object {
        private object DEFAULT : SuspendPaginationClickEventProvider<Any> {
            override suspend fun CoroutineScope.getCallback(
                targetPage: Int,
                pagination: SuspendPagination<Any>,
                content: Collection<Any>
            ): ClickEvent<*> = ClickEvent.callback { clicker ->
                check(isActive) {
                    "The pagination page button for page $targetPage cannot run: the " +
                            "CoroutineScope it was rendered with has already finished."
                }

                launch {
                    clicker.sendMessage(pagination.renderComponent(content, targetPage))
                }
            }
        }

        /**
         * Re-renders the target page when the button is clicked.
         *
         * Runs on the [CoroutineScope] it is given, which must outlive the message — see the
         * interface. It fails loudly rather than quietly when that scope has already finished.
         */
        fun <T> default(): SuspendPaginationClickEventProvider<T> {
            @Suppress("UNCHECKED_CAST")
            return DEFAULT as SuspendPaginationClickEventProvider<T>
        }
    }
}