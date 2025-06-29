package dev.slne.surf.surfapi.core.api.messages.pagination

import dev.slne.surf.surfapi.core.api.util.freeze
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent

data class PaginationImpl<T>(
    private val width: Int,
    private val resultsPerPage: Int,
    private val renderer: PaginationRenderer,
    private val title: Component,
    private val rowRenderer: PaginationRowRenderer<T>,
    private val firstPageButton: PageButton = Pagination.DEFAULT_FIRST_PAGE_BUTTON,
    private val previousPageButton: PageButton = Pagination.DEFAULT_PREVIOUS_PAGE_BUTTON,
    private val nextPageButton: PageButton = Pagination.DEFAULT_NEXT_PAGE_BUTTON,
    private val lastPageButton: PageButton = Pagination.DEFAULT_LAST_PAGE_BUTTON,
) : Pagination<T> {
    override fun render(
        content: Collection<T>,
        page: Int,
    ): List<Component> {
        if (content.isEmpty()) {
            return listOf(renderer.renderEmpty())
        }

        val pages = pages(resultsPerPage, content.size)

        if (page !in 1..pages) {
            return listOf(renderer.renderUnknownPage(page, pages))
        }

        val results = mutableObjectListOf<Component>()
        results.add(renderer.renderHeader(width, title, page, pages))

        forEachPageEntry(content, resultsPerPage, page) { value, index ->
            results.addAll(rowRenderer.renderRow(value, index))
        }
        results.add(
            renderer.renderFooter(
                width,
                page,
                pages,
                firstPageButton,
                previousPageButton,
                nextPageButton,
                lastPageButton
            ) { page ->
                ClickEvent.callback { clicker ->
                    clicker.sendMessage(
                        renderComponent(
                            content,
                            page
                        )
                    )
                }
            }
        )

        return results.freeze()
    }

    companion object {
        private fun pages(pageSize: Int, count: Int): Int = (count + pageSize - 1) / pageSize
    }
}

private fun <T> forEachPageEntry(
    content: Collection<T>,
    pageSize: Int,
    page: Int,
    consumer: (T, Int) -> Unit,
) {
    val size = content.size
    val start = pageSize * (page - 1)
    val end = pageSize * page

    if (content is List<T> && content is RandomAccess) {
        for (i in start until end.coerceAtMost(size)) {
            consumer(content[i], i)
        }
    } else {
        val iterator = content.iterator()
        // Skip previous pages
        repeat(start) { iterator.next() }
        for (i in start until end.coerceAtMost(size)) {
            consumer(iterator.next(), i)
        }
    }
}
