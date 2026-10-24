package dev.slne.surf.api.minestom.chat

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player

/**
 * Adapts a [ChatRenderer.ViewerUnaware] to a [ChatRenderer].
 *
 * The result of the last render is reused while the arguments are the same instances, so a message
 * sent to many viewers is rendered once. Any other arguments render again. Concurrent calls with
 * identical arguments may each render.
 */
internal sealed class ViewerUnawareChatRenderer(
    private val unaware: ChatRenderer.ViewerUnaware
) : ChatRenderer, ChatRenderer.ViewerUnaware {

    private class Rendered(
        val source: Player,
        val sourceDisplayName: Component,
        val message: Component,
        val result: Component,
    ) {
        fun matches(source: Player, sourceDisplayName: Component, message: Component) =
            this.source === source &&
                    this.sourceDisplayName === sourceDisplayName &&
                    this.message === message
    }

    @Volatile
    private var last: Rendered? = null

    override suspend fun render(
        source: Player,
        sourceDisplayName: Component,
        message: Component,
        viewer: Audience
    ): Component = render(source, sourceDisplayName, message)

    override suspend fun render(
        source: Player,
        sourceDisplayName: Component,
        message: Component
    ): Component {
        last?.takeIf { it.matches(source, sourceDisplayName, message) }?.let { return it.result }

        val result = unaware.render(source, sourceDisplayName, message)
        last = Rendered(source, sourceDisplayName, message, result)
        return result
    }

    class Impl(unaware: ChatRenderer.ViewerUnaware) : ViewerUnawareChatRenderer(unaware)

    class Default(unaware: ChatRenderer.ViewerUnaware) :
        ViewerUnawareChatRenderer(unaware), ChatRenderer.Default
}
