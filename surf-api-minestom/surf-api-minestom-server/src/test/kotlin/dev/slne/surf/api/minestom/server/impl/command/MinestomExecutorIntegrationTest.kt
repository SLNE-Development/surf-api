package dev.slne.surf.api.minestom.server.impl.command

import dev.slne.surf.api.minestom.command.CommandAPI
import dev.slne.surf.api.minestom.command.CommandAPICommand
import dev.slne.surf.api.minestom.command.argument.EnumArgument
import dev.slne.surf.api.minestom.command.argument.IntegerArgument
import dev.slne.surf.api.minestom.command.argument.LiteralArgument
import dev.slne.surf.api.minestom.command.argument.TextArgument
import dev.slne.surf.api.minestom.command.exception.CommandSyntaxException
import dev.slne.surf.api.minestom.command.exception.WrapperCommandSyntaxException
import dev.slne.surf.api.minestom.command.executor.GENERIC_COMMAND_FAILURE
import dev.slne.surf.api.minestom.command.dsl.anyExecutionInfo
import dev.slne.surf.api.minestom.command.dsl.anyExecutionInfoExecutorSuspend
import dev.slne.surf.api.minestom.command.dsl.anyExecutor
import dev.slne.surf.api.minestom.command.dsl.anyResultingExecutor
import dev.slne.surf.api.minestom.command.dsl.consoleExecutor
import dev.slne.surf.api.minestom.command.dsl.playerExecutor
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import net.kyori.adventure.identity.Identity
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.CommandResult
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.network.packet.server.play.SystemChatPacket
import net.minestom.server.tag.TagHandler
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import net.minestom.testing.TestConnection
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.layout.PatternLayout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

@EnvTest
class MinestomExecutorIntegrationTest {
    @Test
    fun `player and console executors win before any executor`(env: Env) {
        withPlatform(env) { manager ->
            val calls = mutableListOf<Pair<String, CommandSender>>()
            CommandAPICommand("dispatch-specific")
                .anyExecutor { sender, _ -> calls += "any" to sender }
                .playerExecutor { sender, _ -> calls += "player" to sender }
                .consoleExecutor { sender, _ -> calls += "console" to sender }
                .register()

            val instance = env.createEmptyInstance()
            var player: Player? = null
            try {
                player = connect(env.createConnection(), instance)

                assertTrue(runCommand(player, "dispatch-specific"))
                assertTrue(runCommand(manager.consoleSender, "dispatch-specific"))
                assertEquals(listOf("player", "console"), calls.map(Pair<String, CommandSender>::first))
                assertSame(player, calls[0].second)
                assertSame(manager.consoleSender, calls[1].second)
            } finally {
                player?.remove()
                env.destroyInstance(instance)
            }
        }
    }

    @Test
    fun `any executor is the fallback for players consoles and other senders`(env: Env) {
        withPlatform(env) { manager ->
            val calls = mutableListOf<CommandSender>()
            CommandAPICommand("dispatch-any")
                .anyExecutor { sender, _ -> calls += sender }
                .register()
            val other = RecordingSender()
            val instance = env.createEmptyInstance()
            var player: Player? = null
            try {
                player = connect(env.createConnection(), instance)

                assertTrue(runCommand(player, "dispatch-any"))
                assertTrue(runCommand(manager.consoleSender, "dispatch-any"))
                assertTrue(runCommand(other, "dispatch-any"))
                assertEquals(listOf(player, manager.consoleSender, other), calls)
            } finally {
                player?.remove()
                env.destroyInstance(instance)
            }
        }
    }

    @Test
    fun `execution info snapshots the full path with converted values raw input delegates and defaults`(env: Env) {
        withPlatform(env) { manager ->
            val received = AtomicReference<Snapshot>()
            CommandAPICommand("inspect")
                .withArguments(
                    LiteralArgument("action", "run"),
                    EnumArgument("mode", listOf(SnapshotMode.READY)) { "ready-now" },
                    TextArgument("message"),
                )
                .withOptionalArguments(IntegerArgument("count").setOptional(7))
                .anyExecutionInfo { info ->
                    val delegated = DelegatedArguments(info.args)
                    received.set(
                        Snapshot(
                            input = info.input,
                            indexed = listOf(info.args[0], info.args[1], info.args[2]),
                            mode = delegated.mode,
                            message = delegated.message,
                            count = delegated.count,
                            actionRaw = info.args.getRaw("action"),
                            messageRaw = info.args.getRaw("message"),
                            countRaw = info.args.getRaw("count"),
                            present = listOf(
                                "action" in info.args,
                                "mode" in info.args,
                                "message" in info.args,
                                "count" in info.args,
                            ),
                            rawArguments = info.args.rawArguments(),
                        ),
                    )
                }
                .register()

            assertTrue(runCommand(manager.consoleSender, "inspect run ready-now \"hello world\""))

            assertEquals(
                Snapshot(
                    input = "inspect run ready-now \"hello world\"",
                    indexed = listOf(SnapshotMode.READY, "hello world", 7),
                    mode = SnapshotMode.READY,
                    message = "hello world",
                    count = 7,
                    actionRaw = null,
                    messageRaw = "\"hello world\"",
                    countRaw = null,
                    present = listOf(false, true, true, false),
                    rawArguments = listOf("ready-now", "\"hello world\""),
                ),
                received.get(),
            )
        }
    }

