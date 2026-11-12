package dev.slne.surf.api.minestom.server.impl.player

import dev.slne.surf.api.minestom.player.SurfMinestomPlayer
import dev.slne.surf.api.minestom.server.SurfMinestomServerBuilder
import dev.slne.surf.api.minestom.server.surfMinestomServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.network.player.GameProfile
import net.minestom.server.network.player.PlayerConnection
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Tests that the surf api creates every player with the factory of the server and that
 * `ConnectionManagerMixin` keeps other player providers out while the api runs.
 */
@EnvTest
class SurfPlayerProvidersTest {

    @Test
    fun `in tests the api replaces the player provider of the test environment`(env: Env) = withApi {
        assertInstanceOf(SurfMinestomPlayer::class.java, env.spawnPlayer())
    }

    @Test
    fun `players are created with the factory of the server`(env: Env) =
        withApi({ playerFactory(::CustomPlayer) }) {
            assertInstanceOf(CustomPlayer::class.java, env.spawnPlayer())
        }

    @Test
    fun `Minestom refuses another player provider while the api runs`(env: Env) = withApi {
        val connections = env.process().connection()

        val failure = assertThrows<IllegalStateException> {
            connections.setPlayerProvider { connection, profile -> Player(connection, profile) }
        }
        assertTrue("playerFactory" in failure.message.orEmpty(), failure.message)
        assertThrows<IllegalStateException> { connections.setPlayerProvider(null) }

        assertInstanceOf(SurfMinestomPlayer::class.java, env.spawnPlayer())
    }

    @Test
    fun `outside of tests a player provider installed through Minestom fails the start`(env: Env) {
        // The test environment installed a player provider of its own
        val failure = assertThrows<IllegalStateException> {
            SurfPlayerProviders.install(::SurfMinestomPlayer, replaceForeignProvider = false)
        }

        assertTrue("playerFactory" in failure.message.orEmpty(), failure.message)
    }

    @Test
    fun `Minestom accepts player providers again once the api is shut down`(env: Env) {
        val connections = env.process().connection()
        withApi {}

        connections.setPlayerProvider(::CustomPlayer)
        try {
            assertInstanceOf(CustomPlayer::class.java, env.spawnPlayer())
        } finally {
            connections.setPlayerProvider(null)
        }
    }

    private fun withApi(
        configure: SurfMinestomServerBuilder.() -> Unit = {},
        block: () -> Unit,
    ) {
        val api = surfMinestomServer(configure)
        try {
            block()
        } finally {
            api.shutdown()
        }
    }

    private fun Env.spawnPlayer(): Player = createPlayer(createFlatInstance(), Pos.ZERO)

    private class CustomPlayer(
        connection: PlayerConnection,
        profile: GameProfile,
    ) : SurfMinestomPlayer(connection, profile)
}
