package dev.slne.surf.surfapi.core.server.impl.messages.pagination

import dev.slne.surf.surfapi.core.api.messages.pagination.*
import net.kyori.adventure.text.Component

class PaginationBuilderImpl<T> : PaginationBuilder<T> {
    override var width: Int = Pagination.DEFAULT_WIDTH
        set(value) {
            require(value >= 3) { "Width must be at least 3" }
            field = value
        }

    override var indent: Int = Pagination.DEFAULT_INDENT
        set(value) {
            require(value >= 0) { "Indent must be at least 0" }
            field = value
        }

    override var resultsPerPage: Int = Pagination.DEFAULT_RESULTS_PER_PAGE
        set(value) {
            require(value > 0) { "Results per page must be greater than 0" }
            field = value
        }


    override var renderer: PaginationRenderer = PaginationRenderer.DEFAULT

    private var _title: Component? = null
    override var title: Component
        get() = _title ?: error("Title must be set before building")
        set(value) {
            _title = value
        }

    private var _rowRenderer: PaginationRowRenderer<T>? = null
    override var rowRenderer: PaginationRowRenderer<T>
        get() = _rowRenderer ?: error("Row renderer must be set before building")
        set(value) {
            _rowRenderer = value
        }

    override var clickEventProvider: PaginationClickEventProvider<T> =
        PaginationClickEventProvider.default()

    override var firstPageButton: PageButton = Pagination.DEFAULT_FIRST_PAGE_BUTTON
    override var previousPageButton: PageButton = Pagination.DEFAULT_PREVIOUS_PAGE_BUTTON
    override var nextPageButton: PageButton = Pagination.DEFAULT_NEXT_PAGE_BUTTON
    override var lastPageButton: PageButton = Pagination.DEFAULT_LAST_PAGE_BUTTON

    override fun build(): Pagination<T> = PaginationImpl(
        width = width,
        indent = indent,
        resultsPerPage = resultsPerPage,
        renderer = renderer,
        title = title,
        rowRenderer = rowRenderer,
        firstPageButton = firstPageButton,
        previousPageButton = previousPageButton,
        nextPageButton = nextPageButton,
        lastPageButton = lastPageButton,
        clickEventProvider = clickEventProvider
    )

    override fun toString(): String {
        return "PaginationBuilderImpl(width=$width, indent=$indent, resultsPerPage=$resultsPerPage, renderer=$renderer, _title=$_title, _rowRenderer=$_rowRenderer, clickEventProvider=$clickEventProvider, firstPageButton=$firstPageButton, previousPageButton=$previousPageButton, nextPageButton=$nextPageButton, lastPageButton=$lastPageButton)"
    }
}