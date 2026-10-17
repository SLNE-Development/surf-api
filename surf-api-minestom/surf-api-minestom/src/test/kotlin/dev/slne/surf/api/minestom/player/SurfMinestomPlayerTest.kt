package dev.slne.surf.api.minestom.player

import dev.slne.surf.api.minestom.chat.ChatSessionHolder
import dev.slne.surf.api.minestom.chat.RemoteChatSender
import dev.slne.surf.api.minestom.chat.RemoteSignedMessage
import dev.slne.surf.api.minestom.permission.MinestomPermissions
import dev.slne.surf.api.minestom.permission.PermissionProvider
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.permission.PermissionChecker
import net.kyori.adventure.text.Component
import net.kyori.adventure.util.TriState
import net.minestom.server.coordinate.Pos
import net.minestom.server.crypto.ChatSession
import net.minestom.server.crypto.PlayerPublicKey
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket
import net.minestom.server.network.player.GameProfile
import net.minestom.server.network.player.PlayerConnection
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import net.minestom.testing.TestConnection
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture

@OptIn(InternalSurfApi::class)
@EnvTest
class SurfMinestomPlayerTest {

    @Test
    fun `a spectator is taken off every client that is not one`(env: Env) =
        withPlayerPair(env) { spectator, other, otherConnection ->
            val destroys = otherConnection.trackIncoming(DestroyEntitiesPacket::class.java)

            spectator.switchGameMode(GameMode.SPECTATOR)

            assertFalse(other in spectator.viewers, "a non-spectator was still shown the spectator")
            assertTrue(spectator in other.viewers, "the spectator stopped seeing a non-spectator")
            assertTrue(spectator.isInvisible, "the spectator was not marked invisible")
            assertEquals(
                listOf(listOf(spectator.entityId)),
                destroys.collect().map(DestroyEntitiesPacket::entityIds),
                "the spectator was not removed from the other client",
            )
        }

    @Test
    fun `spectators keep seeing each other`(env: Env) =
        withPlayerPair(env) { spectator, other, _ ->
            spectator.switchGameMode(GameMode.SPECTATOR)
            other.switchGameMode(GameMode.SPECTATOR)

            assertTrue(other in spectator.viewers, "a spectator was not shown another spectator")
            assertTrue(spectator in other.viewers, "a spectator was not shown another spectator")
        }

    @Test
    fun `leaving spectator shows the player to everyone again`(env: Env) =
        withPlayerPair(env) { spectator, other, _ ->
            spectator.switchGameMode(GameMode.SPECTATOR)
            spectator.switchGameMode(GameMode.ADVENTURE)

            assertTrue(other in spectator.viewers, "the player stayed hidden after spectator")
            assertFalse(spectator.isInvisible, "the player stayed invisible after spectator")
        }

    @Test
    fun `leaving spectator hides the spectators that were visible`(env: Env) =
        withPlayerPair(env) { leaving, staying, _ ->
            leaving.switchGameMode(GameMode.SPECTATOR)
            staying.switchGameMode(GameMode.SPECTATOR)
            leaving.switchGameMode(GameMode.ADVENTURE)

            assertFalse(leaving in staying.viewers, "a spectator stayed shown to a non-spectator")
            assertTrue(staying in leaving.viewers, "the remaining spectator lost the player")
        }

    @Test
    fun `a switch between the other modes leaves visibility alone`(env: Env) =
        withPlayerPair(env) { player, other, _ ->
            player.switchGameMode(GameMode.CREATIVE)

            assertTrue(other in player.viewers, "an ordinary mode switch hid the player")
            assertFalse(player.isInvisible, "an ordinary mode switch made the player invisible")
        }

    @Test
    fun `resolves Adventure's permission checker through the surf api`(env: Env) {
        MinestomPermissions.installProvider(PermissionProvider { _, permission ->
            if (permission == GRANTED) TriState.TRUE else TriState.NOT_SET
        })

        try {
            withPlayerPair(env) { player, _, _ ->
                assertInstanceOf(SurfMinestomPlayer::class.java, player)

                val checker = player.get(PermissionChecker.POINTER).orElseThrow()
                assertEquals(TriState.TRUE, checker.value(GRANTED))
                assertEquals(TriState.NOT_SET, checker.value("surf.test.other"))
            }
        } finally {
            MinestomPermissions.installProvider(null)
        }
    }

    @Test
    fun `keeps the signed chat session in every player list entry`(env: Env) {
        val session = chatSession()
        withExposingPlayerPair(env) { player, _ ->
            assertNull(
                player.addPlayerToList().entries().single().chatSession(),
                "the entry carried a session without signed chat",
            )

            ChatSessionHolder.installProvider(SessionProvider(player, session))
            try {
                val packet = player.addPlayerToList()

                assertEquals(
                    listOf(session),
                    packet.entries().map { entry -> entry.chatSession() },
                    "the entry lost the chat session",
                )
            } finally {
                ChatSessionHolder.installProvider(null)
            }
        }
    }

