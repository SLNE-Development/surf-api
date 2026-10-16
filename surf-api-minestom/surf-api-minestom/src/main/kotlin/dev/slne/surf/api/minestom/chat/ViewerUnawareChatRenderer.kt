package dev.slne.surf.api.minestom.chat

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player

/**
 * Adapts a [ChatRenderer.ViewerUnaware] to a [ChatRenderer].
 *
 * The first rendered result is cached and returned for every later call regardless of its
 * arguments, so an instance serves one message only. The cache is not synchronized.
 */
internal sealed class ViewerUnawareChatRenderer(
    private val unaware: ChatRenderer.ViewerUnaware
) : ChatRenderer, ChatRenderer.ViewerUnaware {

    private var rendered: Component? = null

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
    ): Component = rendered ?: unaware.render(source, sourceDisplayName, message).also {
        rendered = it
    }

    class Impl(unaware: ChatRenderer.ViewerUnaware) : ViewerUnawareChatRenderer(unaware)

    class Default(unaware: ChatRenderer.ViewerUnaware) :
        ViewerUnawareChatRenderer(unaware), ChatRenderer.Default
}
