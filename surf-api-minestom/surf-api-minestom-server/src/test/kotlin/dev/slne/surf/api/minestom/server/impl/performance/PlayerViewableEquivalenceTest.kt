package dev.slne.surf.api.minestom.server.impl.performance

import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.instance.EntityTracker
import net.minestom.server.instance.Instance
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.network.player.GameProfile
import net.minestom.server.property.ServerProperties
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * Proves the direct scan plus entity-id dedup reproduces vanilla `ChunkView` viewer resolution
 * for real players, including players living in a `SharedInstance`.
 */
@EnvTest
class PlayerViewableEquivalenceTest {

    @Test
    fun `viewable players match a deduplicated direct scan across main and shared instances`(env: Env) {
        val instance = env.createFlatInstance() as InstanceContainer
        instance.loadChunk(0, 0).join()
        val shared = env.process().instance().createSharedInstance(instance)

        val near = connect(env, instance, "Near", Pos(8.0, 42.0, 8.0))
        val nearEdge = connect(env, instance, "NearEdge", Pos(-1.0, 42.0, 8.0))
        val far = connect(env, instance, "Far", Pos(300.0, 42.0, 300.0))
        val sharedNear = connect(env, shared, "SharedNear", Pos(24.0, 42.0, 8.0))
        awaitSpawn(env, near, nearEdge, far, sharedNear)
        check(far.position.chunkX() > ServerProperties.CHUNK_VIEW_DISTANCE.get())

        val viewable = instance.entityTracker.viewable(listOf(shared), 0, 0)
        val vanillaViewers = viewable.viewers.map { it.entityId }.toSet()

        val direct = HashMap<Int, Player>()
        val resolver = EntityViewerLookup.EntityPositionResolver { entity -> entity.position }
        for (tracker in listOf(instance.entityTracker, shared.entityTracker)) {
            EntityViewerLookup.directScan(
                tracker.entities(EntityTracker.Target.PLAYERS),
                resolver,
                0,
                0,
                ServerProperties.CHUNK_VIEW_DISTANCE.get(),
            ) { player -> direct.putIfAbsent(player.entityId, player) }
        }

        assertEquals(vanillaViewers, direct.keys)
        assertEquals(
            setOf(near.entityId, nearEdge.entityId, sharedNear.entityId),
            direct.keys,
            "expected exactly the players within view distance of chunk (0, 0)",
        )
    }

    private fun connect(env: Env, instance: Instance, username: String, pos: Pos): Player {
        val connection = env.createConnection(GameProfile(UUID.randomUUID(), username))
        return onVirtualThread { connection.connect(instance, pos) }
    }

    /** The harness completes spawns off the test thread; poll before touching trackers. */
    private fun awaitSpawn(env: Env, vararg players: Player) {
        repeat(SPAWN_ATTEMPTS) {
            if (players.all { player -> player.instance != null }) return
            env.tick()
            Thread.sleep(SPAWN_POLL_MILLIS)
        }
        fail<Nothing>("the players never spawned")
    }

    private fun <T> onVirtualThread(block: () -> T): T {
        val result = CompletableFuture<T>()

        Thread.startVirtualThread {
            runCatching(block)
                .onSuccess(result::complete)
                .onFailure(result::completeExceptionally)
        }

        return result.join()
    }

    private companion object {
        const val SPAWN_ATTEMPTS = 200
        const val SPAWN_POLL_MILLIS = 5L
    }
}
