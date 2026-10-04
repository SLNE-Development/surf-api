package dev.slne.surf.api.minestom.server.npc

import codes.bed.minestom.npc.StomNPCs
import dev.slne.surf.api.minestom.npc.mannequinNpc
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.entity.PlayerSkin
import net.minestom.server.entity.metadata.avatar.MannequinMeta
import net.minestom.server.event.EventFilter
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerEntityInteractEvent
import net.minestom.server.instance.Instance
import net.minestom.server.network.player.ResolvableProfile
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import net.minestom.testing.TestConnection
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

@EnvTest
class MannequinNpcTest {

    @Test
    fun `mannequin npc spawns with skin, immovable meta and hologram`(env: Env) {
        val instance = env.createFlatInstance()
        instance.loadChunk(0, 0).join()

        val npc = mannequinNpc("test", instance, Pos(0.5, 42.0, 0.5)) {
            displayName = Component.text("Test NPC")
            profile = ResolvableProfile(PlayerSkin("textures", "signature"))
            scale = 1.5
            description = Component.empty()
        }

        assertEquals(EntityType.MANNEQUIN, npc.entityType)
        assertSame(instance, npc.instance)

        val meta = npc.entityMeta as MannequinMeta
        assertTrue(meta.isImmovable)
        assertNotNull(meta.profile)
        assertEquals(Component.empty(), meta.description)

        assertNotNull(npc.textDisplayController)
    }

    @Test
    fun `mannequin npc receives interactions through the npc listener`(env: Env) {
        val npcNode = EventNode.type("test-npcs", EventFilter.INSTANCE)
        env.process().eventHandler().addChild(npcNode)
        StomNPCs.initialize(npcNode)

        val instance = env.createFlatInstance()
        instance.loadChunk(0, 0).join()

        val interacted = AtomicReference<java.util.UUID>()
        val npc = mannequinNpc("test", instance, Pos(0.5, 42.0, 0.5)) {
            onInteract { interaction ->
                interacted.set(interaction.player.uuid)
            }
        }

        val player = connect(env.createConnection(), instance)
        env.process().eventHandler().call(
            PlayerEntityInteractEvent(player, npc, PlayerHand.MAIN, Vec.ZERO)
        )

        assertEquals(player.uuid, interacted.get())
    }

    @Test
    fun `mannequin npc attaches a single hologram when its chunk loads late`(env: Env) {
        val npcNode = EventNode.type("test-npcs-late-chunk", EventFilter.INSTANCE)
        env.process().eventHandler().addChild(npcNode)
        StomNPCs.initialize(npcNode)

        val instance = env.createFlatInstance()
        val failures = CopyOnWriteArrayList<Throwable>()
        env.process().exception().setExceptionHandler { failures += it }

        val npc = mannequinNpc("late", instance, Pos(500.5, 42.0, 500.5)) {
            displayName = Component.text("Late NPC")
        }

        fun holograms() = instance.entities.filter { it.entityType == EntityType.TEXT_DISPLAY }

        assertTrue(
            env.tickWhile({ holograms().isEmpty() }, Duration.ofSeconds(5)),
            "the hologram was never spawned"
        )

        val displays = holograms()
        assertEquals(1, displays.size, "expected exactly one hologram, got $displays")
        assertTrue(npc in instance.entities, "the npc itself was not registered")
        assertSame(npc.textDisplayController?.getEntity(), displays.single())
        assertTrue(failures.isEmpty(), "spawning logged exceptions: $failures")
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
