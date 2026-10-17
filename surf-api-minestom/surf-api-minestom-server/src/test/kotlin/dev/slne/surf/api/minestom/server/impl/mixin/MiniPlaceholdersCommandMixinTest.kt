package dev.slne.surf.api.minestom.server.impl.mixin

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.objectweb.asm.tree.MethodInsnNode

/**
 * Asserts that the compatibility mixin replaces the call MiniPlaceholders 3.2.0 makes against the
 * Minestom 26.2 signature of `ConnectionManager#getOnlinePlayers`.
 *
 * The mixin does not require its injection, so [ConfiguredMixinsApplyTest] alone would also pass
 * when the redirect matched nothing.
 */
class MiniPlaceholdersCommandMixinTest {

    @Test
    fun `the miniplaceholders command no longer calls the Minestom 26_2 signature`() {
        val command = MixinTestSupport.transform(MINESTOM_COMMAND)
        val calls = command.methods.single { it.name == "lambda\$new\$0" }
            .instructions.filterIsInstance<MethodInsnNode>()

        assertTrue(calls.none { it.owner == CONNECTION_MANAGER && it.name == "getOnlinePlayers" && it.desc == COLLECTION_DESCRIPTOR }) {
            "lambda\$new\$0 still calls getOnlinePlayers$COLLECTION_DESCRIPTOR"
        }
        assertTrue(calls.any { it.name.endsWith("surf\$onlinePlayers") }) {
            "lambda\$new\$0 does not call the redirect handler"
        }
    }

    private companion object {
        const val MINESTOM_COMMAND = "io.github.miniplaceholders.minestom.command.MinestomCommand"
        const val CONNECTION_MANAGER = "net/minestom/server/network/ConnectionManager"
        const val COLLECTION_DESCRIPTOR = "()Ljava/util/Collection;"
    }
}
