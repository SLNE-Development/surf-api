package dev.slne.surf.api.minestom.chat

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import org.jetbrains.annotations.ApiStatus

/**
 * Renders chat messages sent by [Player]s into the components shown to viewers.
 */
fun interface ChatRenderer {

    /**
     * Renders a chat message. Called once per receiving [Audience].
     *
     * @param source            the message source
     * @param sourceDisplayName the display name of the source player
     * @param message           the chat message
     * @param viewer            the receiving [Audience]
     * @return a rendered chat message
     */
    @ApiStatus.OverrideOnly
    suspend fun render(
        source: Player,
        sourceDisplayName: Component,
        message: Component,
        viewer: Audience
    ): Component

    /**
     * Marker for the renderer returned by [defaultRenderer].
     */
    @ApiStatus.Internal
    sealed interface Default : ChatRenderer, ViewerUnaware

    /**
     * A [ChatRenderer] variant that does not depend on the viewer.
     *
     * @see viewerUnaware
     */
    fun interface ViewerUnaware {

        /**
         * Renders a chat message.
         *
         * @param source            the message source
         * @param sourceDisplayName the display name of the source player
         * @param message           the chat message
         * @return a rendered chat message
         */
        @ApiStatus.OverrideOnly
        suspend fun render(source: Player, sourceDisplayName: Component, message: Component): Component
    }

    companion object {

        /**
         * Creates the default renderer, which formats messages with the vanilla `chat.type.text`
         * translation.
         */
        fun defaultRenderer(): ChatRenderer =
            ViewerUnawareChatRenderer.Default { _, sourceDisplayName, message ->
                Component.translatable("chat.type.text", sourceDisplayName, message)
            }

        /**
         * Creates a [ChatRenderer] that renders the message once and shows the result to every
         * [Audience].
         *
         * @param renderer the viewer-independent renderer
         */
        fun viewerUnaware(renderer: ViewerUnaware): ChatRenderer =
            ViewerUnawareChatRenderer.Impl(renderer)
    }
}
