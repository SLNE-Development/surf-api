package dev.slne.surf.api.minestom.chat

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import org.jetbrains.annotations.ApiStatus

/**
 * A chat renderer is responsible for rendering chat messages sent by [Player]s to the server.
 */
fun interface ChatRenderer {

    /**
     * Renders a chat message. This will be called once for each receiving [Audience].
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
     * Similar to [ChatRenderer], but without knowledge of the message viewer.
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
         * Creates a new instance of the default [ChatRenderer].
         */
        fun defaultRenderer(): ChatRenderer =
            ViewerUnawareChatRenderer.Default { _, sourceDisplayName, message ->
                Component.translatable("chat.type.text", sourceDisplayName, message)
            }

        /**
         * Creates a new viewer-unaware [ChatRenderer], which will render the chat message a single
         * time, displaying the same rendered message to every viewing [Audience].
         *
         * @param renderer the viewer unaware renderer
         */
        fun viewerUnaware(renderer: ViewerUnaware): ChatRenderer =
            ViewerUnawareChatRenderer.Impl(renderer)
    }
}
