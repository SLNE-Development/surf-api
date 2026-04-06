package dev.slne.surf.api.core.messages.pagination

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import org.jetbrains.annotations.Range
import kotlin.experimental.ExperimentalTypeInference

interface SuspendPaginationBuilder<T> {
    // required properties
    var title: Component
    var rowRenderer: SuspendPaginationRowRenderer<T>

    // optional properties
    var width: @Range(from = 3, to = Int.MAX_VALUE.toLong()) Int
    var indent: @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int
    var resultsPerPage: @Range(from = 1, to = Int.MAX_VALUE.toLong()) Int
    var renderer: SuspendPaginationRenderer
    var clickEventProvider: SuspendPaginationClickEventProvider<T>
    var firstPageButton: PageButton
    var previousPageButton: PageButton
    var nextPageButton: PageButton
    var lastPageButton: PageButton

    fun title(block: SurfComponentBuilder.() -> Unit) {
        title = SurfComponentBuilder(block)
    }

    fun rowRenderer(renderer: SuspendPaginationRowRenderer<T>) {
        rowRenderer = renderer
    }

    fun rowRendererSimple(renderer: SuspendPaginationRowRenderer.Simple<T>) {
        rowRenderer = renderer
    }

    fun clickEventProvider(provider: SuspendPaginationClickEventProvider<T>) {
        clickEventProvider = provider
    }

    fun firstPageButton(text: String, enabledStyle: Style, disabledStyle: Style) {
        firstPageButton = PageButton(text, enabledStyle, disabledStyle)
    }

    fun previousPageButton(text: String, enabledStyle: Style, disabledStyle: Style) {
        previousPageButton = PageButton(text, enabledStyle, disabledStyle)
    }

    fun nextPageButton(text: String, enabledStyle: Style, disabledStyle: Style) {
        nextPageButton = PageButton(text, enabledStyle, disabledStyle)
    }

    fun lastPageButton(text: String, enabledStyle: Style, disabledStyle: Style) {
        lastPageButton = PageButton(text, enabledStyle, disabledStyle)
    }

    fun build(): SuspendPagination<T>

    companion object {
        @Suppress("DEPRECATION")
        @OptIn(ExperimentalTypeInference::class)
        operator fun <T> invoke(@BuilderInference block: SuspendPaginationBuilder<T>.() -> Unit): SuspendPagination<T> {
            val builder = InternalPaginationBridge.createPaginationBuilderSuspend<T>()
            builder.block()
            return builder.build()
        }

        fun <T> builder(): SuspendPaginationBuilder<T> {
            return InternalPaginationBridge.createPaginationBuilderSuspend()
        }
    }
}

interface PaginationBuilder<T> {
    // required properties
    var title: Component
    var rowRenderer: PaginationRowRenderer<T>

    // optional properties
    var width: @Range(from = 3, to = Int.MAX_VALUE.toLong()) Int
    var indent: @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int
    var resultsPerPage: @Range(from = 1, to = Int.MAX_VALUE.toLong()) Int
    var renderer: PaginationRenderer
    var clickEventProvider: PaginationClickEventProvider<T>
    var firstPageButton: PageButton
    var previousPageButton: PageButton
    var nextPageButton: PageButton
    var lastPageButton: PageButton

    fun title(block: SurfComponentBuilder.() -> Unit) {
        title = SurfComponentBuilder(block)
    }

    fun rowRenderer(renderer: PaginationRowRenderer<T>) {
        rowRenderer = renderer
    }

    fun rowRendererSimple(renderer: PaginationRowRenderer.Simple<T>) {
        rowRenderer = renderer
    }

    fun clickEventProvider(provider: PaginationClickEventProvider<T>) {
        clickEventProvider = provider
    }

    fun firstPageButton(text: String, enabledStyle: Style, disabledStyle: Style) {
        firstPageButton = PageButton(text, enabledStyle, disabledStyle)
    }

    fun previousPageButton(text: String, enabledStyle: Style, disabledStyle: Style) {
        previousPageButton = PageButton(text, enabledStyle, disabledStyle)
    }

    fun nextPageButton(text: String, enabledStyle: Style, disabledStyle: Style) {
        nextPageButton = PageButton(text, enabledStyle, disabledStyle)
    }

    fun lastPageButton(text: String, enabledStyle: Style, disabledStyle: Style) {
        lastPageButton = PageButton(text, enabledStyle, disabledStyle)
    }

    fun build(): Pagination<T>

    companion object {
        @Suppress("DEPRECATION")
        @OptIn(ExperimentalTypeInference::class)
        operator fun <T> invoke(@BuilderInference block: PaginationBuilder<T>.() -> Unit): Pagination<T> {
            val builder = InternalPaginationBridge.createPaginationBuilder<T>()
            builder.block()
            return builder.build()
        }

        fun <T> builder(): PaginationBuilder<T> {
            return InternalPaginationBridge.createPaginationBuilder()
        }
    }
}