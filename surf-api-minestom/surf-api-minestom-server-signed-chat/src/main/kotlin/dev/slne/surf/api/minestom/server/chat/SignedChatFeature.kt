package dev.slne.surf.api.minestom.server.chat

import dev.slne.surf.api.minestom.server.SurfMinestomServer
import dev.slne.surf.api.minestom.server.SurfMinestomServerBuilder
import dev.slne.surf.api.minestom.server.SurfMinestomServerDsl
import dev.slne.surf.api.minestom.server.SurfMinestomFeature
import dev.slne.surf.api.minestom.chat.ChatSessionHolder
import dev.slne.surf.api.minestom.server.chat.impl.ChatProcessor
import dev.slne.surf.api.minestom.server.chat.impl.ChatSessionHolderImpl
import dev.slne.surf.api.minestom.server.chat.impl.ChatTranslations
import dev.slne.surf.api.minestom.server.chat.impl.SurfChatTypes
import dev.slne.surf.api.minestom.server.chat.impl.signature.PlayerChatMessage
import dev.slne.surf.api.minestom.server.chat.impl.signature.RemoteChatSession
import dev.slne.surf.api.minestom.extension.CommandManager
import dev.slne.surf.api.minestom.extension.ConnectionManager
import dev.slne.surf.api.minestom.extension.PacketListenerManager
import dev.slne.surf.api.minestom.extension.addListener
import dev.slne.surf.api.minestom.server.impl.command.SignedCommandArguments
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import net.minestom.server.crypto.ChatSession
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.event.player.PlayerTickEvent
import net.minestom.server.listener.ChatMessageListener
import net.minestom.server.network.packet.client.play.ClientChatAckPacket
import net.minestom.server.network.packet.client.play.ClientChatMessagePacket
import net.minestom.server.network.packet.client.play.ClientChatSessionUpdatePacket
import net.minestom.server.network.packet.client.play.ClientCommandChatPacket
import net.minestom.server.network.packet.client.play.ClientSignedCommandChatPacket
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket
import net.minestom.server.utils.PacketSendingUtils

/**
 * Configures [withSignedChat].
 */
@SurfMinestomServerDsl
class SignedChatSettings internal constructor() {

    /**
     * Whether clients must present a signed chat session before they may chat.
     *
     * When enabled, players without a Mojang-issued profile key (offline mode, some proxies,
     * Geyser) are told chat is disabled instead of being allowed to send unsigned messages.
     */
    var enforceSecureProfile: Boolean = false

    /**
     * Vanilla's `chat-spam-threshold-seconds`. Each chat message adds 20 to a counter that drains
     * by 1 per tick; exceeding 20 times this value disconnects the player with `disconnect.spam`.
     */
    var chatSpamThresholdSeconds: Int = 10

    /** Vanilla's `command-spam-threshold-seconds`. Same mechanism as above, for commands. */
    var commandSpamThresholdSeconds: Int = 10
}

/**
 * Handles chat the way vanilla does: messages keep their signatures, so clients show them as
 * verified, and can be reported.
 *
 * Chat messages are processed through [AsyncPlayerChatEvent], off the tick thread. The secure chat
 * state of a player is reached through [ChatSessionHolder.fromPlayer].
 */
