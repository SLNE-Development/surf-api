package dev.slne.surf.api.minestom.server.chat.impl

import dev.slne.surf.api.minestom.chat.AsyncPlayerChatEvent
import dev.slne.surf.api.minestom.chat.ChatRenderer
import dev.slne.surf.api.minestom.extension.ConnectionManager
import dev.slne.surf.api.minestom.server.chat.impl.signature.PlayerChatMessage
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.audience.ForwardingAudience
import net.kyori.adventure.text.Component
import net.kyori.adventure.translation.GlobalTranslator
import net.minestom.server.adventure.MinestomAdventure
import net.minestom.server.adventure.audience.Audiences
import net.minestom.server.command.ConsoleSender
import net.minestom.server.entity.Player
import net.minestom.server.message.ChatType

/**
 * Runs one signed chat message through [AsyncPlayerChatEvent] and sends it to the event's viewers.
 *
 * Delivery depends on what the listeners changed. Nothing is sent for cancelled events or when the
 * sender has left while the listeners ran.
 * - Nothing changed: the message is sent as received with the vanilla chat type.
 * - Message changed, default renderer: vanilla chat type, the new content as unsigned content.
 * - Custom renderer: `surf:raw` chat type ([SurfChatTypes.raw]), the rendered component as
 *   unsigned content.
 *
 * The original signature stays attached in every case. [process] suspends while viewer-specific
 * renderers run, and renders concurrently per viewer only for renderers that are not
 * [ChatRenderer.ViewerUnaware].
 */
internal class ChatProcessor(
    private val player: Player,
    private val message: PlayerChatMessage,
) {
    private val originalMessage = message.decoratedContent()
    private val outgoing = OutgoingChatMessage.create(message)

    suspend fun process() {
        val players = ConnectionManager.onlinePlayers
        val viewers = ObjectLinkedOpenHashSet<Audience>(players.size + 1).apply {
            addAll(players)
            add(Audiences.console())
        }

        val renderer = ChatRenderer.defaultRenderer()
        val event = AsyncPlayerChatEvent(
            player = player,
            viewers = viewers,
            renderer = renderer,
            message = originalMessage,
            originalMessage = originalMessage,
            signedMessage = message.adventureView()
        )

        AsyncPlayerChatEvent.node.call(event)

        complete(
            event,
            messageChanged = event.message != originalMessage,
            formatChanged = event.renderer !== renderer,
        )
    }

    private suspend fun complete(
        event: AsyncPlayerChatEvent,
        messageChanged: Boolean,
        formatChanged: Boolean,
    ) {
        if (event.isCancelled || player.isRemoved || !player.isOnline) return

        val displayName = player.chatDisplayName()
        val message = event.message
        val renderer = event.renderer
        val viewers = event.viewers

        val useVanillaChatType = renderer is ChatRenderer.Default
        val chatType = BoundChatType(
            chatType = if (useVanillaChatType) ChatType.CHAT else SurfChatTypes.raw,
            name = displayName
        )

        when {
            formatChanged -> if (renderer is ChatRenderer.ViewerUnaware) {
                val rendered = renderer.render(player, displayName, message)
                broadcast(viewers, chatType, sendConcurrent = false) { rendered }
            } else {
                broadcast(viewers, chatType, sendConcurrent = true) { viewer ->
                    renderer.render(player, displayName, message, viewer)
                }
            }

            messageChanged -> {
                val rendered = if (useVanillaChatType) {
                    message
                } else {
                    (renderer as ChatRenderer.ViewerUnaware).render(player, displayName, message)
                }

                broadcast(viewers, chatType, sendConcurrent = false) { rendered }
            }

            else -> broadcast(viewers, chatType, sendConcurrent = false, unsignedFor = null)
        }
    }

    private suspend fun broadcast(
        viewers: Set<Audience>,
        chatType: BoundChatType,
        sendConcurrent: Boolean,
        unsignedFor: (suspend (Audience) -> Component)?
    ) {
        if (viewers.isEmpty()) return
        if (sendConcurrent) {
            supervisorScope {
                for (viewer in viewers) {
                    launch {
                        sendTo(viewer, chatType, unsignedFor?.invoke(viewer))
                    }
                }
            }
        } else {
            for (viewer in viewers) {
                sendTo(viewer, chatType, unsignedFor?.invoke(viewer))
            }
        }
    }

    private fun sendTo(viewer: Audience, chatType: BoundChatType, unsigned: Component?) {
        when (viewer) {
            is Player -> {
                val holder = ChatSessionHolderImpl.fromPlayer(viewer) ?: return
                outgoing.sendToPlayer(holder.handler, chatType, unsigned)
            }

            is ConsoleSender -> viewer.sendMessage(
                GlobalTranslator.render(
                    chatType.decorate(unsigned ?: outgoing.content),
                    MinestomAdventure.getDefaultLocale()
                )
            )

            is ForwardingAudience.Single -> sendTo(viewer.audience(), chatType, unsigned)

            else -> {
                val message =
                    if (unsigned == null) message else message.withUnsignedContent(unsigned)
                viewer.sendMessage(message.adventureView(), chatType.adventure())
            }
        }
    }
}

/**
 * The name a chat message is attributed to: the display name, or the username when there is none.
 * Shift-clicking it inserts the username, and hovering it shows the player.
 */
internal fun Player.chatDisplayName(): Component = (displayName ?: Component.text(username))
    .insertion(username)
    .hoverEvent(this)
