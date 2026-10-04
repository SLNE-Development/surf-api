package dev.slne.surf.api.minestom.server.chat.impl

import dev.slne.surf.api.minestom.chat.AsyncPlayerChatEvent
import dev.slne.surf.api.minestom.chat.ChatRenderer
import dev.slne.surf.api.minestom.extension.ConnectionManager
import dev.slne.surf.api.minestom.server.chat.impl.signature.PlayerChatMessage
import net.minestom.server.entity.Player
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
import net.minestom.server.message.ChatType

internal class ChatProcessor(
    private val player: Player,
    private val message: PlayerChatMessage,
) {
    private val originalMessage = message.decoratedContent()
    private val outgoing = OutgoingChatMessage.create(message)

    private var messageChanged = false
    private var formatChanged = false

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

        readModifications(event, renderer)
        complete(event)
    }

    private fun readModifications(event: AsyncPlayerChatEvent, originalRenderer: ChatRenderer) {
        messageChanged = event.message != originalMessage
        if (originalRenderer !== event.renderer) {
            formatChanged = true
        }
    }

    private suspend fun complete(event: AsyncPlayerChatEvent) {
        if (event.isCancelled) return

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
        if (viewers.size == 1) {
            sendTo(viewers.first(), chatType, unsignedFor?.invoke(viewers.first()))
        } else if (sendConcurrent) {
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
                outgoing.sendToPlayer(holder.handler, filtered = false, chatType, unsigned)
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
 * The name a chat message is attributed to: the display name, or the username, inserting the
 * username on shift-click and showing the player on hover.
 */
internal fun Player.chatDisplayName(): Component = (displayName ?: Component.text(username))
    .insertion(username)
    .hoverEvent(this)
