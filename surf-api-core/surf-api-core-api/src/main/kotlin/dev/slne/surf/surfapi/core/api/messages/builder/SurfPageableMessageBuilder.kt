package dev.slne.surf.surfapi.core.api.messages.builder

import net.kyori.adventure.audience.Audience

/**
 * DSL interface for building pageable messages using [PageableMessageBuilder].
 * Provides a structured way to define paginated message content and send it to an [Audience].
 */
@DslMarker
annotation class SurfPageableMessageBuilderDsl

@SurfPageableMessageBuilderDsl
interface SurfPageableMessageBuilder {

    /**
     * The number of lines displayed per page.
     */
    var linesPerPage: Int

    /**
     * The base command used for navigating between pages.
     * Example: "/example page %page%"
     */
    var pageCommand: String

    /**
     * Sets the message title.
     *
     * @param block a builder block to configure the title using [SurfComponentBuilder]
     */
    fun title(block: SurfComponentBuilder.() -> Unit)

    /**
     * Adds a line of content to the message.
     *
     * @param block a builder block to configure the line using [SurfComponentBuilder]
     */
    fun line(block: SurfComponentBuilder.() -> Unit)

    /**
     * Sends the paginated message to the given [Audience] at the specified page.
     *
     * @param sender the audience receiving the message
     * @param page the page number to display (starting at 1)
     */
    fun send(sender: Audience, page: Int)

    companion object {
        /**
         * Creates a new [PageableMessageBuilder] instance and applies the provided DSL block.
         *
         * @param block the DSL configuration block
         * @return a fully constructed [PageableMessageBuilder] instance
         */
        @JvmStatic
        operator fun invoke(linesPerPage: Int = 10, block: PageableMessageBuilder.() -> Unit): PageableMessageBuilder {
            return PageableMessageBuilder(linesPerPage).apply(block)
        }
    }
}
