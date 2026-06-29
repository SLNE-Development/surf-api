package dev.slne.surf.api.core.messages.pagination

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.DefaultFontInfo
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.plain
import dev.slne.surf.api.core.messages.adventure.text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.TextDecoration
import kotlin.math.roundToInt

interface SuspendPaginationRenderer {
    suspend fun CoroutineScope.renderEmpty(): Component = PaginationRenderer.DEFAULT.renderEmpty()
    suspend fun CoroutineScope.renderUnknownPage(page: Int, pages: Int): Component =
        PaginationRenderer.DEFAULT.renderUnknownPage(page, pages)

    suspend fun CoroutineScope.renderHeader(
        width: Int,
        indent: Int,
        title: Component,
        page: Int,
        pages: Int
    ): Component = PaginationRenderer.DEFAULT.renderHeader(width, indent, title, page, pages)

    suspend fun <T> CoroutineScope.renderRow(
        width: Int,
        indent: Int,
        page: Int,
        pages: Int,
        value: T,
        contentIndex: Int,
        renderer: SuspendPaginationRowRenderer<T>,
    ): Collection<Component> = renderer.run {
        renderRow(value, contentIndex)
            .map { text(" ".repeat(indent)).append(it) }
    }

    suspend fun CoroutineScope.renderFooter(
        width: Int,
        indent: Int,
        page: Int,
        pages: Int,
        firstPage: PageButton,
        previousPage: PageButton,
        nextPage: PageButton,
        lastPage: PageButton,
        changePageEvent: suspend CoroutineScope.(Int) -> ClickEvent<*>?,
    ): Component = buildText {
        if (page == 1 && pages == 1) {
            append(renderFooterSingle(width))
        } else {
            appendNewline()

            val nav = buildText {
                val first =
                    async { renderPreviousPageButton(firstPage, changePageEvent(1), page > 1) }
                val prev = async {
                    renderPreviousPageButton(
                        previousPage,
                        changePageEvent(page - 1),
                        page > 1
                    )
                }
                val next = async {
                    renderNextPageButton(
                        nextPage,
                        changePageEvent(page + 1),
                        page < pages
                    )
                }
                val last =
                    async { renderNextPageButton(lastPage, changePageEvent(pages), page < pages) }

                append(first.await())
                append(prev.await())
                append {
                    info(page)
                    spacer("/")
                    info(pages)
                }
                append(next.await())
                append(last.await())
            }

            append(renderFooterLine(width, nav))
        }
    }

    suspend fun CoroutineScope.renderPreviousPageButton(
        button: PageButton,
        clickEvent: ClickEvent<*>?,
        enabled: Boolean,
    ): Component = PaginationRenderer.DEFAULT.renderPreviousPageButton(button, clickEvent, enabled)

    suspend fun CoroutineScope.renderNextPageButton(
        button: PageButton,
        clickEvent: ClickEvent<*>?,
        enabled: Boolean,
    ): Component = PaginationRenderer.DEFAULT.renderNextPageButton(button, clickEvent, enabled)

    data object DEFAULT : SuspendPaginationRenderer
}

interface PaginationRenderer {

    fun renderEmpty(): Component = buildText {
        appendInfoPrefix()
        info("Es wurden keine Ergebnisse gefunden.")
    }

    fun renderUnknownPage(page: Int, pages: Int): Component = buildText {
        appendErrorPrefix()
        error("Unbekannte Seite: ")
        variableValue(page)
        error(". Es gibt nur ")
        variableValue(pages)
        if (pages == 1) {
            error(" Seite.")
        } else {
            error(" Seiten.")
        }
    }

    fun renderHeader(width: Int, indent: Int, title: Component, page: Int, pages: Int): Component =
        buildText {
            darkSpacer("*" + "-".repeat(width - 2) + "*", TextDecoration.STRIKETHROUGH)
            appendNewline {
                text(" ".repeat(indent))
                append(title.colorIfAbsent(Colors.PRIMARY))
                decorate(TextDecoration.BOLD)
            }
            appendNewline()
        }

    fun <T> renderRow(
        width: Int,
        indent: Int,
        page: Int,
        pages: Int,
        value: T,
        contentIndex: Int,
        renderer: PaginationRowRenderer<T>,
    ): Collection<Component> = renderer.renderRow(value, contentIndex)
        .map { text(" ".repeat(indent)).append(it) }

    fun renderFooter(
        width: Int,
        indent: Int,
        page: Int,
        pages: Int,
        firstPage: PageButton,
        previousPage: PageButton,
        nextPage: PageButton,
        lastPage: PageButton,
        changePageEvent: (Int) -> ClickEvent<*>?,
    ): Component = buildText {
        if (page == 1 && pages == 1) {
            append(renderFooterSingle(width))
        } else {
            appendNewline()

            val nav = buildText {
                append(renderPreviousPageButton(firstPage, changePageEvent(1), page > 1))
                append(renderPreviousPageButton(previousPage, changePageEvent(page - 1), page > 1))
                append {
                    info(page)
                    spacer("/")
                    info(pages)
                }
                append(renderNextPageButton(nextPage, changePageEvent(page + 1), page < pages))
                append(renderNextPageButton(lastPage, changePageEvent(pages), page < pages))
            }

            append(renderFooterLine(width, nav))
        }
    }

    fun renderPreviousPageButton(
        button: PageButton,
        clickEvent: ClickEvent<*>?,
        enabled: Boolean,
    ): Component = buildText {
        appendSpace()
        if (enabled) {
            append(Component.text(button.text, button.enabledStyle.clickEvent(clickEvent)))
        } else {
            append(Component.text(button.text, button.disabledStyle))
        }
        appendSpace()
    }

    fun renderNextPageButton(
        button: PageButton,
        clickEvent: ClickEvent<*>?,
        enabled: Boolean,
    ): Component = buildText {
        appendSpace()
        if (enabled) {
            append(Component.text(button.text, button.enabledStyle.clickEvent(clickEvent)))
        } else {
            append(Component.text(button.text, button.disabledStyle))
        }
        appendSpace()
    }

    data object DEFAULT : PaginationRenderer
}

private fun renderFooterSingle(width: Int): Component = buildText {
    appendNewline()
    darkSpacer("*" + "-".repeat(width - 2) + "*", TextDecoration.STRIKETHROUGH)
}

private fun renderFooterLine(width: Int, nav: Component): Component = buildText {
    val dashPx = DefaultFontInfo.MINUS.length + 1
    val navPx = DefaultFontInfo.pixelWidth(nav.plain())
    val linePx = (width - 2) * dashPx
    val sidePx = linePx - navPx
    val left = (sidePx / 2.0 / dashPx).roundToInt()
    val right = ((sidePx - left * dashPx.toDouble()) / dashPx).roundToInt()

    darkSpacer("*" + "-".repeat(left), TextDecoration.STRIKETHROUGH)
    append(nav)
    darkSpacer("-".repeat(right) + "*", TextDecoration.STRIKETHROUGH)
}