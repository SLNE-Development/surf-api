package dev.slne.surf.surfapi.core.server.impl.messages.pagination

import dev.slne.surf.surfapi.core.api.messages.pagination.*
import dev.slne.surf.surfapi.core.api.util.freeze
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import dev.slne.surf.surfapi.core.api.util.objectListOf
import net.kyori.adventure.text.Component

data class PaginationImpl<T>(
    private val width: Int,
    private val resultsPerPage: Int,
    private val indent: Int,
    private val renderer: PaginationRenderer,
    private val title: Component,
    private val rowRenderer: PaginationRowRenderer<T>,
    private val firstPageButton: PageButton,
    private val previousPageButton: PageButton,
    private val nextPageButton: PageButton,
    private val lastPageButton: PageButton,
    private val clickEventProvider: PaginationClickEventProvider<T>,
) : Pagination<T> {

    override fun render(
        content: Collection<T>,
        page: Int,
    ): List<Component> {
        if (content.isEmpty()) {
            return objectListOf(renderer.renderEmpty())
        }

        val pages = pages(resultsPerPage, content.size)

        if (page !in 1..pages) {
            return objectListOf(renderer.renderUnknownPage(page, pages))
        }

        val results = mutableObjectListOf<Component>()
        results.add(renderer.renderHeader(width, indent, title, page, pages))

        forEachPageEntry(content, resultsPerPage, page) { value, index ->
            results.addAll(
                renderer.renderRow(
                    width,
                    indent,
                    page,
                    pages,
                    value,
                    index,
                    rowRenderer
                )
            )
        }

        results.add(
            renderer.renderFooter(
                width,
                indent,
                page,
                pages,
                firstPageButton,
                previousPageButton,
                nextPageButton,
                lastPageButton
            ) { page -> clickEventProvider.getCallback(page, this, content) }
        )

        return results.freeze()
    }

    companion object {
        fun pages(pageSize: Int, count: Int): Int = (count + pageSize - 1) / pageSize

        inline fun <T> forEachPageEntry(
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
    }
}
