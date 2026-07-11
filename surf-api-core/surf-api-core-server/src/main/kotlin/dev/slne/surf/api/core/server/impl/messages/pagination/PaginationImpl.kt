package dev.slne.surf.api.core.server.impl.messages.pagination

import dev.slne.surf.api.core.messages.pagination.*
import dev.slne.surf.api.core.util.freeze
import dev.slne.surf.api.core.util.mutableObjectListOf
import dev.slne.surf.api.core.util.objectListOf
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
        fun pages(pageSize: Int, count: Int): Int {
            require(pageSize > 0) { "pageSize must be greater than 0" }
            require(count >= 0) { "count must not be negative" }
            return if (count == 0) 0 else ((count - 1) / pageSize) + 1
        }

        inline fun <T> forEachPageEntry(
            content: Collection<T>,
            pageSize: Int,
            page: Int,
            consumer: (T, Int) -> Unit,
        ) {
            val size = content.size
            val start = (pageSize.toLong() * (page - 1L)).toInt()
            val end = minOf(size.toLong(), start.toLong() + pageSize).toInt()

            if (content is List<T> && content is RandomAccess) {
                for (i in start until end) {
                    consumer(content[i], i)
                }
            } else {
                val iterator = content.iterator()
                // Skip previous pages
                repeat(start) { iterator.next() }
                for (i in start until end) {
                    consumer(iterator.next(), i)
                }
            }
        }
    }
}
