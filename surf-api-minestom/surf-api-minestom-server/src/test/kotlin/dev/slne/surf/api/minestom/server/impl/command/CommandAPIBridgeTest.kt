package dev.slne.surf.api.minestom.server.impl.command

import dev.slne.surf.api.minestom.command.CommandAPI
import dev.slne.surf.api.minestom.command.CommandAPICommand
import dev.slne.surf.api.minestom.command.argument.IntegerArgument
import dev.slne.surf.api.minestom.command.dsl.anyExecutor
import net.minestom.server.command.builder.Command
import net.minestom.server.coordinate.Pos
import net.minestom.server.command.builder.CommandResult
import net.minestom.server.network.packet.server.play.DeclareCommandsPacket
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * CommandAPI commands are reached through Minestom's own command manager by a bridge command.
 */
@EnvTest
class CommandAPIBridgeTest {

    @Test
    fun `the command manager runs a CommandAPI command through its bridge`(env: Env) {
        val manager = env.process().command()
        withPlatform(env) {
            val received = AtomicInteger(-1)

            CommandAPICommand("bridged")
                .withAliases("bridge")
                .withArguments(IntegerArgument("amount"))
                .anyExecutor { _, args -> received.set(args.get<Int>("amount")) }
                .register()

            assertTrue(manager.commandExists("bridged"))
            assertTrue(manager.commandExists("bridge"))

            val result = manager.execute(manager.consoleSender, "bridged 7")
            assertEquals(CommandResult.Type.SUCCESS, result.type)
            assertEquals(7, received.get())

            manager.execute(manager.consoleSender, "bridge 9")
            assertEquals(9, received.get())
        }
    }

    @Test
    fun `unregistering a command removes its bridge`(env: Env) {
        val manager = env.process().command()
        withPlatform(env) {
            CommandAPICommand("temporary")
                .anyExecutor { _, _ -> }
                .register()

            assertNotNull(manager.getCommand("temporary"))
            assertTrue(CommandAPI.unregister("temporary"))
            assertNull(manager.getCommand("temporary"))
        }
    }

    @Test
    fun `a name taken by a Minestom command is rejected`(env: Env) {
        val manager = env.process().command()
        val foreign = Command("foreign")
        manager.register(foreign)
        try {
            withPlatform(env) {
                val failure = runCatching {
                    CommandAPICommand("foreign").anyExecutor { _, _ -> }.register()
                }
                assertTrue(failure.isFailure)
                assertEquals(foreign, manager.getCommand("foreign"))
            }
        } finally {
            manager.unregister(foreign)
        }
    }

    @Test
    fun `the declared tree replaces the bridge node with the real one`(env: Env) {
        val manager = env.process().command()
        withPlatform(env) {
            CommandAPICommand("declared")
                .withArguments(IntegerArgument("amount"))
                .anyExecutor { _, _ -> }
                .register()

            val original = minestomDeclaration(env)
            val merger = checkNotNull(MinestomCommandAPIPlatform.activeMerger())
            val merged = merger.merge(original, manager.consoleSender)

            val rootChildren = merged.nodes[merged.rootIndex].children.map { merged.nodes[it] }
            val declared = rootChildren.filter { it.name == "declared" }
            assertEquals(1, declared.size, "only the CommandAPI's node is reachable from the root")

            val arguments = declared.single().children.map { merged.nodes[it].name }
            assertEquals(listOf("amount"), arguments)
            assertFalse("arguments" in arguments, "the bridge's greedy argument is not declared")
        }
    }

    private fun minestomDeclaration(env: Env): DeclareCommandsPacket {
        val instance = env.createFlatInstance()
        val player = env.createPlayer(instance, Pos.ZERO)
        try {
            return env.process().command().createDeclareCommandsPacket(player)
        } finally {
            player.remove()
            env.destroyInstance(instance)
        }
    }

    private fun withPlatform(env: Env, block: () -> Unit) {
        val platform = MinestomCommandAPIPlatform(env.process().command(), MinestomCommandOwnership())
        CommandAPI.installPlatform(platform)
        try {
            block()
        } finally {
            platform.close()
            CommandAPI.uninstallPlatform(platform)
        }
    }
}
