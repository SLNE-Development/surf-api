package dev.slne.surf.surfapi.core.api.messages.pagination

import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.adventure.buildText
import dev.slne.surf.surfapi.core.api.messages.adventure.plain
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.TextDecoration

interface PaginationRenderer {

    fun renderEmpty(): Component = buildText {
        appendPrefix()
        info("Es wurden keine Ergebnisse gefunden.")
    }

    fun renderUnknownPage(page: Int, pages: Int): Component = buildText {
        appendPrefix()
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

    fun renderHeader(width: Int, title: Component, page: Int, pages: Int): Component = buildText {
        darkSpacer("*" + "-".repeat(width - 2) + "*", TextDecoration.STRIKETHROUGH)
        appendNewline {
            appendSpace()
            append(title.colorIfAbsent(Colors.PRIMARY))
            decorate(TextDecoration.BOLD)
        }
        appendNewline()
    }

    fun renderFooter(
        width: Int,
        page: Int,
        pages: Int,
        firstPage: PageButton,
        previousPage: PageButton,
        nextPage: PageButton,
        lastPage: PageButton,
        changePageEvent: (Int) -> ClickEvent?,
    ): Component = buildText {
        if (page == 1 && pages == 1) {
            appendNewline()
            darkSpacer("*" + "-".repeat(width - 2) + "*", TextDecoration.STRIKETHROUGH)
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

            val navLength = nav.plain().length
            val totalLen = width - 2 // minus the starting/ending *
            val sideLen = totalLen - navLength
            val left = sideLen / 2
            val right = sideLen - left

            darkSpacer("*" + "-".repeat(left), TextDecoration.STRIKETHROUGH)
            append(nav)
            darkSpacer("-".repeat(right) + "*", TextDecoration.STRIKETHROUGH)
        }
    }

    fun renderPreviousPageButton(
        button: PageButton,
        clickEvent: ClickEvent?,
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
        clickEvent: ClickEvent?,
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