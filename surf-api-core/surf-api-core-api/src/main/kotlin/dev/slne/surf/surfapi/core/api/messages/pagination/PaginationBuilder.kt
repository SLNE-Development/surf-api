package dev.slne.surf.surfapi.core.api.messages.pagination

import net.kyori.adventure.text.Component

class PaginationBuilder {
    private var width: Int = Pagination.DEFAULT_WIDTH
    private var resultsPerPage: Int = Pagination.DEFAULT_RESULTS_PER_PAGE

    private var renderer: PaginationRenderer = PaginationRenderer.DEFAULT

    private var title: Component = Component.text("CHANGE ME")



}