@OptIn(InternalSurfApi::class)
class SignedChatFeature internal constructor(
    private val settings: SignedChatSettings,
) : SurfMinestomFeature {

    override val id: String = ID

    override fun load(api: SurfMinestomServer) {
        SurfChatTypes.register()
        ChatSessionHolder.installProvider(ChatSessionHolderImpl)
        ChatTranslations.register()
        registerPacketListeners()

        SignedCommandArguments.unsignedFactory = { sender, content ->
            (sender?.let { PlayerChatMessage.unsigned(it, content) } ?: PlayerChatMessage.system(content))
                .adventureView()
        }

        api.eventNode.addListener<PlayerTickEvent> { event ->
            ChatSessionHolderImpl.fromPlayer(event.player)?.handler?.tick()
        }
        api.eventNode.addListener<PlayerSpawnEvent> { event ->
            if (event.isFirstSpawn) announceChatSessionsTo(event.player)
        }
        api.eventNode.addListener<PlayerDisconnectEvent> { event ->
            ChatSessionHolderImpl.remove(event.player)?.handler?.close()
        }
    }

    override fun disable(api: SurfMinestomServer) {
        with(PacketListenerManager) {
            setPlayListener(ClientChatMessagePacket::class.java, ChatMessageListener::chatMessageListener)
            setPlayListener(ClientChatAckPacket::class.java) { _, _ -> }
            setPlayListener(ClientChatSessionUpdatePacket::class.java) { _, _ -> }
            setPlayListener(ClientCommandChatPacket::class.java, ChatMessageListener::commandChatListener)
            setPlayListener(
                ClientSignedCommandChatPacket::class.java,
                ChatMessageListener::signedCommandChatListener,
            )
        }

        SignedCommandArguments.resetUnsignedFactory()
        ChatSessionHolder.installProvider(null)
        ChatSessionHolderImpl.clear()
    }

    private fun holder(player: Player): ChatSessionHolderImpl? = ChatSessionHolderImpl.create(player, settings)

    private fun registerPacketListeners() = with(PacketListenerManager) {
        setPlayListener(ClientChatMessagePacket::class.java) { packet, player ->
            holder(player)?.handler?.handleChat(packet) { message ->
                ChatProcessor(player, message).process()
            }
        }

        setPlayListener(ClientChatAckPacket::class.java) { packet, player ->
            holder(player)?.handler?.handleChatAck(packet)
        }

        setPlayListener(ClientChatSessionUpdatePacket::class.java) { packet, player ->
            holder(player)?.handler?.handleChatSessionUpdate(packet) { session ->
                broadcastChatSession(player, session)
            }
        }

        setPlayListener(ClientCommandChatPacket::class.java) { packet, player ->
            holder(player)?.handler?.handleUnsignedCommandChat(packet.message()) { command ->
                CommandManager.execute(player, command)
            }
        }

        setPlayListener(ClientSignedCommandChatPacket::class.java) { packet, player ->
            holder(player)?.handler?.handleSignedCommandChat(packet) { command ->
                CommandManager.execute(player, command)
            }
        }
    }

    private fun broadcastChatSession(player: Player, session: RemoteChatSession) {
        PacketSendingUtils.broadcastPlayPacket(chatSessionInfoPacket(player, session.asData()))
    }

    /**
     * Minestom announces every player to a joining one without a chat session, so the sessions
     * the others already established are sent after it.
     */
    private fun announceChatSessionsTo(joined: Player) {
        for (other in ConnectionManager.onlinePlayers) {
            if (other === joined) continue

            val session = ChatSessionHolderImpl.fromPlayer(other)?.chatSession ?: continue
            joined.sendPacket(chatSessionInfoPacket(other, session))
        }
    }

    private fun chatSessionInfoPacket(player: Player, session: ChatSession) =
        PlayerInfoUpdatePacket(
            PlayerInfoUpdatePacket.Action.INITIALIZE_CHAT,
            PlayerInfoUpdatePacket.Entry(
                player.uuid,
                player.username,
                emptyList(),
                true,
                player.latency,
                player.gameMode ?: GameMode.SURVIVAL,
                player.displayName,
                session,
                0,
                false
            )
        )

    companion object {
        const val ID = "signed-chat"
    }
}

/**
 * Handles chat the way vanilla does, keeping the signatures of chat messages and signed command
 * arguments.
 *
 * @see SignedChatFeature
 */
fun SurfMinestomServerBuilder.withSignedChat(block: SignedChatSettings.() -> Unit = {}) {
    install(SignedChatFeature(SignedChatSettings().apply(block)))
}
