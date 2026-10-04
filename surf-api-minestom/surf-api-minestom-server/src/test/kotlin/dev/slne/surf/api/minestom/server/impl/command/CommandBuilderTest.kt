package dev.slne.surf.api.minestom.server.impl.command

import dev.slne.surf.api.minestom.command.CommandAPI
import dev.slne.surf.api.minestom.command.CommandAPICommand
import dev.slne.surf.api.minestom.command.CommandDefinition
import dev.slne.surf.api.minestom.command.CommandTree
import dev.slne.surf.api.minestom.command.RegisteredCommand
import dev.slne.surf.api.minestom.command.argument.GreedyStringArgument
import dev.slne.surf.api.minestom.command.argument.IntegerArgument
import dev.slne.surf.api.minestom.command.argument.LiteralArgument
import dev.slne.surf.api.minestom.command.argument.StringArgument
import dev.slne.surf.api.minestom.command.dsl.anyExecutor
import dev.slne.surf.api.minestom.command.dsl.commandAPICommand
import dev.slne.surf.api.minestom.command.dsl.integerArgument
import dev.slne.surf.api.minestom.command.executor.ExecutorType
import dev.slne.surf.api.minestom.command.dsl.playerExecutor
import dev.slne.surf.api.minestom.command.exception.CommandSyntaxException
import dev.slne.surf.api.minestom.command.exception.CommandValidationException
import dev.slne.surf.api.minestom.command.internal.CommandAPIPlatform
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.minestom.server.command.CommandSender
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy

class CommandBuilderTest {
    @Test
    fun `subcommands and optional suffixes freeze into paths`() {
        val rootRequirement: (CommandSender) -> Boolean = { true }
        val subcommandRequirement: (CommandSender) -> Boolean = { false }
        val command = CommandAPICommand("staff")
            .withPermission("lobby.staff")
            .withRequirement(rootRequirement)
            .withSubcommand(
                CommandAPICommand("message")
                    .withPermission("lobby.staff.message")
                    .withRequirement(subcommandRequirement)
                    .withArguments(StringArgument("target"))
                    .withOptionalArguments(GreedyStringArgument("reason").setOptional("none"))
                    .anyExecutor { _, _ -> },
            )

        val definition = command.toDefinition()
        val path = definition.paths.single()

        assertEquals(listOf("message", "target", "reason"), path.arguments.map { it.nodeName })
        assertEquals(listOf(false, false, true), path.arguments.map { it.optional })
        assertTrue(path.arguments.last().greedy)
        assertEquals("none", path.arguments.last().defaultValue?.invoke(nullSender()))
        assertEquals(setOf("lobby.staff", "lobby.staff.message"), path.permissions)
        assertSame(rootRequirement, path.requirements[0])
        assertSame(subcommandRequirement, path.requirements[1])
    }

    @Test
    fun `root path retains aliases metadata and conditions`() {
        val requirement: (CommandSender) -> Boolean = { true }
        val definition = CommandAPICommand("announce")
            .withAliases("broadcast", "notice")
            .withShortDescription("Short")
            .withFullDescription("First line", "Second line")
            .withUsage("/announce <message>", "/broadcast <message>")
            .withHelp("Long-form help")
            .withPermission("lobby.announce")
            .withRequirement(requirement)
            .anyExecutor { _, _ -> }
            .toDefinition()

        assertEquals(setOf("broadcast", "notice"), definition.aliases)
        assertEquals("Short", definition.metadata.shortDescription)
        assertEquals(listOf("First line", "Second line"), definition.metadata.fullDescription)
        assertEquals(listOf("/announce <message>", "/broadcast <message>"), definition.metadata.usage)
        assertEquals("Long-form help", definition.metadata.help)
        assertEquals(setOf("lobby.announce"), definition.paths.single().permissions)
        assertSame(requirement, definition.paths.single().requirements.single())
    }

    @Test
    fun `snapshot does not observe later builder mutations`() {
        val argument = StringArgument("target").withPermission("before.argument")
        val command = CommandAPICommand("message")
            .withAliases("msg")
            .withPermission("before.command")
            .withArguments(argument)
            .anyExecutor { _, _ -> }

        val snapshot = command.toDefinition()
        argument.withPermission("after.argument").then(StringArgument("child").anyExecutor { _, _ -> })
        command.withAliases("tell").withPermission("after.command").withArguments(StringArgument("extra"))

        assertEquals(setOf("msg"), snapshot.aliases)
        assertEquals(listOf("target"), snapshot.paths.single().arguments.map { it.nodeName })
        assertEquals(setOf("before.argument"), snapshot.paths.single().arguments.single().permissions)
        assertEquals(setOf("before.command"), snapshot.paths.single().permissions)
    }

