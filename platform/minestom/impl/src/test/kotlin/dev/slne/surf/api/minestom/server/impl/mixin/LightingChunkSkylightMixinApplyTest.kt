package dev.slne.surf.api.minestom.server.impl.mixin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.objectweb.asm.tree.MethodInsnNode

class LightingChunkSkylightMixinApplyTest {

    @Test
    fun `createLightData reads its sky light through the redirect`() {
        val chunk = MixinTestSupport.transform("net.minestom.server.instance.LightingChunk")

        val methods = chunk.methods.map { it.name }
        assertTrue(methods.any { it.endsWith("surf\$skipDisabledSkyLight") }) {
            "the sky light redirect handler is missing: $methods"
        }

        val calls = chunk.methods
            .single { it.name == "createLightData" }
            .instructions
            .asSequence()
            .filterIsInstance<MethodInsnNode>()
            .map { "${it.owner}.${it.name}" }
            .toList()

        assertEquals(0, calls.count { it == "net/minestom/server/instance/Section.skyLight" }) {
            "createLightData still reads a section's own sky light: $calls"
        }
        assertTrue(calls.any { it.endsWith("surf\$skipDisabledSkyLight") }) {
            "expected the sky light reads to be redirected: $calls"
        }
        assertTrue(calls.any { it == "net/minestom/server/instance/Section.blockLight" }) {
            "block light must be untouched: $calls"
        }
        assertTrue(calls.any { it.endsWith(".relightSection") }) {
            "createLightData lost its relight, the redirect should only skip the sky one: $calls"
        }
    }
}
