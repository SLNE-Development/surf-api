package dev.slne.surf.surfapi.core.api.messages.builder

import dev.slne.surf.surfapi.core.api.font.toSmallCaps
import dev.slne.surf.surfapi.core.api.messages.adventure.buildText
import dev.slne.surf.surfapi.core.api.messages.adventure.clickRunsCommand
import dev.slne.surf.surfapi.core.api.messages.adventure.sendText
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.event.ClickEvent
import kotlin.math.ceil
import kotlin.math.min

/**
 * Default implementation of the [SurfPageableMessageBuilder] interface.
 */
@SurfPageableMessageBuilderDsl
class PageableMessageBuilder(
    override var linesPerPage: Int = 10
) : SurfPageableMessageBuilder {

    private val lines = mutableObjectListOf<Component>()
    private var title: Component = Component.empty()

    override fun line(block: SurfComponentBuilder.() -> Unit) {
        lines.add(SurfComponentBuilder(block))
    }

    override fun title(block: SurfComponentBuilder.() -> Unit) {
        title = SurfComponentBuilder(block)
    }

    override fun send(sender: Audience, page: Int) {
        val totalPages = ceil(lines.size.toDouble() / linesPerPage).toInt().coerceAtLeast(1)
        if (page < 1 || page > totalPages) {
            sender.sendText {
                error("Seite ")
                variableValue(page.toString())
                error(" existiert nicht.")
            }
            return
        }

        val start = (page - 1) * linesPerPage
        val end = min(start + linesPerPage, lines.size)

        sender.sendText {
            if (title != Component.empty()) {
                append(title)
                appendNewline()
            }

            for (i in start until end) {
                append(lines[i])
                appendNewline()
            }

            if (totalPages > 1) {
                append(paginationComponent(page, totalPages))
            }
        }
    }

    private fun navButton(label: String, targetPage: Int, enabled: Boolean): Component {
        return buildText {
            if (enabled) {
                success(label)
                clickEvent(ClickEvent.callback {
                    send(it, targetPage)
                })
            } else {
                error(label)
            }
        }
    }

    private fun paginationComponent(page: Int, totalPages: Int): Component {
        return buildText {
            append(navButton("[<<] ", 1, page > 1))
            append(navButton("[<] ", page - 1, page > 1))
            darkSpacer("Seite $page/$totalPages".toSmallCaps())
            append(navButton(" [>] ", page + 1, page < totalPages))
            append(navButton(" [>>]", totalPages, page < totalPages))
        }
    }

    companion object {
        /**
         * DSL-style builder entry point.
         */
        @JvmStatic
        operator fun invoke(block: PageableMessageBuilder.() -> Unit): PageableMessageBuilder {
            return PageableMessageBuilder().apply(block)
        }
    }
}
