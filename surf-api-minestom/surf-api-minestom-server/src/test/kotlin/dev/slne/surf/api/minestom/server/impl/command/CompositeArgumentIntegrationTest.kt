package dev.slne.surf.api.minestom.server.impl.command

import dev.slne.surf.api.minestom.command.CommandAPI
import dev.slne.surf.api.minestom.command.CommandAPICommand
import dev.slne.surf.api.minestom.command.CommandTree
import dev.slne.surf.api.minestom.command.argument.CustomArgument
import dev.slne.surf.api.minestom.command.argument.EnumArgument
import dev.slne.surf.api.minestom.command.argument.GreedyStringArgument
import dev.slne.surf.api.minestom.command.argument.IntegerArgument
import dev.slne.surf.api.minestom.command.argument.ListArgument
import dev.slne.surf.api.minestom.command.argument.LiteralArgument
import dev.slne.surf.api.minestom.command.argument.PositionArgument
import dev.slne.surf.api.minestom.command.argument.StringArgument
import dev.slne.surf.api.minestom.command.argument.UUIDArgument
import dev.slne.surf.api.minestom.command.argument.filter
import dev.slne.surf.api.minestom.command.argument.map
import dev.slne.surf.api.minestom.command.dsl.customArgument
import dev.slne.surf.api.minestom.command.dsl.listArgument
import com.mojang.brigadier.StringReader
import dev.slne.surf.api.minestom.command.exception.CommandSyntaxException
import dev.slne.surf.api.minestom.command.exception.CommandValidationException
import dev.slne.surf.api.minestom.command.exception.componentOrNull
import dev.slne.surf.api.minestom.server.impl.command.brigadier.NodeDeclarations
import com.mojang.brigadier.exceptions.CommandSyntaxException as BrigadierCommandSyntaxException
import dev.slne.surf.api.minestom.command.dsl.anyExecutionInfo
import dev.slne.surf.api.minestom.command.dsl.anyExecutor
import dev.slne.surf.api.minestom.command.suggestion.ArgumentSuggestions
import net.kyori.adventure.identity.Identity
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.ArgumentParserType
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.CommandResult
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.command.builder.exception.ArgumentSyntaxException
import net.minestom.server.tag.TagHandler
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

@EnvTest
class CompositeArgumentIntegrationTest {
    @Test
    fun `custom mapped filtered and list arguments execute with converted values and exact raw input`(env: Env) {
        withPlatform(env) { manager ->
            val firstId = UUID.fromString("79d40754-7772-4ab4-a7c6-f21df121db4a")
            val secondId = UUID.fromString("3b91a87f-82d2-4e85-ad47-3315f0021420")
            val users = mapOf("alice" to User("alice", 17))
            val customInfo = AtomicReference<CustomSnapshot>()
            val received = AtomicReference<ExecutionSnapshot>()
            val sender = RecordingSender()

            CommandAPICommand("compose")
                .withArguments(
                    CustomArgument(StringArgument("user")) { info ->
                        customInfo.set(CustomSnapshot(info.sender, info.currentInput, info.baseValue))
                        users[info.currentInput]
                            ?: CommandAPI.failWithString("Unknown user ${info.currentInput}")
                    },
                    IntegerArgument("amount").map { value -> value * 2 },
                    IntegerArgument("positive").filter { value -> value > 0 },
                    ListArgument("ids", UUIDArgument("id")),
                )
                .anyExecutionInfo { info ->
                    received.set(
                        ExecutionSnapshot(
                            user = info.args[0],
                            amount = info.args[1],
                            positive = info.args[2],
                            ids = info.args[3],
                            raw = info.args.rawArguments(),
                            idsRaw = info.args.getRaw("ids"),
                        ),
                    )
                }
                .register()

            val input = "compose alice 4 5 $firstId, $secondId"
            assertTrue(runCommand(sender, input))
            assertEquals(CustomSnapshot(sender, "alice", "alice"), customInfo.get())
            assertEquals(
                ExecutionSnapshot(
                    user = users.getValue("alice"),
                    amount = 8,
                    positive = 5,
                    ids = listOf(firstId, secondId),
                    raw = listOf("alice", "4", "5", "$firstId, $secondId"),
                    idsRaw = "$firstId, $secondId",
                ),
                received.get(),
            )
        }
    }