    @Test
    fun `resulting executor stores its value in the command result`(env: Env) {
        withPlatform(env) { manager ->
            CommandAPICommand("sum")
                .withArguments(IntegerArgument("left"), IntegerArgument("right"))
                .anyResultingExecutor { _, arguments ->
                    arguments.get<Int>("left") + arguments.get<Int>("right")
                }
                .register()
            CommandAPICommand("normal-result")
                .anyExecutor { _, _ -> }
                .register()

            val result = runCommandForResult(manager.consoleSender, "sum 2 3")

            assertEquals(5, result)
            assertEquals(1, runCommandForResult(manager.consoleSender, "normal-result"))
        }
    }

    @Test
    fun `sync syntax failures render directly while unexpected failures render and log once`(env: Env) {
        withPlatform(env) { manager ->
            val direct = Component.text("Direct syntax failure", NamedTextColor.YELLOW)
            val wrapped = Component.text("Wrapped syntax failure", NamedTextColor.RED)
            CommandAPICommand("fail-direct")
                .anyExecutor { _, _ -> throw CommandSyntaxException(direct) }
                .register()
            CommandAPICommand("fail-wrapped")
                .anyExecutor { _, _ ->
                    throw WrapperCommandSyntaxException(CommandSyntaxException(wrapped))
                }
                .register()
            CommandAPICommand("fail-unexpected")
                .anyResultingExecutor { _, _ -> throw IllegalStateException("broken sync executor") }
                .register()

            val instance = env.createEmptyInstance()
            var player: Player? = null
            val logEvents = mutableListOf<LogEvent>()
            val appender = RecordingAppender(logEvents)
            val loggerContext = LogManager.getContext(false) as LoggerContext
            try {
                val connection = env.createConnection()
                player = connect(connection, instance)
                val messages = connection.trackIncoming(SystemChatPacket::class.java)
                appender.start()
                loggerContext.configuration.rootLogger.addAppender(appender, Level.ERROR, null)

                assertTrue(runCommand(player, "fail-direct"))
                assertTrue(runCommand(player, "fail-wrapped"))
                val unexpectedResult = runCommand(player, "fail-unexpected")

                assertTrue(unexpectedResult)
                val expectedMessages = listOf(direct, wrapped, GENERIC_COMMAND_FAILURE).map(::plain)
                assertEquals(expectedMessages, messages.collect().map { packet -> plain(packet.message) })
                assertEquals(
                    1,
                    logEvents.count { event ->
                        event.message.formattedMessage.contains("Failed to execute command 'fail-unexpected'")
                    },
                )
            } finally {
                loggerContext.configuration.rootLogger.removeAppender(appender.name)
                appender.stop()
                player?.remove()
                env.destroyInstance(instance)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `suspend executor returns immediately with an immutable full argument snapshot`(env: Env) = runTest {
        withPlatform(env) { manager ->
            var entered = false
            var valueAfterReturn: Triple<String, Int, Boolean>? = null
            CommandAPICommand("later")
                .withArguments(TextArgument("message"))
                .withOptionalArguments(IntegerArgument("count").setOptional(9))
                .anyExecutionInfoExecutorSuspend(scope = this) { info ->
                    entered = true
                    valueAfterReturn = Triple(
                        info.args.get("message"),
                        info.args.get("count"),
                        "count" in info.args,
                    )
                }
                .register()

            val result = runCommand(manager.consoleSender, "later \"kept value\"")

            assertTrue(result)
            assertFalse(entered)
            runCurrent()
            assertTrue(entered)
            assertEquals(Triple("kept value", 9, false), valueAfterReturn)
        }
    }

    @Test
    fun `missing compatible executor sends a sender type failure instead of cancelling syntax`(env: Env) {
        withPlatform(env) { manager ->
            val sender = RecordingSender()
            CommandAPICommand("players-only")
                .playerExecutor { _, _ -> }
                .register()

            val result = runCommand(sender, "players-only")

            assertTrue(result)
            assertEquals(
                listOf(
                    Component.text(
                        "Dieser Befehl kann von diesem Absendertyp nicht ausgeführt werden.",
                        NamedTextColor.RED,
                    ),
                ),
                sender.messages,
            )
        }
    }

    private inline fun withPlatform(env: Env, block: (net.minestom.server.command.CommandManager) -> Unit) {
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

    private fun connect(connection: TestConnection, instance: Instance): Player {
        val player = CompletableFuture<Player>()
        Thread.startVirtualThread {
            runCatching { connection.connect(instance, Pos.ZERO) }
                .onSuccess(player::complete)
                .onFailure(player::completeExceptionally)
        }
        return player.join()
    }

    private fun plain(component: Component): String =
        PlainTextComponentSerializer.plainText().serialize(component)

    private class DelegatedArguments(arguments: dev.slne.surf.api.minestom.command.executor.CommandArguments) {
        val mode: SnapshotMode by arguments
        val message: String by arguments
        val count: Int by arguments
    }

    private data class Snapshot(
        val input: String,
        val indexed: List<Any?>,
        val mode: SnapshotMode,
        val message: String,
        val count: Int,
        val actionRaw: String?,
        val messageRaw: String?,
        val countRaw: String?,
        val present: List<Boolean>,
        val rawArguments: List<String>,
    )

    private enum class SnapshotMode {
        READY,
    }

    private class RecordingSender : CommandSender {
        val messages = mutableListOf<Component>()

        override fun identity(): Identity = Identity.nil()

        override fun tagHandler(): TagHandler = TagHandler.newHandler()

        override fun sendMessage(message: Component) {
            messages += message
        }
    }

    private class RecordingAppender(
        private val events: MutableList<LogEvent>,
    ) : AbstractAppender(
        "commandapi-executor-${UUID.randomUUID()}",
        null,
        PatternLayout.createDefaultLayout(),
        false,
        emptyArray(),
    ) {
        override fun append(event: LogEvent) {
            events += event.toImmutable()
        }
    }
}
