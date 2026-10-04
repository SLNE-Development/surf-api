package dev.slne.surf.api.minestom.server.impl.command.brigadier

import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.slne.surf.api.minestom.command.CommandAPICommand
import dev.slne.surf.api.minestom.command.CommandTree
import dev.slne.surf.api.minestom.command.argument.EnumArgument
import dev.slne.surf.api.minestom.command.argument.GreedyStringArgument
import dev.slne.surf.api.minestom.command.argument.IntegerArgument
import dev.slne.surf.api.minestom.command.argument.LiteralArgument
import dev.slne.surf.api.minestom.command.argument.StringArgument
import dev.slne.surf.api.minestom.command.dsl.anyExecutor
import dev.slne.surf.api.minestom.command.dsl.anyResultingExecutor
import dev.slne.surf.api.minestom.command.executor.CommandArguments
import net.minestom.server.command.CommandSender
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicReference

@EnvTest
class BrigadierCommandTreeTest {
    @Test
    fun `a literal and an argument may share a name without any renaming`(env: Env) {
        val received = AtomicReference<CommandArguments>()
        val tree = treeOf(
            CommandTree("send").then(
                LiteralArgument("server").then(
                    StringArgument("server").anyExecutor { _, args -> received.set(args) },
                ),
            ),
        )

        assertEquals(1, tree.dispatch(env, "send server lobby-1"))
        assertEquals("lobby-1", received.get().get<String>("server"))
        assertEquals("lobby-1", received.get().getRaw("server"))
    }

    @Test
    fun `input that matches no branch never falls through to the preceding node`(env: Env) {
        val root = AtomicReference(0)
        val branch = AtomicReference(0)
        val tree = treeOf(
            CommandTree("difficulty")
                .anyExecutor { _, _ -> root.set(root.get() + 1) }
                .then(
                    LiteralArgument("hard")
                        .anyExecutor { _, _ -> branch.set(branch.get() + 1) },
                ),
        )

        assertThrows(CommandSyntaxException::class.java) { tree.dispatch(env, "difficulty aaa") }
        assertEquals(0, root.get())
        assertEquals(0, branch.get())

        assertEquals(1, tree.dispatch(env, "difficulty"))
        assertEquals(1, root.get())
    }

    @Test
    fun `trailing input after a complete syntax is rejected`(env: Env) {
        val executions = AtomicReference(0)
        val tree = treeOf(
            CommandTree("difficulty").then(
                LiteralArgument("hard").anyExecutor { _, _ ->
                    executions.set(executions.get() + 1)
                },
            ),
        )

        val failure = assertThrows(CommandSyntaxException::class.java) {
            tree.dispatch(env, "difficulty hard aaa")
        }

        assertTrue(failure.message!!.isNotBlank())
        assertEquals(0, executions.get())
        assertEquals(1, tree.dispatch(env, "difficulty hard"))
    }

    @Test
    fun `an optional suffix stays executable at every length`(env: Env) {
        val received = AtomicReference<CommandArguments>()
        val tree = treeOf(
            CommandAPICommand("kick")
                .withArguments(StringArgument("target"))
                .withOptionalArguments(GreedyStringArgument("reason"))
                .anyExecutor { _, args -> received.set(args) },
        )

        assertEquals(1, tree.dispatch(env, "kick Deniz"))
        assertNull(received.get().getOptional<String>("reason"))

        assertEquals(1, tree.dispatch(env, "kick Deniz being rude"))
        assertEquals("being rude", received.get().get<String>("reason"))
    }

    @Test
    fun `a resulting executor returns its own value to the dispatcher`(env: Env) {
        val tree = treeOf(
            CommandAPICommand("sum")
                .withArguments(IntegerArgument("left"), IntegerArgument("right"))
                .anyResultingExecutor { _, args ->
                    args.get<Int>("left") + args.get<Int>("right")
                },
        )

        assertEquals(5, tree.dispatch(env, "sum 2 3"))
    }

    @Test
    fun `a rejected argument value reports a syntax failure instead of executing`(env: Env) {
        val executions = AtomicReference(0)
        val tree = treeOf(
            CommandAPICommand("count")
                .withArguments(IntegerArgument("value", 1, 8))
                .anyExecutor { _, _ -> executions.set(executions.get() + 1) },
        )

        assertThrows(CommandSyntaxException::class.java) { tree.dispatch(env, "count 99") }
        assertEquals(0, executions.get())
    }

    @Test
    fun `a value produced by an argument's own parser reaches the executor`(env: Env) {
        val received = AtomicReference<CommandArguments>()
        val tree = treeOf(
            CommandAPICommand("warp")
                .withArguments(EnumArgument("mode", Mode.entries))
                .withOptionalArguments(IntegerArgument("delay", 0, 60))
                .anyExecutor { _, args -> received.set(args) },
        )

        assertEquals(1, tree.dispatch(env, "warp fast"))
        assertEquals(Mode.FAST, received.get().get<Mode>("mode"))
        assertNull(received.get().getOptional<Int>("delay"))

        assertEquals(1, tree.dispatch(env, "warp slow 30"))
        assertEquals(Mode.SLOW, received.get().get<Mode>("mode"))
        assertEquals(30, received.get().get<Int>("delay"))

        assertThrows(CommandSyntaxException::class.java) { tree.dispatch(env, "warp sideways") }
        assertThrows(CommandSyntaxException::class.java) { tree.dispatch(env, "warp fast 99") }
    }

    private enum class Mode { FAST, SLOW }

    private fun treeOf(command: CommandAPICommand): BrigadierCommandTree =
        BrigadierCommandTree().apply {
            register(command.name, listOf(command.name), command.toDefinition())
        }

    private fun treeOf(command: CommandTree): BrigadierCommandTree =
        BrigadierCommandTree().apply {
            register(command.name, listOf(command.name), command.toDefinition())
        }

    private fun BrigadierCommandTree.dispatch(env: Env, input: String): Int {
        val sender: CommandSender = env.process().command().consoleSender
        return dispatcher.execute(input, sender)
    }
}