    @Test
    fun `base parsers feed custom and list element conversions and are declared as greedy strings`(
        env: Env,
    ) {
        val declarations = NodeDeclarations()
        val sender = env.process().command().consoleSender
        val greedy = CustomArgument(GreedyStringArgument("message")) { info ->
            info.baseValue to info.currentInput
        }.toDefinition()
        val modes = ListArgument(
            "modes",
            EnumArgument("mode", CompositeMode.entries) { mode -> "mode-${mode.name.lowercase()}" },
        ).toDefinition()

        assertEquals(
            "hello wide world" to "hello wide world",
            greedy.rawType.parse(StringReader("hello wide world"), sender),
        )
        assertEquals(
            listOf(CompositeMode.LOW, CompositeMode.HIGH),
            modes.rawType.parse(StringReader("mode-low, mode-high"), sender),
        )

        listOf(greedy, modes).forEach { definition ->
            val declaration = declarations.of(definition)
            assertEquals(ArgumentParserType.STRING, declaration.parser)
            assertArrayEquals(byteArrayOf(2), declaration.properties)
        }
    }

    @Test
    fun `custom arguments retain and convert a base default supplier`(env: Env) {
        withPlatform(env) { manager ->
            val defaultInfo = AtomicReference<CustomSnapshot>()
            val received = AtomicReference<Any?>()
            val base = IntegerArgument("amount").setOptional { sender ->
                assertSame(manager.consoleSender, sender)
                3
            }
            val doubled = CustomArgument(base) { info ->
                defaultInfo.set(CustomSnapshot(info.sender, info.currentInput, info.baseValue.toString()))
                info.baseValue * 2
            }
            val definition = doubled.toDefinition()
            CommandAPICommand("default-composite")
                .withArguments(doubled)
                .anyExecutor { _, arguments -> received.set(arguments[0]) }
                .register()

            assertTrue(runCommand(manager.consoleSender, "default-composite"))
            assertEquals(6, received.get())
            assertEquals(6, definition.defaultValue!!.invoke(manager.consoleSender))
            assertEquals(
                CustomSnapshot(manager.consoleSender, "", "3"),
                defaultInfo.get(),
            )
        }
    }

