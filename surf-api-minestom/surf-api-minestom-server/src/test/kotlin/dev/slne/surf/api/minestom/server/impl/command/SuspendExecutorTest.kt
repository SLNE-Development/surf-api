package dev.slne.surf.api.minestom.server.impl.command

import dev.slne.surf.api.minestom.command.exception.CommandSyntaxException
import dev.slne.surf.api.minestom.command.exception.WrapperCommandSyntaxException
import dev.slne.surf.api.minestom.command.executor.CommandArguments
import dev.slne.surf.api.minestom.command.executor.CommandExecutable
import dev.slne.surf.api.minestom.command.executor.ExecutionInfo
import dev.slne.surf.api.minestom.command.executor.ExecutorDefinition
import dev.slne.surf.api.minestom.command.executor.ExecutorType
import dev.slne.surf.api.minestom.command.executor.ParsedArgument
import dev.slne.surf.api.minestom.command.dsl.anyExecutionInfo
import dev.slne.surf.api.minestom.command.dsl.anyExecutionInfoExecutorSuspend
import dev.slne.surf.api.minestom.command.dsl.anyExecutor
import dev.slne.surf.api.minestom.command.dsl.anyExecutorSuspend
import dev.slne.surf.api.minestom.command.dsl.anyResultingExecutionInfo
import dev.slne.surf.api.minestom.command.dsl.anyResultingExecutor
import dev.slne.surf.api.minestom.command.dsl.consoleExecutionInfo
import dev.slne.surf.api.minestom.command.dsl.consoleExecutionInfoExecutorSuspend
import dev.slne.surf.api.minestom.command.dsl.consoleExecutor
import dev.slne.surf.api.minestom.command.dsl.consoleExecutorSuspend
import dev.slne.surf.api.minestom.command.dsl.consoleResultingExecutionInfo
import dev.slne.surf.api.minestom.command.dsl.consoleResultingExecutor
import dev.slne.surf.api.minestom.command.executor.launchCommandExecutor
import dev.slne.surf.api.minestom.command.dsl.playerExecutionInfo
import dev.slne.surf.api.minestom.command.dsl.playerExecutionInfoExecutorSuspend
import dev.slne.surf.api.minestom.command.dsl.playerExecutor
import dev.slne.surf.api.minestom.command.dsl.playerExecutorSuspend
import dev.slne.surf.api.minestom.command.dsl.playerResultingExecutionInfo
import dev.slne.surf.api.minestom.command.dsl.playerResultingExecutor
import dev.slne.surf.api.minestom.coroutine.minestomAsyncScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.minestom.server.MinecraftServer
import net.minestom.server.command.CommandSender
import net.minestom.server.command.ConsoleSender
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.network.packet.server.play.SystemChatPacket
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import net.minestom.testing.TestConnection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

@OptIn(ExperimentalCoroutinesApi::class)
@EnvTest
class SuspendExecutorTest {
    @Test
    fun `normal fluent variants register typed callbacks and return their receiver`(env: Env) {
        withPlayer(env) { player ->
            val console = MinecraftServer.getCommandManager().consoleSender
            val arguments = arguments()
            val executable = RecordingExecutable()
            val calls = mutableListOf<String>()

            assertSame(executable, executable.anyExecutor { sender, args ->
                assertSame(player, sender)
                assertSame(arguments, args)
                calls += "any"
            })
            assertSame(executable, executable.executes { sender, args ->
                assertSame(player, sender)
                assertSame(arguments, args)
                calls += "executes"
            })
            assertSame(executable, executable.playerExecutor { sender, args ->
                assertSame(player, sender)
                assertSame(arguments, args)
                calls += "player"
            })
            assertSame(executable, executable.executesPlayer { sender, args ->
                assertSame(player, sender)
                assertSame(arguments, args)
                calls += "executesPlayer"
            })
            assertSame(executable, executable.consoleExecutor { sender, args ->
                assertSame(console, sender)
                assertSame(arguments, args)
                calls += "console"
            })
            assertSame(executable, executable.executesConsole { sender, args ->
                assertSame(console, sender)
                assertSame(arguments, args)
                calls += "executesConsole"
            })

            assertEquals(
                listOf(
                    ExecutorType.ANY,
                    ExecutorType.ANY,
                    ExecutorType.PLAYER,
                    ExecutorType.PLAYER,
                    ExecutorType.CONSOLE,
                    ExecutorType.CONSOLE,
                ),
                executable.definitions.map(ExecutorDefinition::type),
            )
            executable.definitions.forEachIndexed { index, definition ->
                val sender = if (definition.type == ExecutorType.CONSOLE) console else player
                normal(definition).executor(ExecutionInfo(sender, arguments, "normal-$index"))
            }
            assertEquals(
                listOf("any", "executes", "player", "executesPlayer", "console", "executesConsole"),
                calls,
            )
        }
    }

