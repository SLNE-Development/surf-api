@file:Suppress("UnstableApiUsage")

package dev.slne.surf.surfapi.bukkit.api.dialog.builder

import dev.slne.surf.surfapi.bukkit.api.dialog.base
import dev.slne.surf.surfapi.bukkit.api.dialog.dialog
import dev.slne.surf.surfapi.bukkit.api.dialog.type
import dev.slne.surf.surfapi.core.api.messages.adventure.buildText
import dev.slne.surf.surfapi.core.api.messages.adventure.text
import io.papermc.paper.registry.data.dialog.ActionButton
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import net.kyori.adventure.text.Component

private const val PAGINATION_BUTTON_DEFAULT_WIDTH = 50

fun <T> PaginatedDialog(
    block: DialogPaginationBuilder<T>.() -> Unit,
) = DialogPaginationBuilder<T>().apply(block).build()

fun <T> paginatedDialog(
    block: DialogPaginationBuilder<T>.() -> Unit,
) = PaginatedDialog(block)

fun interface DialogPageAction {
    fun newPage(currentPage: Int, maxPages: Int): Int
}

private enum class DialogPaginationBaseAction(
    val label: (Int, Int) -> Component,
    val tooltip: (Int, Int) -> Component,
    val pageAction: DialogPageAction,
) {
    FIRST(
        label = { _, _ -> text("«") },
        tooltip = { _, _ -> buildText { info("Erste Seite") } },
        pageAction = { _, _ -> 0 }
    ),
    BACK(
        label = { _, _ -> text("‹") },
        tooltip = { _, _ -> buildText { info("Vorherige Seite") } },
        pageAction = { currentPage, _ -> (currentPage - 1).coerceAtLeast(0) }
    ),
    CURRENT(
        label = { currentPage, maxPages -> text("${currentPage + 1} / $maxPages") },
        tooltip = { currentPage, maxPages -> buildText { info("Seite ${currentPage + 1} von $maxPages") } },
        pageAction = { currentPage, _ -> currentPage },
    ),
    NEXT(
        label = { _, _ -> text("›") },
        tooltip = { _, _ -> buildText { info("Nächste Seite") } },
        pageAction = { currentPage, maxPages -> (currentPage + 1).coerceAtMost(maxPages - 1) }
    ),
    LAST(
        label = { _, _ -> text("»") },
        tooltip = { _, _ -> buildText { info("Letzte Seite") } },
        pageAction = { _, maxPages -> maxPages - 1 }
    );

    fun actionButton(
        currentPage: Int,
        maxPages: Int,
        width: Int,
    ) = actionButton {
        label(this@DialogPaginationBaseAction.label(currentPage, maxPages))
        tooltip(this@DialogPaginationBaseAction.tooltip(currentPage, maxPages))
        width(width)
    }
}

class DialogPaginationBuilder<T> {

    private var base: (DialogBaseBuilder.() -> Unit)? = null
    private var exitAction: ActionButton? = null
    private var buttonBuilder: (DialogActionButtonBuilder.(T) -> Unit)? = null

    private val elements = ObjectLinkedOpenHashSet<T>()
    private var elementsPerPage = 10
    private val currentPageElements
        get() = elements.drop(currentPage * elementsPerPage).take(elementsPerPage)

    private var paginationButtonWidth = PAGINATION_BUTTON_DEFAULT_WIDTH

    @Suppress("SuspiciousVarProperty")
    private var minElementButtonWidth = 200

    private fun calculateElementButtonWidth(): Int {
        var width = 0

        if (hasPreviousPage) {
            width += paginationButtonWidth * 2 // first + back
        }

        if (hasNextPage) {
            width += paginationButtonWidth * 2 // next + last
        }

        width += paginationButtonWidth // current page button

        return width.coerceAtLeast(minElementButtonWidth)
    }

    var currentPage = 0
        private set

    val maxPages: Int
        get() = (elements.size + elementsPerPage - 1) / elementsPerPage

    val hasNextPage: Boolean
        get() = currentPage < maxPages - 1

    val hasPreviousPage: Boolean
        get() = currentPage > 0

    private var firstPageButton = DialogPaginationBaseAction.FIRST.actionButton(
        currentPage, maxPages, paginationButtonWidth
    )

    private var backButton = DialogPaginationBaseAction.BACK.actionButton(
        currentPage, maxPages, paginationButtonWidth
    )

    private var currentPageButton = DialogPaginationBaseAction.CURRENT.actionButton(
        currentPage, maxPages, paginationButtonWidth
    )

