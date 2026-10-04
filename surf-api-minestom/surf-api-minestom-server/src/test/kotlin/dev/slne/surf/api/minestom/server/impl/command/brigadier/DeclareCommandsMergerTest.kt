package dev.slne.surf.api.minestom.server.impl.command.brigadier

import dev.slne.surf.api.minestom.command.CommandAPI
import dev.slne.surf.api.minestom.command.CommandAPICommand
import dev.slne.surf.api.minestom.command.argument.CustomArgument
import dev.slne.surf.api.minestom.command.argument.IntegerArgument
import dev.slne.surf.api.minestom.command.argument.StringArgument
import dev.slne.surf.api.minestom.command.dsl.anyExecutor
import dev.slne.surf.api.minestom.command.suggestion.ArgumentSuggestions
import dev.slne.surf.api.minestom.server.impl.command.MinestomCommandAPIPlatform
import dev.slne.surf.api.minestom.server.impl.command.MinestomCommandOwnership
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.arguments.minecraft.SuggestionType
import net.minestom.server.network.packet.server.play.DeclareCommandsPacket
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

/**
 * A node's suggestion type is only written to the packet when its flags carry
 * [DeclareCommandsPacket.HAS_SUGGESTION_TYPE], so a merged node that names `ask_server` without that
 * flag reaches the client as one offering no completions at all.
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
            platform.close()
            CommandAPI.uninstallPlatform(platform)
        }
    }

    /**
     * The CommandAPI's commands merged into a tree holding nothing but a root, which is all the
     * merger reads of the packet Minestom produced.
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