    @Test
    fun `execution info variants preserve sender arguments and raw input`(env: Env) {
        withPlayer(env) { player ->
            val console = MinecraftServer.getCommandManager().consoleSender
            val arguments = arguments()
            val executable = RecordingExecutable()
            val calls = mutableListOf<String>()

            assertSame(executable, executable.anyExecutionInfo { info ->
                assertInfo(info, player, arguments, "any-info")
                calls += "any"
            })
            assertSame(executable, executable.playerExecutionInfo { info ->
                assertInfo(info, player, arguments, "player-info")
                calls += "player"
            })
            assertSame(executable, executable.consoleExecutionInfo { info ->
                assertInfo(info, console, arguments, "console-info")
                calls += "console"
            })

            normal(executable.definitions[0]).executor(ExecutionInfo(player, arguments, "any-info"))
            normal(executable.definitions[1]).executor(ExecutionInfo(player, arguments, "player-info"))
            normal(executable.definitions[2]).executor(ExecutionInfo(console, arguments, "console-info"))

            assertEquals(listOf(ExecutorType.ANY, ExecutorType.PLAYER, ExecutorType.CONSOLE), executable.definitions.map { it.type })
            assertEquals(listOf("any", "player", "console"), calls)
        }
    }

    @Test
    fun `resulting variants retain integer results for every sender type`(env: Env) {
        withPlayer(env) { player ->
            val console = MinecraftServer.getCommandManager().consoleSender
            val arguments = arguments()
            val executable = RecordingExecutable()

            assertSame(executable, executable.anyResultingExecutor { sender, args ->
                assertSame(player, sender)
                assertSame(arguments, args)
                11
            })
            assertSame(executable, executable.playerResultingExecutor { sender, args ->
                assertSame(player, sender)
                assertSame(arguments, args)
                22
            })
            assertSame(executable, executable.consoleResultingExecutor { sender, args ->
                assertSame(console, sender)
                assertSame(arguments, args)
                33
            })
            assertSame(executable, executable.anyResultingExecutionInfo { info ->
                assertInfo(info, player, arguments, "any-result-info")
                44
            })
            assertSame(executable, executable.playerResultingExecutionInfo { info ->
                assertInfo(info, player, arguments, "player-result-info")
                55
            })
            assertSame(executable, executable.consoleResultingExecutionInfo { info ->
                assertInfo(info, console, arguments, "console-result-info")
                66
            })

            assertEquals(11, resulting(executable.definitions[0]).executor(ExecutionInfo(player, arguments, "unused")))
            assertEquals(22, resulting(executable.definitions[1]).executor(ExecutionInfo(player, arguments, "unused")))
            assertEquals(33, resulting(executable.definitions[2]).executor(ExecutionInfo(console, arguments, "unused")))
            assertEquals(44, resulting(executable.definitions[3]).executor(ExecutionInfo(player, arguments, "any-result-info")))
            assertEquals(55, resulting(executable.definitions[4]).executor(ExecutionInfo(player, arguments, "player-result-info")))
            assertEquals(66, resulting(executable.definitions[5]).executor(ExecutionInfo(console, arguments, "console-result-info")))
            assertEquals(
                listOf(
                    ExecutorType.ANY,
                    ExecutorType.PLAYER,
                    ExecutorType.CONSOLE,
                    ExecutorType.ANY,
                    ExecutorType.PLAYER,
                    ExecutorType.CONSOLE,
                ),
                executable.definitions.map { it.type },
            )
        }
    }

