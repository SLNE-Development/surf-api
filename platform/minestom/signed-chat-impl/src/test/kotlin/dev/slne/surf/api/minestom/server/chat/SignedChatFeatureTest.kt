package dev.slne.surf.api.minestom.server.chat

import dev.slne.surf.api.minestom.chat.AsyncPlayerChatEvent
import dev.slne.surf.api.minestom.chat.ChatSessionHolder
import dev.slne.surf.api.minestom.command.CommandAPICommand
import dev.slne.surf.api.minestom.command.dsl.anyExecutor
import dev.slne.surf.api.minestom.command.dsl.signedMessageArgument
import dev.slne.surf.api.minestom.server.SurfMinestomServer
import dev.slne.surf.api.minestom.server.chat.impl.BoundChatType
import dev.slne.surf.api.minestom.server.chat.impl.ChatProcessor
import dev.slne.surf.api.minestom.server.chat.impl.ChatSessionHolderImpl
import dev.slne.surf.api.minestom.server.chat.impl.signature.PlayerChatMessage
import dev.slne.surf.api.minestom.server.surfMinestomServer
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.minestom.server.coordinate.Pos
import net.minestom.server.crypto.ArgumentSignatures
import net.minestom.server.crypto.LastSeenMessages
import net.minestom.server.crypto.MessageSignature
import net.minestom.server.entity.Player
import net.minestom.server.event.EventDispatcher
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.message.ChatMessageType
import net.minestom.server.message.ChatType
import net.minestom.server.network.packet.client.play.ClientSignedCommandChatPacket
import net.minestom.server.network.packet.server.play.PlayerChatMessagePacket
import net.minestom.server.network.packet.server.play.SystemChatPacket
import net.minestom.server.network.player.ClientSettings
import net.minestom.testing.Collector
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

@EnvTest
class SignedChatFeatureTest {

    private fun withFeature(
        settings: SignedChatSettings.() -> Unit = {},
        block: (SurfMinestomServer) -> Unit,
    ) {
        val api = surfMinestomServer { withSignedChat(settings) }
        try {
            block(api)
        } finally {
            api.shutdown()
        }
    }

    private fun onVirtualThread(block: () -> Unit) = Thread.startVirtualThread(block).join()

    /**
     * Runs [block] while the server process reports itself as started; the signed chat handlers
     * ignore packets while the server stops, which is the state of every test process.
     */
    private fun <T> asStartedServer(env: Env, block: () -> T): T {
        val field = env.process().javaClass.getDeclaredField("started")
        field.isAccessible = true
        val started = field.get(env.process()) as AtomicBoolean
        started.set(true)

        try {
            return block()
        } finally {
            started.set(false)
        }
    }

    private fun Player.holder() = ChatSessionHolderImpl.fromPlayer(this)

    private fun Player.setChatMessageType(type: ChatMessageType) {
        val current = settings
        refreshSettings(
            ClientSettings(
                current.locale(), current.viewDistance(), type, current.chatColors(),
                current.displayedSkinParts(), current.mainHand(), current.enableTextFiltering(),
                current.allowServerListings(), current.particleSetting(),
            )
        )
    }

    private fun signedCommand(command: String, vararg names: String) = ClientSignedCommandChatPacket(
        command,
        System.currentTimeMillis(),
        0L,
        ArgumentSignatures(names.map { ArgumentSignatures.Entry(it, MessageSignature(ByteArray(256))) }),
        LastSeenMessages.Update(0, BitSet()),
        0,
    )

    private fun Collector<SystemChatPacket>.translationKeys() =
        collect().map { (it.message() as? TranslatableComponent)?.key() }

    @Test
    fun `a player gets a chat session holder when it spawns`(env: Env) = withFeature {
        val player = env.createPlayer(env.createFlatInstance(), Pos.ZERO)

        assertNotNull(player.holder())
        assertSame(player.holder(), ChatSessionHolder.fromPlayer(player))
    }

    @Test
    fun `a new configuration phase resets the chat state`(env: Env) = withFeature {
        val player = env.createPlayer(env.createFlatInstance(), Pos.ZERO)
        val before = player.holder()
        assertNotNull(before)

        onVirtualThread { EventDispatcher.call(AsyncPlayerConfigurationEvent(player, true)) }
        assertSame(before, player.holder())

        onVirtualThread { EventDispatcher.call(AsyncPlayerConfigurationEvent(player, false)) }
        assertNull(player.holder())
    }

    @Test
    fun `players that do not accept chat receive no player chat`(env: Env) = withFeature {
        val instance = env.createFlatInstance()
        val sender = env.createPlayer(instance, Pos.ZERO)
        val fullConnection = env.createConnection()
        val full = fullConnection.connect(instance, Pos.ZERO)
        val hiddenConnection = env.createConnection()
        val hidden = hiddenConnection.connect(instance, Pos.ZERO)
        hidden.setChatMessageType(ChatMessageType.SYSTEM)

        val fullPackets = fullConnection.trackIncoming(PlayerChatMessagePacket::class.java)
        val hiddenPackets = hiddenConnection.trackIncoming(PlayerChatMessagePacket::class.java)

        val message = PlayerChatMessage.unsigned(sender.uuid, "hello")
        val chatType = BoundChatType(ChatType.CHAT, Component.text("sender"))
        full.holder()!!.handler.sendPlayerChatMessage(message, chatType)
        hidden.holder()!!.handler.sendPlayerChatMessage(message, chatType)

        fullPackets.assertCount(1)
        hiddenPackets.assertEmpty()
    }