    private var nextButton = DialogPaginationBaseAction.NEXT.actionButton(
        currentPage, maxPages, paginationButtonWidth
    )

    private var lastPageButton = DialogPaginationBaseAction.LAST.actionButton(
        currentPage, maxPages, paginationButtonWidth
    )

    fun base(block: DialogBaseBuilder.(Int, Int) -> Unit) {
        base = {
            block(currentPage, maxPages)
        }
    }

    private fun buildPageButtonAction(pageAction: DialogPageAction) = dialogAction {
        playerCallback { player ->
            val newPage = pageAction.newPage(currentPage, maxPages)

            if (newPage == currentPage) return@playerCallback

            player.showDialog(this@DialogPaginationBuilder.build())
        }
    }

    fun firstPageButton(
        block: DialogActionButtonBuilder.(Int, Int) -> Unit,
        pageAction: DialogPageAction = DialogPaginationBaseAction.FIRST.pageAction,
    ) {
        firstPageButton = actionButton {
            block(currentPage, maxPages)

            width(paginationButtonWidth)
            action(buildPageButtonAction(pageAction))
        }
    }

    fun backButton(
        block: DialogActionButtonBuilder.(Int, Int) -> Unit,
        pageAction: DialogPageAction = DialogPaginationBaseAction.BACK.pageAction,
    ) {
        backButton = actionButton {
            block(currentPage, maxPages)

            width(paginationButtonWidth)
            action(buildPageButtonAction(pageAction))
        }
    }

    fun currentPageButton(
        block: DialogActionButtonBuilder.(Int, Int) -> Unit,
        pageAction: DialogPageAction = DialogPaginationBaseAction.CURRENT.pageAction,
    ) {
        currentPageButton = actionButton {
            block(currentPage, maxPages)

            width(paginationButtonWidth)
            action(buildPageButtonAction(pageAction))
        }
    }

    fun nextButton(
        block: DialogActionButtonBuilder.(Int, Int) -> Unit,
        pageAction: DialogPageAction = DialogPaginationBaseAction.NEXT.pageAction,
    ) {
        nextButton = actionButton {
            block(currentPage, maxPages)

            width(paginationButtonWidth)
            action(buildPageButtonAction(pageAction))
        }
    }

    fun lastPageButton(
        block: DialogActionButtonBuilder.(Int, Int) -> Unit,
        pageAction: DialogPageAction = DialogPaginationBaseAction.LAST.pageAction,
    ) {
        lastPageButton = actionButton {
            block(currentPage, maxPages)

            width(paginationButtonWidth)
            action(buildPageButtonAction(pageAction))
        }
    }

    fun elementsPerPage(elementsPerPage: Int) {
        require(elementsPerPage > 0) { "elementsPerPage must be greater than 0" }

        this.elementsPerPage = elementsPerPage
    }

    fun addElement(element: T) {
        elements.add(element)
    }

    fun addElements(elements: Collection<T>) {
        this.elements.addAll(elements)
    }

    fun addElements(vararg elements: T) {
        this.elements.addAll(elements)
    }

    fun paginationButtonWidth(width: Int) {
        require(width in 1..1024) {
            "Pagination button width must be between 1 and 100"
        }

        paginationButtonWidth = width
    }

    fun minElementButtonWidth(width: Int) {
        require(width in 1..1024) {
            "Element button minimum width must be between 1 and 1024"
        }

        minElementButtonWidth = width
    }

    fun exitAction(block: DialogActionButtonBuilder.() -> Unit) {
        exitAction = actionButton(block)
    }

    fun buttonBuilder(builder: DialogActionButtonBuilder.(T) -> Unit) {
        buttonBuilder = builder
    }

    fun build() = dialog {
        val baseBuilder = base
        val buttonBuilder = buttonBuilder
        val exitAction = exitAction

        require(baseBuilder != null) {
            "Dialog base must be set before building the paginated dialog"
        }

        require(buttonBuilder != null) {
            "Dialog button builder must be set before building the paginated dialog"
        }

        base(baseBuilder)

        val elementButtonWidth = calculateElementButtonWidth()
        val elementButtons = currentPageElements.map {
            actionButton {
                buttonBuilder(it)

                width(elementButtonWidth)
            }
        }

        type {
            multiAction {
                columns(1)
                
                elementButtons.forEach { action(it) }

                if (hasPreviousPage) {
                    action(firstPageButton)
                    action(backButton)
                }

                action(currentPageButton)

                if (hasNextPage) {
                    action(nextButton)
                    action(lastPageButton)
                }

                if (exitAction != null) {
                    exitAction(exitAction)
                }
            }
        }
    }

}