    @Test
    fun `filter and invalid or empty list elements reject without reordering valid entries`(env: Env) {
        val sender = env.process().command().consoleSender
        val firstId = UUID.fromString("79d40754-7772-4ab4-a7c6-f21df121db4a")
        val secondId = UUID.fromString("3b91a87f-82d2-4e85-ad47-3315f0021420")
        val positive = IntegerArgument("value").filter { it > 0 }.toDefinition().rawType
        val ids = ListArgument("ids", UUIDArgument("id")).toDefinition().rawType
        val emptyAllowed = ListArgument(
            "values",
            StringArgument("value"),
            delimiter = ';',
            allowEmpty = true,
        ).toDefinition().rawType

        assertThrows(BrigadierCommandSyntaxException::class.java) {
            positive.parse(StringReader("-1"), sender)
        }
        assertThrows(BrigadierCommandSyntaxException::class.java) {
            ids.parse(StringReader("$firstId,,${secondId}"), sender)
        }
        assertThrows(BrigadierCommandSyntaxException::class.java) {
            ids.parse(StringReader("$firstId,not-a-uuid"), sender)
        }
        assertEquals(
            listOf(firstId, secondId),
            ids.parse(StringReader("  $firstId , $secondId  "), sender),
        )
        assertEquals(
            listOf("one", "", "two", ""),
            emptyAllowed.parse(StringReader("one; ;two;"), sender),
        )

        assertThrows(IllegalArgumentException::class.java) {
            ListArgument("invalid", GreedyStringArgument("part"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ListArgument("invalid", StringArgument("part"), delimiter = ' ')
        }
    }

    @Test
    fun `custom syntax failures retain styled components and the base parser declaration`(env: Env) {
        val sender = env.process().command().consoleSender
        val styled = Component.text("Amount is reserved", NamedTextColor.GOLD)
        val definition = CustomArgument(IntegerArgument("amount", 1, 8)) { info ->
            if (info.baseValue == 3) {
                throw CommandSyntaxException(styled, input = "reserved-three")
            }
            info.baseValue
        }.toDefinition()

        val declaration = NodeDeclarations().of(definition)
        assertEquals(ArgumentParserType.INTEGER, declaration.parser)
        assertArrayEquals(
            ArgumentType.Integer("amount").min(1).max(8).nodeProperties(),
            declaration.properties,
        )

        // The value is rejected where it started, so the client underlines the value itself.
        val failure = assertThrows(BrigadierCommandSyntaxException::class.java) {
            definition.rawType.parse(StringReader("3"), sender)
        }
        assertEquals(styled, failure.rawMessage.componentOrNull())
        assertEquals(0, failure.cursor)

        val filterFailure = assertThrows(BrigadierCommandSyntaxException::class.java) {
            IntegerArgument("positive").filter { it > 0 }.toDefinition()
                .rawType.parse(StringReader("-1"), sender)
        }
        assertEquals(
            Component.text("Invalid value for positive"),
            filterFailure.rawMessage.componentOrNull(),
        )
    }

    @Test
    fun `custom argument wrapping a literal still fails registration despite the bypassed guard`(env: Env) {
        withPlatform(env) { manager ->
            val argument = CustomArgument(LiteralArgument("fixed")) { info -> info.baseValue }
                .replaceSuggestions(ArgumentSuggestions.strings("other"))
            val failure = assertThrows(CommandValidationException::class.java) {
                CommandAPICommand("custom-reject-literal")
                    .withArguments(argument)
                    .anyExecutor { _, _ -> }
                    .register()
            }
            assertEquals(
                "Literal argument '${argument.nodeName}' cannot use custom suggestions",
                failure.message,
            )
            assertFalse(runCommand(manager.consoleSender, "custom-reject-literal fixed"))
        }
    }

    @Test
    fun `custom argument wrapping a position kind still fails registration despite the bypassed guard`(env: Env) {
        withPlatform(env) { manager ->
            val argument = CustomArgument(PositionArgument("pos")) { info -> info.baseValue }
                .replaceSuggestions(ArgumentSuggestions.strings("~ ~ ~"))
            val failure = assertThrows(CommandValidationException::class.java) {
                CommandAPICommand("custom-reject-position")
                    .withArguments(argument)
                    .anyExecutor { _, _ -> }
                    .register()
            }
            assertEquals(
                "Position argument '${argument.nodeName}' cannot use custom suggestions",
                failure.message,
            )
            assertFalse(runCommand(manager.consoleSender, "custom-reject-position 1 2 3"))
        }
    }

    @Test
    fun `composite DSL builders attach root tree and child arguments`() {
        val command = CommandAPICommand("dsl-root")
            .customArgument(StringArgument("user"), parser = { info -> info.baseValue.length })
            .listArgument("ids", UUIDArgument("id"), delimiter = ';')
            .anyExecutor { _, _ -> }
            .toDefinition()
        val tree = CommandTree("dsl-tree")
            .customArgument(StringArgument("user"), parser = { info -> info.baseValue.length }) {
                anyExecutor { _, _ -> }
            }
            .toDefinition()
        val child = CommandTree("dsl-child")
            .then(
                StringArgument("prefix").listArgument("ids", UUIDArgument("id")) {
                    anyExecutor { _, _ -> }
                },
            )
            .toDefinition()

        assertEquals(listOf("user", "ids"), command.paths.single().arguments.map { it.nodeName })
        assertEquals(listOf("user"), tree.paths.single().arguments.map { it.nodeName })
        assertEquals(listOf("prefix", "ids"), child.paths.single().arguments.map { it.nodeName })
    }

    @Test
    fun `custom DSL receivers preserve inherited constant and sender defaults`(env: Env) {
        withPlatform(env) { manager ->
            val commandValue = AtomicReference<Any?>()
            val treeValue = AtomicReference<Any?>()
            val childValue = AtomicReference<Any?>()

            CommandAPICommand("dsl-command-preserve")
                .customArgument(
                    IntegerArgument("amount").setOptional(3),
                    parser = { info -> info.baseValue * 2 },
                )
                .anyExecutor { _, arguments -> commandValue.set(arguments[0]) }
                .register()
            CommandTree("dsl-tree-preserve")
                .customArgument(
                    IntegerArgument("amount").setOptional { sender ->
                        assertSame(manager.consoleSender, sender)
                        4
                    },
                    parser = { info -> info.baseValue * 2 },
                ) {
                    anyExecutor { _, arguments -> treeValue.set(arguments[0]) }
                }
                .register()
            CommandTree("dsl-child-preserve")
                .then(
                    StringArgument("prefix").customArgument(
                        IntegerArgument("amount").setOptional(5),
                        parser = { info -> info.baseValue * 2 },
                    ) {
                        anyExecutor { _, arguments -> childValue.set(arguments[1]) }
                    },
                )
                .register()

            assertTrue(runCommand(manager.consoleSender, "dsl-command-preserve"))
            assertTrue(runCommand(manager.consoleSender, "dsl-tree-preserve"))
            assertTrue(runCommand(manager.consoleSender, "dsl-child-preserve prefix"))
            assertEquals(6, commandValue.get())
            assertEquals(8, treeValue.get())
            assertEquals(10, childValue.get())
        }
    }

    @Test
    fun `custom DSL receivers apply explicit false and true optional overrides`(env: Env) {
        withPlatform(env) { manager ->
            val optionalValues = mutableMapOf<String, Any?>()
            val commandFalse = CommandAPICommand("dsl-command-false")
                .customArgument(
                    IntegerArgument("amount").setOptional(3),
                    parser = { info -> info.baseValue * 2 },
                    optional = false,
                )
                .anyExecutor { _, arguments -> optionalValues["command-false"] = arguments[0] }
            val commandTrue = CommandAPICommand("dsl-command-true")
                .customArgument(
                    IntegerArgument("amount").setOptional(3),
                    parser = { info -> info.baseValue * 2 },
                    optional = true,
                )
                .anyExecutor { _, arguments -> optionalValues["command-true"] = arguments[0] }
            val treeFalse = CommandTree("dsl-tree-false")
                .customArgument(
                    IntegerArgument("amount").setOptional(4),
                    parser = { info -> info.baseValue * 2 },
                    optional = false,
                ) {
                    anyExecutor { _, arguments -> optionalValues["tree-false"] = arguments[0] }
                }
            val treeTrue = CommandTree("dsl-tree-true")
                .customArgument(
                    IntegerArgument("amount").setOptional(4),
                    parser = { info -> info.baseValue * 2 },
                    optional = true,
                ) {
                    anyExecutor { _, arguments -> optionalValues["tree-true"] = arguments[0] }
                }
            val childFalse = CommandTree("dsl-child-false").then(
                StringArgument("prefix").customArgument(
                    IntegerArgument("amount").setOptional(5),
                    parser = { info -> info.baseValue * 2 },
                    optional = false,
                ) {
                    anyExecutor { _, arguments -> optionalValues["child-false"] = arguments[1] }
                },
            )
            val childTrue = CommandTree("dsl-child-true").then(
                StringArgument("prefix").customArgument(
                    IntegerArgument("amount").setOptional(5),
                    parser = { info -> info.baseValue * 2 },
                    optional = true,
                ) {
                    anyExecutor { _, arguments -> optionalValues["child-true"] = arguments[1] }
                },
            )

            listOf(
                commandFalse.toDefinition(),
                treeFalse.toDefinition(),
                childFalse.toDefinition(),
            ).forEach { definition ->
                val argument = definition.paths.single().arguments.last()
                assertFalse(argument.optional)
                assertNull(argument.defaultValue)
            }
            listOf(
                commandTrue.toDefinition(),
                treeTrue.toDefinition(),
                childTrue.toDefinition(),
            ).forEach { definition ->
                val argument = definition.paths.single().arguments.last()
                assertTrue(argument.optional)
                assertNull(argument.defaultValue)
            }
            commandFalse.register()
            commandTrue.register()
            treeFalse.register()
            treeTrue.register()
            childFalse.register()
            childTrue.register()

            listOf(
                "dsl-command-false",
                "dsl-tree-false",
                "dsl-child-false prefix",
            ).forEach { input ->
                assertFalse(runCommand(manager.consoleSender, input))
            }
            listOf(
                "dsl-command-true",
                "dsl-tree-true",
                "dsl-child-true prefix",
            ).forEach { input ->
                assertTrue(runCommand(manager.consoleSender, input))
            }
            assertEquals(
                mapOf(
                    "command-true" to null,
                    "tree-true" to null,
                    "child-true" to null,
                ),
                optionalValues,
            )
        }
    }

    private inline fun withPlatform(
        env: Env,
        block: (net.minestom.server.command.CommandManager) -> Unit,
    ) {
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

    private data class User(val name: String, val level: Int)

    private data class CustomSnapshot(
        val sender: CommandSender,
        val currentInput: String,
        val baseValue: String,
    )

    private data class ExecutionSnapshot(
        val user: Any?,
        val amount: Any?,
        val positive: Any?,
        val ids: Any?,
        val raw: List<String>,
        val idsRaw: String?,
    )

    private enum class CompositeMode {
        LOW,
        HIGH,
    }

    private class RecordingSender : CommandSender {
        val messages = mutableListOf<Component>()

        override fun identity(): Identity = Identity.nil()

        override fun tagHandler(): TagHandler = TagHandler.newHandler()

        override fun sendMessage(message: Component) {
            messages += message
        }
    }
}