    @Test
    fun `suspend fluent variants adapt typed senders arguments info and scope choices`(env: Env) = runTest {
        withPlayerSuspend(env) { player ->
            val console = MinecraftServer.getCommandManager().consoleSender
            val arguments = arguments()
            val executable = RecordingExecutable()
            val calls = mutableListOf<String>()

            assertSame(executable, executable.anyExecutorSuspend(scope = { this }) { sender, args ->
                assertSame(player, sender)
                assertSame(arguments, args)
                calls += "any"
            })
            assertSame(executable, executable.playerExecutorSuspend(scope = this) { sender, args ->
                assertSame(player, sender)
                assertSame(arguments, args)
                calls += "player"
            })
            assertSame(executable, executable.consoleExecutorSuspend(scope = { this }) { sender, args ->
                assertSame(console, sender)
                assertSame(arguments, args)
                calls += "console"
            })
            assertSame(executable, executable.anyExecutionInfoExecutorSuspend(scope = { this }) { info ->
                assertInfo(info, player, arguments, "any-info-suspend")
                calls += "any-info"
            })
            assertSame(executable, executable.playerExecutionInfoExecutorSuspend(scope = { this }) { info ->
                assertInfo(info, player, arguments, "player-info-suspend")
                calls += "player-info"
            })
            assertSame(executable, executable.consoleExecutionInfoExecutorSuspend(scope = { this }) { info ->
                assertInfo(info, console, arguments, "console-info-suspend")
                calls += "console-info"
            })
            executable.anyExecutorSuspend { _, _ -> }

            assertSame(this, suspending(executable.definitions[0]).scopeProvider())
            assertSame(this, suspending(executable.definitions[1]).scopeProvider())
            assertSame(minestomAsyncScope, suspending(executable.definitions.last()).scopeProvider())
            assertEquals(
                listOf(
                    ExecutorType.ANY,
                    ExecutorType.PLAYER,
                    ExecutorType.CONSOLE,
                    ExecutorType.ANY,
                    ExecutorType.PLAYER,
                    ExecutorType.CONSOLE,
                    ExecutorType.ANY,
                ),
                executable.definitions.map { it.type },
            )

            suspending(executable.definitions[0]).executor(this, ExecutionInfo(player, arguments, "unused"))
            suspending(executable.definitions[1]).executor(this, ExecutionInfo(player, arguments, "unused"))
            suspending(executable.definitions[2]).executor(this, ExecutionInfo(console, arguments, "unused"))
            suspending(executable.definitions[3]).executor(this, ExecutionInfo(player, arguments, "any-info-suspend"))
            suspending(executable.definitions[4]).executor(this, ExecutionInfo(player, arguments, "player-info-suspend"))
            suspending(executable.definitions[5]).executor(this, ExecutionInfo(console, arguments, "console-info-suspend"))
            assertEquals(listOf("any", "player", "console", "any-info", "player-info", "console-info"), calls)
        }
    }

    @Test
    fun `launcher schedules work and supplies immutable execution inputs`(env: Env) = runTest {
        withPlayerSuspend(env) { player ->
            val arguments = arguments()
            var received: ExecutionInfo<CommandSender>? = null
            val definition = ExecutorDefinition.Suspending(
                type = ExecutorType.ANY,
                scopeProvider = { this },
                executor = { received = it },
            )

            val job = launchCommandExecutor(definition, player, arguments, "async-test value")

            assertFalse(job.isCompleted)
            assertEquals(null, received)
            runCurrent()
            assertTrue(job.isCompleted)
            assertInfo(checkNotNull(received), player, arguments, "async-test value")
        }
    }

    @Test
    fun `cancellation is propagated without sender failure`(env: Env) = runTest {
        withPlayerConnectionSuspend(env) { player, messages ->
            val definition = ExecutorDefinition.Suspending(
                type = ExecutorType.ANY,
                scopeProvider = { this },
                executor = { throw CancellationException("stop") },
            )

            val job = launchCommandExecutor(definition, player, CommandArguments.empty(), "cancel-test")
            runCurrent()

            assertTrue(job.isCancelled)
            messages.assertEmpty()
        }
    }

    @Test
    fun `direct syntax failure sends its component`(env: Env) = runTest {
        val component = Component.text("Expected a destination", NamedTextColor.RED)
        assertFailureMessage(env, component) {
            throw CommandSyntaxException(component, "/warp", 5)
        }
    }