    @Test
    fun `withOptionalArguments marks every supplied argument optional and keeps explicit defaults`() {
        val definition = CommandAPICommand("lookup")
            .withOptionalArguments(
                StringArgument("query"),
                IntegerArgument("page").setOptional { 7 },
            )
            .anyExecutor { _, _ -> }
            .toDefinition()

        val arguments = definition.paths.single().arguments
        assertEquals(listOf(true, true), arguments.map { it.optional })
        assertEquals(null, arguments[0].defaultValue)
        assertEquals(7, arguments[1].defaultValue?.invoke(nullSender()))
    }

    @Test
    fun `invalid command names aliases and namespaces are rejected`() {
        assertThrows(CommandValidationException::class.java) {
            CommandAPICommand("bad name").anyExecutor { _, _ -> }.toDefinition()
        }
        assertThrows(CommandValidationException::class.java) {
            CommandAPICommand("valid").withAliases("valid").anyExecutor { _, _ -> }.toDefinition()
        }
        assertThrows(CommandValidationException::class.java) {
            CommandAPICommand("valid").withAliases("Same", "same").anyExecutor { _, _ -> }.toDefinition()
        }

        val platform = CapturingPlatform()
        CommandAPI.installPlatform(platform)
        try {
            assertThrows(CommandValidationException::class.java) {
                CommandAPICommand("valid").anyExecutor { _, _ -> }.register("Bad Namespace")
            }
            assertTrue(platform.definitions.isEmpty())
        } finally {
            CommandAPI.uninstallPlatform(platform)
        }
    }

    @Test
    fun `duplicate argument names in an executable path are rejected`() {
        assertThrows(CommandValidationException::class.java) {
            CommandAPICommand("duplicate")
                .withArguments(StringArgument("value"), IntegerArgument("value"))
                .anyExecutor { _, _ -> }
                .toDefinition()
        }
    }

    @Test
    fun `required arguments after an optional suffix are rejected`() {
        assertThrows(CommandValidationException::class.java) {
            CommandAPICommand("optional")
                .withArguments(StringArgument("first").setOptional(true), StringArgument("required"))
                .anyExecutor { _, _ -> }
                .toDefinition()
        }
    }

    @Test
    fun `arguments after a greedy argument are rejected`() {
        assertThrows(CommandValidationException::class.java) {
            CommandAPICommand("greedy")
                .withArguments(GreedyStringArgument("rest"), StringArgument("later"))
                .anyExecutor { _, _ -> }
                .toDefinition()
        }
    }

    @Test
    fun `commands without an executable path are rejected`() {
        assertThrows(CommandValidationException::class.java) {
            CommandAPICommand("empty").withArguments(StringArgument("unused")).toDefinition()
        }
        assertThrows(CommandValidationException::class.java) {
            CommandTree("empty-tree").then(StringArgument("unused")).toDefinition()
        }
    }

    @Test
    fun `equal specificity executors on one path are rejected`() {
        assertThrows(CommandValidationException::class.java) {
            CommandAPICommand("ambiguous")
                .anyExecutor { _, _ -> }
                .anyExecutor { _, _ -> }
                .toDefinition()
        }

        val valid = CommandAPICommand("specific")
            .anyExecutor { _, _ -> }
            .playerExecutor { _, _ -> }
            .toDefinition()
        assertEquals(setOf(ExecutorType.ANY, ExecutorType.PLAYER), valid.paths.single().executors.map { it.type }.toSet())
    }

    @Test
    fun `command tree flattens only executable branches in depth first order`() {
        val users = LiteralArgument("users")
            .then(StringArgument("target").anyExecutor { _, _ -> })
            .then(LiteralArgument("all").anyExecutor { _, _ -> })
        val reload = LiteralArgument("reload").anyExecutor { _, _ -> }

        val definition = CommandTree("admin")
            .withPermission("lobby.admin")
            .then(users)
            .then(reload)
            .toDefinition()

        assertEquals(
            listOf(listOf("users", "target"), listOf("users", "all"), listOf("reload")),
            definition.paths.map { path -> path.arguments.map { it.nodeName } },
        )
        assertTrue(definition.paths.all { it.permissions == setOf("lobby.admin") })
    }

