package dev.slne.surf.api.minestom.server.impl.command.brigadier

import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.slne.surf.api.minestom.command.CommandAPI
import dev.slne.surf.api.minestom.command.CommandAPICommand
import dev.slne.surf.api.minestom.command.argument.StringArgument
import dev.slne.surf.api.minestom.command.dsl.anyExecutor
import dev.slne.surf.api.minestom.command.dsl.anyResultingExecutor
import dev.slne.surf.api.minestom.server.impl.command.MinestomCommandAPIPlatform
import dev.slne.surf.api.minestom.server.impl.command.MinestomCommandOwnership
import net.minestom.server.command.CommandManager
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

@EnvTest
class CommandApiExecuteTest {
    @Test
    fun `execute dispatches a registered command and rejects trailing input`(env: Env) =
        withPlatform(env) { manager ->
            val runs = AtomicInteger()
            CommandAPICommand("ping").anyExecutor { _, _ -> runs.incrementAndGet() }.register()

            assertEquals(1, CommandAPI.execute(manager.consoleSender, "ping"))
            assertEquals(1, runs.get())

            assertThrows(CommandSyntaxException::class.java) {
                CommandAPI.execute(manager.consoleSender, "ping extra")
            }
            assertEquals(1, runs.get())
        }

    @Test
    fun `execute returns a resulting executor's own value`(env: Env) = withPlatform(env) { manager ->
        CommandAPICommand("answer").anyResultingExecutor { _, _ -> 42 }.register()

        assertEquals(42, CommandAPI.execute(manager.consoleSender, "answer"))
    }

    @Test
    fun `dispatch goes through brigadier, not through minestom's parser`(env: Env) =
        withPlatform(env) { manager ->
            val runs = AtomicInteger()
            CommandAPICommand("owned").withArguments(StringArgument("value"))
                .anyExecutor { _, _ -> runs.incrementAndGet() }
                .register()

            assertEquals(1, CommandAPI.execute(manager.consoleSender, "owned thing"))
            assertEquals(1, runs.get())

            // Minestom would accept the trailing token and run the same syntax; Brigadier rejects it.
            assertThrows(CommandSyntaxException::class.java) {
                CommandAPI.execute(manager.consoleSender, "owned thing extra")
            }
            assertEquals(1, runs.get())
        }

    @Test
    fun `an unknown command is rejected`(env: Env) = withPlatform(env) { manager ->
        assertThrows(CommandSyntaxException::class.java) {
            CommandAPI.execute(manager.consoleSender, "nothing-here")
        }
    }

    @Test
    fun `unregistering removes the command from the dispatcher`(env: Env) =
        withPlatform(env) { manager ->
            CommandAPICommand("temporary").anyExecutor { _, _ -> }.register()
            assertEquals(1, CommandAPI.execute(manager.consoleSender, "temporary"))

            CommandAPI.unregister("temporary")

            assertThrows(CommandSyntaxException::class.java) {
                CommandAPI.execute(manager.consoleSender, "temporary")
            }
        }

    private fun withPlatform(env: Env, block: (CommandManager) -> Unit) {
        val manager = env.process().command()
        val platform = MinestomCommandAPIPlatform(manager, MinestomCommandOwnership())
        CommandAPI.installPlatform(platform)
        try {
            block(manager)
        } finally {
            platform.close()
            CommandAPI.uninstallPlatform(platform)
        }
    }
}
