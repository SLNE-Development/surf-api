package dev.slne.surf.api.minestom.highlight

import dev.slne.surf.api.minestom.highlight.BlockHighlights
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import net.minestom.testing.TestConnection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

@EnvTest
class BlockHighlightsTest {

    @Test
    fun `a highlight is drawn as a glowing block display where the block sits`(env: Env) {
        val instance = env.createFlatInstance()
        val viewer = connect(env.createConnection(), instance)

        try {
            val position = instance.prepare(1, 40, 1)
            instance.setBlock(position, Block.RED_CONCRETE)

            BlockHighlights.show(viewer, instance, position, NamedTextColor.WHITE)

            val display = blockDisplays(instance).single()

            assertEquals(Pos(1.0, 40.0, 1.0), display.position)
            assertTrue(display.isGlowing)
        } finally {
            cleanUp(env, instance, viewer)
        }
    }

    @Test
    fun `only the player a highlight was shown to sees it`(env: Env) {
        val instance = env.createFlatInstance()
        val viewer = connect(env.createConnection(), instance)
        val other = connect(env.createConnection(), instance)

        try {
            BlockHighlights.show(viewer, instance, instance.prepare(2, 40, 2))

            val display = blockDisplays(instance).single()

            assertTrue(display.viewers.contains(viewer))
            assertFalse(display.viewers.contains(other))
        } finally {
            cleanUp(env, instance, viewer, other)
        }
    }

    @Test
    fun `hiding a highlight takes its display away again`(env: Env) {
        val instance = env.createFlatInstance()
        val viewer = connect(env.createConnection(), instance)

        try {
            val position = instance.prepare(3, 40, 3)
            BlockHighlights.show(viewer, instance, position)

            assertTrue(BlockHighlights.hide(viewer, instance, position))
            assertEquals(0, BlockHighlights.count(viewer))
            assertTrue(blockDisplays(instance).isEmpty())
        } finally {
            cleanUp(env, instance, viewer)
        }
    }

    @Test
    fun `hiding a highlight nobody was shown reports nothing was taken back`(env: Env) {
        val instance = env.createFlatInstance()
        val viewer = connect(env.createConnection(), instance)

        try {
            assertFalse(BlockHighlights.hide(viewer, instance, instance.prepare(4, 40, 4)))
        } finally {
            cleanUp(env, instance, viewer)
        }
    }

    @Test
    fun `showing a highlight twice keeps a single display`(env: Env) {
        val instance = env.createFlatInstance()
        val viewer = connect(env.createConnection(), instance)

        try {
            val position = instance.prepare(5, 40, 5)

            BlockHighlights.show(viewer, instance, position)
            BlockHighlights.show(viewer, instance, position)

            assertEquals(1, BlockHighlights.count(viewer))
            assertEquals(1, blockDisplays(instance).size)
        } finally {
            cleanUp(env, instance, viewer)
        }
    }

    @Test
    fun `any point inside a block addresses the same highlight`(env: Env) {
        val instance = env.createFlatInstance()
        val viewer = connect(env.createConnection(), instance)

        try {
            instance.prepare(6, 40, 6)
            BlockHighlights.show(viewer, instance, Pos(6.7, 40.2, 6.9))

            assertTrue(BlockHighlights.hide(viewer, instance, Pos(6.0, 40.0, 6.0)))
            assertEquals(0, BlockHighlights.count(viewer))
        } finally {
            cleanUp(env, instance, viewer)
        }
    }

    @Test
    fun `taking back everything a player was shown leaves nothing behind`(env: Env) {
        val instance = env.createFlatInstance()
        val viewer = connect(env.createConnection(), instance)

        try {
            BlockHighlights.show(viewer, instance, instance.prepare(7, 40, 7))
            BlockHighlights.show(viewer, instance, instance.prepare(8, 40, 8))

            assertEquals(2, blockDisplays(instance).size)
            assertEquals(2, BlockHighlights.hideAll(viewer))
            assertEquals(0, BlockHighlights.count(viewer))
            assertTrue(blockDisplays(instance).isEmpty())
        } finally {
            cleanUp(env, instance, viewer)
        }
    }

    /**
     * Loads the chunk holding the given block, so that a highlight shown there joins the instance
     * straight away instead of once the chunk arrives.
     */
    private fun Instance.prepare(x: Int, y: Int, z: Int): Point {
        val position = Pos(x.toDouble(), y.toDouble(), z.toDouble())
        loadChunk(position).join()
        return position
    }

    private fun blockDisplays(instance: Instance) =
        instance.entities.filter { it.entityType == EntityType.BLOCK_DISPLAY }

    private fun cleanUp(env: Env, instance: Instance, vararg players: Player) {
        players.forEach {
            BlockHighlights.hideAll(it)
            it.remove()
        }
        env.destroyInstance(instance)
    }

    private fun connect(connection: TestConnection, instance: Instance): Player {
        val player = CompletableFuture<Player>()
        Thread.startVirtualThread {
            runCatching { connection.connect(instance, Pos.ZERO) }
                .onSuccess(player::complete)
                .onFailure(player::completeExceptionally)
        }
        return player.join()
    }
}
