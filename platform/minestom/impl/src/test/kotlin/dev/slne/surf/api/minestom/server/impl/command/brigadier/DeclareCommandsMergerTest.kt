package dev.slne.surf.api.minestom.server.impl.command.brigadier

import dev.slne.surf.api.minestom.command.CommandAPI
import dev.slne.surf.api.minestom.command.CommandAPICommand
import dev.slne.surf.api.minestom.command.argument.*
import dev.slne.surf.api.minestom.command.dsl.anyExecutor
import dev.slne.surf.api.minestom.command.dsl.commandTree
import dev.slne.surf.api.minestom.command.suggestion.ArgumentSuggestions
import dev.slne.surf.api.minestom.server.impl.command.MinestomCommandAPIPlatform
import dev.slne.surf.api.minestom.server.impl.command.MinestomCommandOwnership
import net.minestom.server.command.ArgumentParserType
import net.minestom.server.command.CommandManager
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.arguments.minecraft.SuggestionType
import net.minestom.server.network.packet.server.play.DeclareCommandsPacket
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests that merged nodes naming `ask_server` also carry
 * [DeclareCommandsPacket.HAS_SUGGESTION_TYPE]. The packet writes a suggestion type only when that
 * flag is set, so without it the client receives a node that offers no suggestions.
 */
@EnvTest
class DeclareCommandsMergerTest {

    @Test
    fun `a merged node that asks the server says so in its flags`(env: Env) {
        val manager = env.process().command()
        val platform = MinestomCommandAPIPlatform(manager, MinestomCommandOwnership())
        CommandAPI.installPlatform(platform)
        try {
            CommandAPICommand("declare-suggestions")
                .withArguments(
                    CustomArgument(StringArgument("server")) { info -> info.baseValue }
                        .replaceSuggestions(ArgumentSuggestions.strings("lobby-1")),
                    IntegerArgument("count"),
                )
                .anyExecutor { _, _ -> }
                .register()

            val merged = merge(manager.consoleSender)
            val server = merged.nodes.single { node -> node.name == "server" }
            val count = merged.nodes.single { node -> node.name == "count" }

            assertEquals(SuggestionType.ASK_SERVER.identifier, server.suggestionsType)
            assertNotEquals(
                0,
                server.flags.toInt() and DeclareCommandsPacket.HAS_SUGGESTION_TYPE,
                "the suggestion type of 'server' is not serialized without its flag",
            )
            assertEquals(
                0,
                count.flags.toInt() and DeclareCommandsPacket.HAS_SUGGESTION_TYPE,
                "'count' carries no suggestion type to announce",
            )
        } finally {
            try {
                platform.close()
            } finally {
                CommandAPI.uninstallPlatform(platform)
            }
        }
    }

    @Test
    fun `a command argument is declared with a parser`(env: Env) {
        withPlatform(env) { manager ->
            CommandAPICommand("declare-command")
                .withArguments(CommandArgument("inner"))
                .anyExecutor { _, _ -> }
                .register()

            val inner = merge(manager.consoleSender).nodes.single { node -> node.name == "inner" }

            assertEquals(ArgumentParserType.STRING, inner.parser)
        }
    }

    @Test
    fun `a path cut at an unusable argument leaves no dead end prefix`(env: Env) {
        withPlatform(env) { manager ->
            commandTree("declare-prune") {
                then(
                    LiteralArgument("admin").then(
                        LiteralArgument("reload").withRequirement { false }.executes { _, _ -> },
                    ),
                )
                then(LiteralArgument("list").executes { _, _ -> })
            }

            val names = merge(manager.consoleSender).nodes.map { node -> node.name }

            assertTrue("list" in names)
            assertFalse("admin" in names)
            assertFalse("reload" in names)
        }
    }

    private inline fun withPlatform(env: Env, block: (CommandManager) -> Unit) {
        val manager = env.process().command()
        val platform = MinestomCommandAPIPlatform(manager, MinestomCommandOwnership())
        CommandAPI.installPlatform(platform)
        try {
            block(manager)
        } finally {
            try {
                platform.close()
            } finally {
                CommandAPI.uninstallPlatform(platform)
            }
        }
    }

    /**
     * The CommandAPI's commands merged into a tree that contains only a root, the only part of
     * Minestom's packet the merger reads.
     */
    private fun merge(sender: CommandSender): DeclareCommandsPacket {
        val root = DeclareCommandsPacket.Node().apply {
            flags = DeclareCommandsPacket.getFlag(
                DeclareCommandsPacket.NodeType.ROOT,
                false,
                false,
                false,
            )
        }
        val merger = MinestomCommandAPIPlatform.activeMerger()
            ?: error("No Minestom CommandAPI platform is installed")
        return merger.merge(DeclareCommandsPacket(listOf(root), 0), sender)
    }
}
