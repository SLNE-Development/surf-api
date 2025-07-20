package dev.slne.surf.surfapi.core.api.messages.pagination

import dev.slne.surf.surfapi.core.api.messages.Colors
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.Style
import kotlin.experimental.ExperimentalTypeInference


interface Pagination<T> {

    fun render(content: Collection<T>, page: Int = 1): List<Component>

    fun renderComponent(content: Collection<T>, page: Int = 1): Component {
        return Component.join(JoinConfiguration.newlines(), render(content, page))
    }

    companion object {
        const val DEFAULT_WIDTH = 33
        const val DEFAULT_INDENT = 2
        const val DEFAULT_RESULTS_PER_PAGE = 6

        val DEFAULT_FIRST_PAGE_BUTTON = PageButton(
            "[<<]",
            Style.style(
                Colors.SUCCESS,
                HoverEvent.showText(Component.text("Erste Seite", Colors.INFO))
            ),
            Style.style(Colors.GRAY)
        )

        val DEFAULT_PREVIOUS_PAGE_BUTTON = PageButton(
            "[<]",
            Style.style(
                Colors.SUCCESS,
                HoverEvent.showText(Component.text("Vorherige Seite", Colors.INFO))
            ),
            Style.style(Colors.GRAY)
        )

        val DEFAULT_NEXT_PAGE_BUTTON = PageButton(
            "[>]",
            Style.style(
                Colors.SUCCESS,
                HoverEvent.showText(Component.text("Nächste Seite", Colors.INFO))
            ),
            Style.style(Colors.GRAY)
        )

        val DEFAULT_LAST_PAGE_BUTTON = PageButton(
            "[>>]",
            Style.style(
                Colors.SUCCESS,
                HoverEvent.showText(Component.text("Letzte Seite", Colors.INFO))
            ),
            Style.style(Colors.GRAY)
        )

        @OptIn(ExperimentalTypeInference::class)
        operator fun <T> invoke(@BuilderInference block: PaginationBuilder<T>.() -> Unit): Pagination<T> {
            val builder = InternalPaginationBridge.instance.createPaginationBuilder<T>()
            builder.block()
            return builder.build()
        }
    }
}