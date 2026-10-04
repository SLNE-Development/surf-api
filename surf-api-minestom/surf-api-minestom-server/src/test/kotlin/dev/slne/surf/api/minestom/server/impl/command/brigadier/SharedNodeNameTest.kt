package dev.slne.surf.api.minestom.server.impl.command.brigadier

import dev.slne.surf.api.minestom.command.CommandAPI
import dev.slne.surf.api.minestom.command.dsl.anyExecutor
import dev.slne.surf.api.minestom.command.dsl.commandTree
import dev.slne.surf.api.minestom.command.dsl.literalArgument
import dev.slne.surf.api.minestom.command.dsl.stringArgument
import dev.slne.surf.api.minestom.command.executor.CommandArguments
import dev.slne.surf.api.minestom.server.impl.command.MinestomCommandAPIPlatform
import dev.slne.surf.api.minestom.server.impl.command.MinestomCommandOwnership
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicReference

@EnvTest
class SharedNodeNameTest {
    @Test
    fun `a literal and an argument share a name with no renaming`(env: Env) {
        val manager = env.process().command()
        val platform = MinestomCommandAPIPlatform(manager, MinestomCommandOwnership())
        CommandAPI.installPlatform(platform)
        try {
            val received = AtomicReference<CommandArguments>()
            commandTree("send") {
                literalArgument("server") {
                    stringArgument("server") { anyExecutor { _, args -> received.set(args) } }
                }
            }

            assertEquals(1, CommandAPI.execute(manager.consoleSender, "send server lobby-1"))
            assertEquals("lobby-1", received.get().get<String>("server"))
        } finally {
            platform.close()
            CommandAPI.uninstallPlatform(platform)
        }
    }
}
