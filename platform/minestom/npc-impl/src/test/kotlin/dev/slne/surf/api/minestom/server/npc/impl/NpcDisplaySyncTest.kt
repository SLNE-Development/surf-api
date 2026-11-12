package dev.slne.surf.api.minestom.server.npc.impl

import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.instance.Instance
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EnvTest
class NpcDisplaySyncTest {

    private val offset = Vec(0.0, 2.15, 0.0)

    @Test
    fun `a missing auxiliary entity is never synchronized`(env: Env) {
        assertFalse(NpcDisplaySync.isSynchronized(null, Entity(EntityType.ARMOR_STAND), offset))
    }

    @Test
    fun `an auxiliary entity on the offset position is synchronized`(env: Env) {
        val instance = env.createFlatInstance()
        val npcPosition = Pos(0.5, 42.0, 0.5, 37.5f, -12.25f)
        val npc = spawn(instance, npcPosition)
        val display = spawn(instance, npcPosition.add(offset))

        assertTrue(NpcDisplaySync.isSynchronized(display, npc, offset))
    }

    @Test
    fun `moving or rotating the npc desynchronizes the auxiliary entity`(env: Env) {
        val instance = env.createFlatInstance()
        val npcPosition = Pos(0.5, 42.0, 0.5)
        val npc = spawn(instance, npcPosition)
        val display = spawn(instance, npcPosition.add(offset))

        npc.teleport(npcPosition.add(1.0, 0.0, 0.0)).join()
        assertFalse(NpcDisplaySync.isSynchronized(display, npc, offset))

        npc.teleport(npcPosition).join()
        assertTrue(NpcDisplaySync.isSynchronized(display, npc, offset))

        npc.setView(90f, 0f)
        assertFalse(NpcDisplaySync.isSynchronized(display, npc, offset))
    }

    @Test
    fun `a different offset desynchronizes the auxiliary entity`(env: Env) {
        val instance = env.createFlatInstance()
        val npcPosition = Pos(0.5, 42.0, 0.5)
        val npc = spawn(instance, npcPosition)
        val display = spawn(instance, npcPosition.add(offset))

        assertFalse(NpcDisplaySync.isSynchronized(display, npc, Vec(0.0, 3.4, 0.0)))
    }

    private fun spawn(instance: Instance, position: Pos): Entity {
        val entity = Entity(EntityType.ARMOR_STAND)
        entity.setInstance(instance, position).join()
        return entity
    }
}
