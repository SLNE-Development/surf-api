package dev.slne.surf.api.minestom.server.npc.mixin

import dev.slne.surf.api.minestom.server.impl.mixin.MixinTransforms
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.objectweb.asm.tree.MethodInsnNode

/**
 * Applies the StomNPCs mixins to the dependency bytecode. Production wraps a failed transform in an
 * `IllegalClassFormatException` that the JVM swallows, so a renamed field or a changed
 * `syncWithNpc` descriptor would silently load the untransformed controller.
 */
class NpcDisplaySyncApplyTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        "codes.bed.minestom.npc.display.TextDisplayController, surf\$skipSynchronizedDisplay",
        "codes.bed.minestom.npc.display.InteractionController, surf\$skipSynchronizedHitbox",
    )
    fun `the controller mixins guard syncWithNpc`(binaryName: String, handler: String) {
        val controller = MixinTransforms.transform(binaryName)

        // Mixin decorates handler names, e.g. handler$zzd000$surf$skipSynchronizedDisplay.
        val methods = controller.methods.map { it.name }
        assertTrue(methods.any { it.endsWith(handler) }) {
            "the head guard was not injected into $binaryName: $methods"
        }

        val sync = controller.methods.single { it.name == "syncWithNpc" }
        val calls = sync.instructions.asSequence()
            .filterIsInstance<MethodInsnNode>()
            .map { "${it.owner}.${it.name}" }
            .toList()
        assertTrue(calls.any { it.endsWith(handler) }) {
            "syncWithNpc does not call the guard: $calls"
        }
        assertTrue(calls.any { it.endsWith("Entity.teleport") }) {
            "syncWithNpc lost its teleport, the guard should only skip it: $calls"
        }
    }

    @Test
    fun `the shadowed controller fields still exist in the dependency`() {
        val fields = MixinTransforms
            .transform("codes.bed.minestom.npc.display.TextDisplayController")
            .fields
            .associate { it.name to it.desc }

        assertTrue(fields["entity"] == "Lnet/minestom/server/entity/Entity;") { "fields: $fields" }
        assertTrue(fields["offset"] == "Lnet/minestom/server/coordinate/Vec;") { "fields: $fields" }
    }
}
