package dev.slne.surf.surfapi.core.server.impl.messages.pagination.suspend

import dev.slne.surf.surfapi.core.api.messages.pagination.*
import dev.slne.surf.surfapi.core.api.util.freeze
import dev.slne.surf.surfapi.core.api.util.logger
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import dev.slne.surf.surfapi.core.api.util.objectListOf
import dev.slne.surf.surfapi.core.server.impl.messages.pagination.PaginationImpl
import dev.slne.surf.surfapi.core.server.impl.messages.pagination.PaginationImpl.Companion.pages
import kotlinx.coroutines.*
import net.kyori.adventure.text.Component

data class SuspendPaginationImpl<T>(
    private val width: Int,
    private val resultsPerPage: Int,
    private val indent: Int,
    private val renderer: SuspendPaginationRenderer,
    private val title: Component,
    private val rowRenderer: SuspendPaginationRowRenderer<T>,
    private val firstPageButton: PageButton,
    private val previousPageButton: PageButton,
    private val nextPageButton: PageButton,
    private val lastPageButton: PageButton,
    private val clickEventProvider: SuspendPaginationClickEventProvider<T>,
) : SuspendPagination<T> {

    companion object {
        private val log = logger()
        private val renderContext = Dispatchers.Default +
                CoroutineName("SuspendPagination-Rendering") +
                CoroutineExceptionHandler { context, throwable ->
                    log.atSevere()
                        .withCause(throwable)
                        .log("Failed to render pagination")
                }

    }

    override suspend fun render(
        content: Collection<T>,
        page: Int
    ): List<Component> = withContext(renderContext) {
        if (content.isEmpty()) {
            return@withContext renderer.run { objectListOf(renderEmpty()) }
        }

        val pages = pages(resultsPerPage, content.size)

        if (page !in 1..pages) {
            return@withContext renderer.run { objectListOf(renderUnknownPage(page, pages)) }
        }

        val results = mutableObjectListOf<Deferred<Collection<Component>>>()
        coroutineScope {
            results.add(async { listOf(renderer.run { renderHeader(width, indent, title, page, pages) }) })

            PaginationImpl.forEachPageEntry(content, resultsPerPage, page) { value, index ->
                val renderedComponents = renderer.run {
                    async {
                        renderRow(
                            width,
                            indent,
                            page,
                            pages,
                            value,
                            index,
                            rowRenderer
                        )
                    }
                }
                results.add(renderedComponents)
            }

            results.add(async {
                renderer.run {
                    listOf(
                        renderFooter(
                            width,
                            indent,
                            page,
                            pages,
                            firstPageButton,
                            previousPageButton,
                            nextPageButton,
                            lastPageButton
                        ) { page -> clickEventProvider.run { getCallback(page, this@SuspendPaginationImpl, content) } })
                }
            })
        }

        val finalResults = mutableObjectListOf<Component>()
        results.forEach { finalResults.addAll(it.await()) }

        finalResults.freeze()
    }
}