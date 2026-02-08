package dev.slne.surf.surfapi.core.server.impl.messages.pagination.suspend

import dev.slne.surf.surfapi.core.api.messages.pagination.*
import net.kyori.adventure.text.Component

class SuspendPaginationBuilderImpl<T> : SuspendPaginationBuilder<T> {
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

    private var _title: Component? = null
    override var title: Component
        get() = _title ?: error("Title must be set before building")
        set(value) {
            _title = value
        }

    override var renderer: SuspendPaginationRenderer = SuspendPaginationRenderer.DEFAULT

    private var _rowRenderer: SuspendPaginationRowRenderer<T>? = null
    override var rowRenderer: SuspendPaginationRowRenderer<T>
        get() = _rowRenderer ?: error("Row renderer must be set before building")
        set(value) {
            _rowRenderer = value
        }

    override var clickEventProvider: SuspendPaginationClickEventProvider<T> =
        SuspendPaginationClickEventProvider.default()

    override var firstPageButton: PageButton = Pagination.DEFAULT_FIRST_PAGE_BUTTON
    override var previousPageButton: PageButton = Pagination.DEFAULT_PREVIOUS_PAGE_BUTTON
    override var nextPageButton: PageButton = Pagination.DEFAULT_NEXT_PAGE_BUTTON
    override var lastPageButton: PageButton = Pagination.DEFAULT_LAST_PAGE_BUTTON

    override fun build(): SuspendPagination<T> = SuspendPaginationImpl(
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
        return "SuspendPaginationBuilderImpl(width=$width, indent=$indent, resultsPerPage=$resultsPerPage, _title=$_title, _rowRenderer=$_rowRenderer, renderer=$renderer, clickEventProvider=$clickEventProvider, firstPageButton=$firstPageButton, previousPageButton=$previousPageButton, nextPageButton=$nextPageButton, lastPageButton=$lastPageButton)"
    }

}