    private fun Player.switchGameMode(gameMode: GameMode) {
        assertTrue(setGameMode(gameMode), "the game mode change was refused")
    }

    private fun withPlayerPair(
        env: Env,
        block: (Player, Player, TestConnection) -> Unit,
    ) = withPlayers(env, { connection, profile -> SurfMinestomPlayer(connection, profile) }, block)

    private fun withExposingPlayerPair(
        env: Env,
        block: (ExposingPlayer, TestConnection) -> Unit,
    ) = withPlayers(
        env,
        { connection, profile -> ExposingPlayer(connection, profile) },
    ) { first, _, secondConnection -> block(first as ExposingPlayer, secondConnection) }

    private fun withPlayers(
        env: Env,
        factory: (PlayerConnection, GameProfile) -> Player,
        block: (Player, Player, TestConnection) -> Unit,
    ) {
        val connectionManager = env.process().connection()
        connectionManager.setPlayerProvider { connection, profile -> factory(connection, profile) }

        val instance = env.createFlatInstance()
        val players = mutableListOf<Player>()
        try {
            instance.loadChunk(0, 0).join()

            val (first, _) = connect(env, instance, "First").also { (player, _) -> players += player }
            val (second, secondConnection) = connect(env, instance, "Second").also { (player, _) -> players += player }

            awaitSpawn(env, first, second)
            assertTrue(second in first.viewers, "the players did not see each other to begin with")
            assertTrue(first in second.viewers, "the players did not see each other to begin with")

            block(first, second, secondConnection)
        } finally {
            players.forEach(Player::remove)
            connectionManager.setPlayerProvider(null)
            env.destroyInstance(instance)
        }
    }

    /** Waits for the spawn, which the harness completes off the test thread. */
    private fun awaitSpawn(env: Env, vararg players: Player) {
        repeat(SPAWN_ATTEMPTS) {
            if (players.all { player -> player.instance != null }) return
            env.tick()
            Thread.sleep(SPAWN_POLL_MILLIS)
        }

        fail<Nothing>("the players never spawned")
    }

    private fun connect(
        env: Env,
        instance: Instance,
        username: String,
    ): Pair<Player, TestConnection> {
        val connection = env.createConnection(GameProfile(UUID.randomUUID(), username))
        val player = onVirtualThread { connection.connect(instance, Pos(0.0, 42.0, 0.0)) }
        return player to connection
    }

    /** Logging a player in asserts that it runs on a virtual thread. */
    private fun <T> onVirtualThread(block: () -> T): T {
        val result = CompletableFuture<T>()

        Thread.startVirtualThread {
            runCatching(block)
                .onSuccess(result::complete)
                .onFailure(result::completeExceptionally)
        }

        return result.join()
    }

    private fun chatSession(): ChatSession {
        val key = KeyPairGenerator.getInstance("RSA").generateKeyPair().public
        val publicKey = PlayerPublicKey(Instant.now().plusSeconds(3600), key, ByteArray(0))
        return ChatSession(UUID.randomUUID(), publicKey)
    }

    private class ExposingPlayer(
        connection: PlayerConnection,
        profile: GameProfile,
    ) : SurfMinestomPlayer(connection, profile) {
        fun addPlayerToList(): PlayerInfoUpdatePacket = getAddPlayerToList()
    }

    private class SessionProvider(
        private val player: Player,
        private val session: ChatSession,
    ) : ChatSessionHolder.Provider {
        private val holder = object : ChatSessionHolder {
            override val player: Player get() = this@SessionProvider.player
            override val chatSession: ChatSession get() = session

            override fun sendSignedMessage(
                message: SignedMessage,
                boundName: Component,
                unsignedContent: Component?,
            ) = unsupported()

            override fun captureSignedMessage(
                message: SignedMessage,
                unsignedContent: Component?,
            ): RemoteSignedMessage? = unsupported()

            override fun sendRemoteSignedMessage(
                sender: RemoteChatSender,
                message: RemoteSignedMessage,
                boundName: Component,
            ) = unsupported()

            override fun deleteMessage(signature: SignedMessage.Signature) = unsupported()
        }

        override fun fromPlayer(player: Player): ChatSessionHolder? =
            holder.takeIf { player === this.player }

        override fun fromUuid(uuid: UUID): ChatSessionHolder? =
            holder.takeIf { uuid == player.uuid }

        override fun deleteMessage(player: Player, signature: SignedMessage.Signature) =
            unsupported()

        private fun unsupported(): Nothing = error("not needed by the test")
    }

    private companion object {
        const val GRANTED = "surf.test.granted"
        const val SPAWN_ATTEMPTS = 200
        const val SPAWN_POLL_MILLIS = 5L
    }
}
