package dev.slne.surf.api.minestom.server.impl.command

import dev.slne.surf.api.minestom.command.CommandAPI
import dev.slne.surf.api.minestom.command.CommandAPICommand
import dev.slne.surf.api.minestom.command.argument.StringArgument
import dev.slne.surf.api.minestom.command.dsl.anyExecutor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.translation.GlobalTranslator
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandResult
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.network.packet.server.play.SystemChatPacket
import net.minestom.testing.Collector
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import net.minestom.testing.TestConnection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertFalse

/**
 * The contract the `CommandManager` mixin relies on: an owned command answers with a result, and
 * anything else answers `null` so the manager keeps parsing it itself.
 */
@EnvTest
class CommandAPIHookTest {

    @Test
    fun `an owned command is dispatched through brigadier`(env: Env) {
        val runs = AtomicInteger()

        withPlatform(env) { player, _ ->
            CommandAPICommand("owned")
                .anyExecutor { _, _ -> runs.incrementAndGet() }
                .register()

            assertTrue(CommandAPIHook.owns("owned"))

            val result = CommandAPIHook.execute(player, "owned")

            assertEquals(CommandResult.Type.SUCCESS, result?.type)
            assertEquals(1, runs.get())
        }
    }

    @Test
    fun `a foreign minestom command is left to the manager`(env: Env) {
        withPlatform(env) { player, _ ->
            env.process().command().register(Command("foreign"))

            assertFalse(CommandAPIHook.owns("foreign"))
            assertNull(CommandAPIHook.execute(player, "foreign"))
            assertNull(CommandAPIHook.execute(player, "never-registered"))
        }
    }

    @Test
    fun `trailing input is reported with the vanilla syntax error`(env: Env) {
        val runs = AtomicInteger()

        withPlatform(env) { player, messages ->
            CommandAPICommand("difficulty")
                .withArguments(StringArgument("value"))
                .anyExecutor { _, _ -> runs.incrementAndGet() }
                .register()

            val result = CommandAPIHook.execute(player, "difficulty hard aaa")

            assertEquals(CommandResult.Type.INVALID_SYNTAX, result?.type)
            assertEquals(0, runs.get())
            messages.assertSingle { packet ->
                val rendered = render(packet)
                assertTrue(rendered.contains("Incorrect argument for command"), rendered)
                assertTrue(rendered.contains("<--[HERE]"), rendered)
            }
        }
    }

    @Test
    fun `an unknown command is reported the way vanilla reports it`(env: Env) {
        withPlatform(env) { player, messages ->
            CommandAPIHook.reportUnknown(player, "/nonsense")

            messages.assertSingle { packet ->
                val rendered = render(packet)
                assertTrue(rendered.contains("Unknown or incomplete command"), rendered)
                assertTrue(rendered.contains("nonsense<--[HERE]"), rendered)
            }
        }
    }

    private fun render(packet: SystemChatPacket): String {
        CommandAPITranslations.register()
        return PlainTextComponentSerializer.plainText()
            .serialize(GlobalTranslator.render(packet.message, Locale.US))
    }

    private fun withPlatform(env: Env, block: (Player, Collector<SystemChatPacket>) -> Unit) {
        val platform = MinestomCommandAPIPlatform(
            env.process().command(),
            MinestomCommandOwnership(),
        )
        CommandAPI.installPlatform(platform)

        val instance = env.createEmptyInstance()
        var player: Player? = null
        try {
            val connection = env.createConnection()
            player = connect(connection, instance)
            block(player, connection.trackIncoming(SystemChatPacket::class.java))
        } finally {
            player?.remove()
            env.destroyInstance(instance)
            platform.close()
            CommandAPI.uninstallPlatform(platform)
        }
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
