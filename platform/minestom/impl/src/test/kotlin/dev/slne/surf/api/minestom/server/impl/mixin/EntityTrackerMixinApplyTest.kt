package dev.slne.surf.api.minestom.server.impl.mixin

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.objectweb.asm.tree.MethodInsnNode

/**
 * Applies the configured mixins to the shipped `EntityTracker` bytecode through the service the
 * production agent uses, so a bytecode change that breaks an injection point fails here instead of
 * silently at runtime. The transformer runs directly; no transformed class is loaded.
 */
class EntityTrackerMixinApplyTest {

    @Test
    fun `the entity tracker mixins apply to the shipped minestom bytecode`() {
        val impl = MixinTestSupport.transform("net.minestom.server.instance.EntityTrackerImpl")
        val entry = MixinTestSupport.transform(
            "net.minestom.server.instance.EntityTrackerImpl\$EntityTrackerEntry",
        )

        // Mixin decorates handler names, e.g. handler$zzd000$surf$directNearbyScan.
        val implMethods = impl.methods.map { it.name }
        assertTrue(implMethods.any { it.endsWith("surf\$directNearbyScan") }) {
            "fast-path inject missing: $implMethods"
        }
        assertTrue(implMethods.any { it.endsWith("surf\$rectangularChunkScan") }) {
            "chunk-scan redirect missing: $implMethods"
        }

        val nearby = impl.methods.single { it.name == "nearbyEntitiesByChunkRange" }
        val calls = nearby.instructions.asSequence()
            .filterIsInstance<MethodInsnNode>()
            .map { "${it.owner}.${it.name}" }
            .toList()
        assertTrue(calls.none { it == "net/minestom/server/coordinate/ChunkRange.chunksInRange" }) {
            "the spiral traversal was not redirected: $calls"
        }
        assertTrue(calls.any { it.endsWith("surf\$rectangularChunkScan") }) {
            "the rectangular redirect handler is not called: $calls"
        }
        assertTrue(calls.any { it.endsWith("surf\$directNearbyScan") }) {
            "the fast-path injection is not called: $calls"
        }

        assertTrue("dev/slne/surf/api/minestom/server/impl/mixin/duck/EntityTrackerEntryDuck" in entry.interfaces) {
            "EntityTrackerEntry does not implement the duck: ${entry.interfaces}"
        }
        assertTrue(entry.methods.any { it.name == "surf\$lastPosition" }) {
            "the lastPosition accessor was not generated"
        }
    }
}