    @Test
    fun `indirect argument child cycles are rejected during snapshotting`() {
        val first = LiteralArgument("first")
        val second = LiteralArgument("second")
        first.then(second)
        second.then(first)

        assertThrows(CommandValidationException::class.java) {
            CommandTree("cycle").then(first).toDefinition()
        }
    }

    @Test
    fun `one argument may be shared by separate acyclic branches`() {
        val shared = StringArgument("value").anyExecutor { _, _ -> }
        val definition = CommandTree("shared")
            .then(LiteralArgument("left").then(shared))
            .then(LiteralArgument("right").then(shared))
            .toDefinition()

        assertEquals(
            listOf(listOf("left", "value"), listOf("right", "value")),
            definition.paths.map { path -> path.arguments.map { it.nodeName } },
        )
    }

    @Test
    fun `platform install is exclusive and uninstall only removes the same instance`() {
        val installed = CapturingPlatform()
        val other = CapturingPlatform()
        CommandAPI.installPlatform(installed)
        try {
            assertThrows(IllegalStateException::class.java) { CommandAPI.installPlatform(other) }
            CommandAPI.uninstallPlatform(other)

            val registered = CommandAPICommand("still-installed").anyExecutor { _, _ -> }.register()
            assertEquals("still-installed", registered.name)
            assertEquals(listOf("still-installed"), installed.definitions.map { it.name })
        } finally {
            CommandAPI.uninstallPlatform(installed)
        }
    }

    @Test
    fun `registration and unregistration without a platform describe the lifecycle error`() {
        val registrationFailure = assertThrows(IllegalStateException::class.java) {
            CommandAPICommand("early").anyExecutor { _, _ -> }.register()
        }
        val unregistrationFailure = assertThrows(IllegalStateException::class.java) {
            CommandAPI.unregister("early")
        }

        assertTrue(registrationFailure.message.orEmpty().contains("platform", ignoreCase = true))
        assertTrue(registrationFailure.message.orEmpty().contains("installed", ignoreCase = true))
        assertTrue(unregistrationFailure.message.orEmpty().contains("platform", ignoreCase = true))
    }

    @Test
    fun `auto registering DSL captures a frozen definition and namespace`() {
        val platform = CapturingPlatform()
        CommandAPI.installPlatform(platform)
        try {
            val registration = commandAPICommand("dsl", "lobby") {
                integerArgument("amount")
                anyExecutor { _, _ -> }
            }

            assertEquals("dsl", registration.name)
            assertEquals(listOf("dsl"), platform.definitions.map { it.name })
            assertEquals(listOf("amount"), platform.definitions.single().paths.single().arguments.map { it.nodeName })
            assertEquals(listOf("lobby"), platform.namespaces)
        } finally {
            CommandAPI.uninstallPlatform(platform)
        }
    }

    @Test
    fun `syntax failure helpers retain text and adventure messages`() {
        val textFailure = assertThrows(CommandSyntaxException::class.java) {
            CommandAPI.failWithString("bad input")
        }
        val component = Component.text("component input")
        val componentFailure = assertThrows(CommandSyntaxException::class.java) {
            CommandAPI.failWithMessage(component)
        }

        val plain = PlainTextComponentSerializer.plainText()
        assertEquals("bad input", plain.serialize(checkNotNull(textFailure.component)))
        assertSame(component, componentFailure.component)
    }

    private fun nullSender(): CommandSender = Proxy.newProxyInstance(
        CommandSender::class.java.classLoader,
        arrayOf(CommandSender::class.java),
    ) { _, _, _ -> null } as CommandSender

    private class CapturingPlatform : CommandAPIPlatform {
        val definitions = mutableListOf<CommandDefinition>()
        val namespaces = mutableListOf<String?>()

        override fun register(definition: CommandDefinition, namespace: String?): RegisteredCommand {
            definitions += definition
            namespaces += namespace
            return RegisteredCommand(definition.name, definition.aliases, namespace)
        }

        override fun unregister(name: String): Boolean = definitions.removeIf { it.name == name }

        override fun execute(sender: CommandSender, input: String): Int =
            throw UnsupportedOperationException("This platform only captures registrations")
    }
}