    @Test
    fun `wrapped syntax failure sends its nested component`(env: Env) = runTest {
        val component = Component.text("Expected a player", NamedTextColor.RED)
        assertFailureMessage(env, component) {
            throw WrapperCommandSyntaxException(CommandSyntaxException(component, "/tell", 6))
        }
    }

    @Test
    fun `unchecked failure sends the standard generic component`(env: Env) = runTest {
        assertFailureMessage(
            env,
            Component.text(
                "Beim Ausführen des Befehls ist ein Fehler aufgetreten.",
                NamedTextColor.RED,
            ),
        ) {
            throw IllegalStateException("broken executor")
        }
    }

    private suspend fun TestScope.assertFailureMessage(
        env: Env,
        expected: Component,
        failure: suspend CoroutineScope.() -> Unit,
    ) {
        withPlayerConnectionSuspend(env) { player, messages ->
            val definition = ExecutorDefinition.Suspending(
                type = ExecutorType.ANY,
                scopeProvider = { this },
                executor = { failure() },
            )

            val job = launchCommandExecutor(definition, player, CommandArguments.empty(), "failure-test")
            runCurrent()

            assertTrue(job.isCompleted)
            messages.assertSingle { packet ->
                assertMessage(expected, packet)
            }
        }
    }

    private fun assertMessage(expected: Component, packet: SystemChatPacket) {
        val plain = PlainTextComponentSerializer.plainText()
        assertEquals(plain.serialize(expected), plain.serialize(packet.message))
        assertEquals(expected.color(), packet.message.color())
    }

    private inline fun withPlayer(env: Env, block: (Player) -> Unit) {
        val instance = env.createEmptyInstance()
        var player: Player? = null
        try {
            val connectedPlayer = connect(env.createConnection(), instance)
            player = connectedPlayer
            block(connectedPlayer)
        } finally {
            player?.remove()
            env.destroyInstance(instance)
        }
    }

    private suspend inline fun withPlayerSuspend(
        env: Env,
        crossinline block: suspend (Player) -> Unit,
    ) {
        val instance = env.createEmptyInstance()
        var player: Player? = null
        try {
            val connectedPlayer = connect(env.createConnection(), instance)
            player = connectedPlayer
            block(connectedPlayer)
        } finally {
            player?.remove()
            env.destroyInstance(instance)
        }
    }

    private suspend inline fun withPlayerConnectionSuspend(
        env: Env,
        crossinline block: suspend (Player, net.minestom.testing.Collector<SystemChatPacket>) -> Unit,
    ) {
        val instance = env.createEmptyInstance()
        var player: Player? = null
        try {
            val connection = env.createConnection()
            val connectedPlayer = connect(connection, instance)
            player = connectedPlayer
            val messages = connection.trackIncoming(SystemChatPacket::class.java)
            block(connectedPlayer, messages)
        } finally {
            player?.remove()
            env.destroyInstance(instance)
        }
    }

    private fun arguments() = CommandArguments.of(
        listOf(ParsedArgument("marker", "value", "value", present = true)),
    )

    private fun connect(connection: TestConnection, instance: Instance): Player {
        val player = CompletableFuture<Player>()
        Thread.startVirtualThread {
            runCatching { connection.connect(instance, Pos.ZERO) }
                .onSuccess(player::complete)
                .onFailure(player::completeExceptionally)
        }
        return player.join()
    }

    private fun normal(definition: ExecutorDefinition) =
        assertInstanceOf(ExecutorDefinition.Normal::class.java, definition)

    private fun resulting(definition: ExecutorDefinition) =
        assertInstanceOf(ExecutorDefinition.Resulting::class.java, definition)

    private fun suspending(definition: ExecutorDefinition) =
        assertInstanceOf(ExecutorDefinition.Suspending::class.java, definition)

    private fun <S : CommandSender> assertInfo(
        info: ExecutionInfo<S>,
        sender: S,
        arguments: CommandArguments,
        input: String,
    ) {
        assertSame(sender, info.sender)
        assertSame(arguments, info.args)
        assertEquals(input, info.input)
    }

    private class RecordingExecutable : CommandExecutable<RecordingExecutable> {
        val definitions = mutableListOf<ExecutorDefinition>()

        override fun addExecutor(definition: ExecutorDefinition): RecordingExecutable {
            definitions += definition
            return this
        }
    }
}