    @Test
    fun `a message of a sender that left while the event ran is not sent`(env: Env) = withFeature {
        val instance = env.createFlatInstance()
        val sender = env.createPlayer(instance, Pos.ZERO)
        val receiverConnection = env.createConnection()
        val receiver = receiverConnection.connect(instance, Pos.ZERO)
        val packets = receiverConnection.trackIncoming(PlayerChatMessagePacket::class.java)

        AsyncPlayerChatEvent.addListener { event -> event.player.remove(false) }.use {
            runBlocking {
                ChatProcessor(sender, PlayerChatMessage.unsigned(sender.uuid, "bye")).process()
            }
        }

        assertTrue(sender.isRemoved)
        assertNotNull(receiver.holder())
        packets.assertEmpty()
    }

    @Test
    fun `a message of a sender that stays is sent to players without a chat packet before`(env: Env) =
        withFeature {
            val instance = env.createFlatInstance()
            val sender = env.createPlayer(instance, Pos.ZERO)
            val receiverConnection = env.createConnection()
            receiverConnection.connect(instance, Pos.ZERO)
            val packets = receiverConnection.trackIncoming(PlayerChatMessagePacket::class.java)

            runBlocking {
                ChatProcessor(sender, PlayerChatMessage.unsigned(sender.uuid, "hi")).process()
            }

            packets.assertCount(1)
        }

    @Test
    fun `an unsigned command with signable arguments is rejected when profiles are enforced`(env: Env) =
        withFeature({ enforceSecureProfile = true }) {
            CommandAPICommand("echoes").signedMessageArgument("text").anyExecutor { _, _ -> }.register()
            CommandAPICommand("plain").anyExecutor { _, _ -> }.register()

            val connection = env.createConnection()
            val player = connection.connect(env.createFlatInstance(), Pos.ZERO)
            val messages = connection.trackIncoming(SystemChatPacket::class.java)
            val handler = player.holder()!!.handler
            var ran = false

            handler.handleUnsignedCommandChat("echoes hello") { ran = true }
            assertFalse(ran)
            assertEquals(listOf("chat.disabled.invalid_command_signature"), messages.translationKeys())

            handler.handleUnsignedCommandChat("plain") { ran = true }
            assertTrue(ran)
        }

    @Test
    fun `a signed command without signatures runs unsigned unless profiles are enforced`(env: Env) =
        withFeature {
            CommandAPICommand("echoes").signedMessageArgument("text").anyExecutor { _, _ -> }.register()
            val player = env.createPlayer(env.createFlatInstance(), Pos.ZERO)
            var ran = false

            asStartedServer(env) {
                player.holder()!!.handler.handleSignedCommandChat(signedCommand("echoes hello")) { ran = true }
            }

            assertTrue(ran)
        }

    @Test
    fun `a signed command without signatures reports the missing profile key when enforced`(env: Env) =
        withFeature({ enforceSecureProfile = true }) {
            CommandAPICommand("echoes").signedMessageArgument("text").anyExecutor { _, _ -> }.register()
            val connection = env.createConnection()
            val player = connection.connect(env.createFlatInstance(), Pos.ZERO)
            val messages = connection.trackIncoming(SystemChatPacket::class.java)
            var ran = false

            asStartedServer(env) {
                player.holder()!!.handler.handleSignedCommandChat(signedCommand("echoes hello")) { ran = true }
            }

            assertFalse(ran)
            assertEquals(listOf("chat.disabled.missingProfileKey"), messages.translationKeys())
        }

    @Test
    fun `mismatching command signatures are reported and counted as command spam`(env: Env) =
        withFeature({ commandSpamThresholdSeconds = 1 }) {
            CommandAPICommand("echoes").signedMessageArgument("text").anyExecutor { _, _ -> }.register()
            val connection = env.createConnection()
            val player = connection.connect(env.createFlatInstance(), Pos.ZERO)
            val messages = connection.trackIncoming(SystemChatPacket::class.java)
            var ran = false

            asStartedServer(env) {
                player.holder()!!.handler.handleSignedCommandChat(signedCommand("echoes hello", "other")) {
                    ran = true
                }
            }

            assertFalse(ran)
            assertEquals(listOf("chat.disabled.invalid_command_signature"), messages.translationKeys())

            env.tick()
            assertFalse(player.isOnline, "the rejected command must count towards the spam threshold")
        }

    @Test
    fun `mismatching command signatures do not kick the player`(env: Env) = withFeature {
        CommandAPICommand("echoes").signedMessageArgument("text").anyExecutor { _, _ -> }.register()
        val player = env.createPlayer(env.createFlatInstance(), Pos.ZERO)

        asStartedServer(env) {
            player.holder()!!.handler.handleSignedCommandChat(signedCommand("echoes hello", "other")) {}
        }
        env.tick()

        assertTrue(player.isOnline)
    }
}
