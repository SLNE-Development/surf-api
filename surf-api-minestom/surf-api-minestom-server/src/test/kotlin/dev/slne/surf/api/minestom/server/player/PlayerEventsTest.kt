package dev.slne.surf.api.minestom.server.player

import dev.slne.surf.api.minestom.extension.addListener
import dev.slne.surf.api.minestom.player.PlayerLimit
import dev.slne.surf.api.minestom.server.impl.player.PlayerEvents
import dev.slne.surf.api.minestom.player.event.AsyncPlayerCountEvent
import dev.slne.surf.api.minestom.player.event.AsyncPlayerSpawnLocationEvent
import dev.slne.surf.api.minestom.player.event.PlayerJoinEvent
import dev.slne.surf.api.minestom.server.surfMinestomServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

@EnvTest
class PlayerEventsTest {

    @Test
    fun `a player beyond the limit is refused unless a listener lets them in`(env: Env) {
        val instance = env.createFlatInstance()
        val first = env.createPlayer(instance, Pos.ZERO)
        val second = env.createPlayer(instance, Pos.ZERO)
        val node = EventNode.all("player-count-test")
        env.process().eventHandler().addChild(node)
        PlayerLimit.maxPlayers = 1

        try {
            val results = mutableListOf<AsyncPlayerCountEvent.Result>()
            node.addListener<AsyncPlayerCountEvent> { event ->
                results += event.result
                if (event.player === first) event.allow()
            }

            assertTrue(onVirtualThread { PlayerEvents.admit(first) })
            assertFalse(onVirtualThread { PlayerEvents.admit(second) })
            assertEquals(
                listOf(AsyncPlayerCountEvent.Result.KICK_FULL, AsyncPlayerCountEvent.Result.KICK_FULL),
                results,
            )
        } finally {
            PlayerLimit.maxPlayers = null
            env.process().eventHandler().removeChild(node)
        }
    }

    @Test
    fun `everyone is admitted without a limit`(env: Env) {
        val instance = env.createFlatInstance()
        val player = env.createPlayer(instance, Pos.ZERO)

        assertNull(PlayerLimit.maxPlayers)
        assertFalse(PlayerLimit.isFull)
        assertTrue(onVirtualThread { PlayerEvents.admit(player) })
    }

    @Test
    fun `the spawn location event decides where the player spawns`(env: Env) {
        val instance = env.createFlatInstance()
        val other = env.createFlatInstance()
        val player = env.createPlayer(instance, Pos.ZERO)
        val node = EventNode.all("spawn-location-test")
        env.process().eventHandler().addChild(node)

        try {
            node.addListener<AsyncPlayerSpawnLocationEvent> { event ->
                assertSame(instance, event.instance)
                event.instance = other
                event.position = Pos(1.0, 2.0, 3.0)
            }

            val configuration = AsyncPlayerConfigurationEvent(player, true)
            configuration.spawningInstance = instance
            onVirtualThread { PlayerEvents.chooseSpawnLocation(configuration) }

            assertSame(other, configuration.spawningInstance)
            assertEquals(Pos(1.0, 2.0, 3.0), player.respawnPoint)
        } finally {
            env.process().eventHandler().removeChild(node)
        }
    }

    @Test
    fun `joining players are announced through the join event`(env: Env) {
        val joined = mutableListOf<String>()
        val node = EventNode.all("join-test")
        node.addListener<PlayerJoinEvent> { event ->
            joined += event.player.username
            event.joinMessage = null
        }
        env.process().eventHandler().addChild(node)

        val api = surfMinestomServer()
        try {
            val instance = env.createFlatInstance()
            val player = env.createPlayer(instance, Pos.ZERO)
            assertEquals(listOf(player.username), joined)
        } finally {
            api.shutdown()
            env.process().eventHandler().removeChild(node)
        }
    }

    /** Async events have to be called on a virtual thread, like the configuration runs on. */
    private fun <T> onVirtualThread(block: () -> T): T {
        val result = CompletableFuture<T>()
        Thread.startVirtualThread {
            runCatching(block).fold(result::complete, result::completeExceptionally)
        }
        return result.join()
    }
}
