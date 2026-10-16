package dev.slne.surf.api.minestom.server.impl.command

import dev.slne.surf.api.minestom.command.CommandAPI
import dev.slne.surf.api.minestom.command.CommandAPICommand
import dev.slne.surf.api.minestom.command.argument.IntegerArgument
import dev.slne.surf.api.minestom.command.dsl.anyExecutor
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandResult
import net.minestom.server.coordinate.Pos
import net.minestom.server.network.packet.server.play.DeclareCommandsPacket
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tests that `CommandManagerMixin` routes CommandAPI commands through Minestom's command manager.
 */
@EnvTest
class CommandManagerMixinTest {

    @Test
    fun `the command manager runs a CommandAPI command`(env: Env) {
        val manager = env.process().command()
        withPlatform(env) {
            val received = AtomicInteger(-1)

            CommandAPICommand("dispatched")
                .withAliases("dispatch")
                .withArguments(IntegerArgument("amount"))
                .anyExecutor { _, args -> received.set(args.get<Int>("amount")) }
                .register()

            assertTrue(manager.commandExists("dispatched"))
            assertTrue(manager.commandExists("dispatch"))

            val result = manager.execute(manager.consoleSender, "dispatched 7")
            assertEquals(CommandResult.Type.SUCCESS, result.type)
            assertEquals(7, received.get())

            manager.execute(manager.consoleSender, "dispatch 9")
            assertEquals(9, received.get())
        }
    }

    @Test
    fun `an unregistered command is unknown to the command manager`(env: Env) {
        val manager = env.process().command()
        withPlatform(env) {
            CommandAPICommand("temporary")
                .anyExecutor { _, _ -> }
                .register()

            assertTrue(manager.commandExists("temporary"))
            assertNull(manager.getCommand("temporary"), "the command is not registered as a Minestom command")
            assertTrue(CommandAPI.unregister("temporary"))
            assertFalse(manager.commandExists("temporary"))
            assertEquals(CommandResult.Type.UNKNOWN, manager.execute(manager.consoleSender, "temporary").type)
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
    fun `the declared commands contain the CommandAPI tree`(env: Env) {
        withPlatform(env) {
            CommandAPICommand("declared")
                .withArguments(IntegerArgument("amount"))
                .anyExecutor { _, _ -> }
                .register()

            val packet = minestomDeclaration(env)

            val rootChildren = packet.nodes[packet.rootIndex].children.map { packet.nodes[it] }
            val declared = rootChildren.filter { it.name == "declared" }
            assertEquals(1, declared.size)

            val arguments = declared.single().children.map { packet.nodes[it].name }
            assertEquals(listOf("amount"), arguments)
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
