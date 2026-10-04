package dev.slne.surf.api.minestom.server.impl.command

import dev.slne.surf.api.minestom.command.CommandAPI
import dev.slne.surf.api.minestom.command.argument.EnumArgument
import dev.slne.surf.api.minestom.command.argument.StringArgument
import dev.slne.surf.api.minestom.command.argument.SuggestionMode
import dev.slne.surf.api.minestom.command.dsl.commandTree
import dev.slne.surf.api.minestom.command.dsl.consoleExecutor
import dev.slne.surf.api.minestom.command.dsl.consoleResultingExecutor
import dev.slne.surf.api.minestom.command.dsl.includeSuggestionsWithTooltipsAsync
import dev.slne.surf.api.minestom.command.dsl.listArgument
import dev.slne.surf.api.minestom.command.dsl.literalArgument
import dev.slne.surf.api.minestom.command.dsl.playerArgument
import dev.slne.surf.api.minestom.command.dsl.replaceSuggestions
import dev.slne.surf.api.minestom.command.dsl.textArgument
import dev.slne.surf.api.minestom.command.executor.CommandArguments
import dev.slne.surf.api.minestom.command.executor.ParsedArgument
import dev.slne.surf.api.minestom.command.suggestion.ArgumentSuggestions
import dev.slne.surf.api.minestom.command.suggestion.StringTooltip
import dev.slne.surf.api.minestom.command.suggestion.SuggestionInfo
import dev.slne.surf.api.minestom.command.suggestion.SuggestionsBranch
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.text.Component
import net.minestom.server.command.builder.CommandResult
import net.minestom.server.coordinate.Pos
import net.minestom.server.crypto.ChatSession
import net.minestom.server.entity.Player
import net.minestom.server.event.EventNode
import net.minestom.server.instance.Instance
import net.minestom.server.network.packet.client.play.ClientTabCompletePacket
import net.minestom.server.network.packet.server.play.TabCompletePacket
import net.minestom.server.network.player.GameProfile
import net.minestom.server.network.player.PlayerConnection
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import net.minestom.testing.TestConnection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@EnvTest
class CommandTreeIntegrationTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `admin tree branches through literals optional arguments async tooltips and a resulting console executor`(
        env: Env,
    ) = runTest {
        val manager = env.process().command()
        val ownership = MinestomCommandOwnership()
        val listener = MinestomSuggestionListener(ownership)
        val platform = MinestomCommandAPIPlatform(manager, ownership, suggestionScope = { this })
        val node = EventNode.all("admin-tree-test")
        CommandAPI.installPlatform(platform)
        listener.register(node)
        env.process().eventHandler().addChild(node)
        env.process().connection().setPlayerProvider { connection, profile ->
            TestPermissionPlayer(connection, profile, setOf("lobby.admin"))
        }
        try {
            val reloadCount = AtomicInteger()
            val kicked = mutableListOf<Pair<String, String>>()
            val tooltipInvocations = mutableListOf<String>()

            commandTree("admin") {
                withPermission("lobby.admin")
                literalArgument("reload") {
                    consoleResultingExecutor { _, _ -> reloadCount.incrementAndGet() }
                }
                literalArgument("kick") {
                    playerArgument("target") {
                        textArgument("reason") {
                            setOptional("no reason given")
                            includeSuggestionsWithTooltipsAsync { info ->
                                tooltipInvocations += info.currentArg
                                listOf(StringTooltip("griefing", Component.text("Breaking blocks")))
                            }
                            consoleExecutor { _, args ->
                                val target: Player by args
                                val reason: String by args
                                kicked += target.username to reason
                            }
                        }
                    }
                }
                listArgument("tags", EnumArgument("tag", Tag.entries)) {
                    consoleExecutor { _, _ -> }
                }
            }

            val reloadResult = runCommandForResult(manager.consoleSender, "admin reload")
            assertEquals(1, reloadResult)
            assertEquals(1, reloadCount.get())

            val instance = env.createEmptyInstance()
            var target: Player? = null
            try {
                val connection = env.createConnection(profile("Offender"))
                target = connect(connection, instance)

                assertTrue(runCommand(manager.consoleSender, "admin kick Offender"))
                assertEquals(listOf("Offender" to "no reason given"), kicked)

                assertTrue(runCommand(manager.consoleSender, "admin kick Offender \"breaking stuff\""))
                assertEquals(2, kicked.size)
                assertEquals("breaking stuff", kicked[1].second)

                val packets = connection.trackIncoming(TabCompletePacket::class.java)
                env.process().packetListener().processClientPacket(
                    ClientTabCompletePacket(1, "/admin kick Offender gr"),
                    target.playerConnection,
                )
                runCurrent()
                packets.assertSingle { packet ->
                    assertTrue(packet.matches.any { match -> match.match == "griefing" })
                }
                assertEquals(1, tooltipInvocations.size)

                assertTrue(runCommand(manager.consoleSender, "admin low,high"))
            } finally {
                target?.remove()
                env.destroyInstance(instance)
            }
        } finally {
            platform.close()
            CommandAPI.uninstallPlatform(platform)
            env.process().eventHandler().removeChild(node)
            env.process().connection().setPlayerProvider(null)
        }
    }

    @Test
    fun `suspend dependent suggestions branch resolves once the provider resumes and adapts to prior arguments`(
        env: Env,
    ) = runTest {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val channels = ArgumentSuggestions.stringsAsync {
            entered.complete(Unit)
            release.await()
            listOf("staff", "public")
        }
        val staffTargets = ArgumentSuggestions.strings("Deniz")
        val branch = SuggestionsBranch.suggest(channels).branch(SuggestionsBranch.suggest(staffTargets))

        val channel = StringArgument("channel").replaceSuggestions(branch)
        val mode = channel.toDefinition().suggestions
        check(mode is SuggestionMode.Replace)

        val sender = env.process().command().consoleSender
        val headSuggestions = async {
            mode.provider.suggest(SuggestionInfo(sender, CommandArguments.empty(), "", ""))
        }
        entered.await()
        assertFalse(headSuggestions.isCompleted)
        release.complete(Unit)
        assertEquals(listOf("staff", "public"), headSuggestions.await().map(StringTooltip::suggestion))

        val previousChannel = CommandArguments.of(listOf(ParsedArgument("channel", "staff", "staff", true)))
        val nested = branch.getNextSuggestion(sender, "staff")
        assertEquals(
            listOf("Deniz"),
            nested?.suggest(SuggestionInfo(sender, previousChannel, "staff ", ""))?.map(StringTooltip::suggestion),
        )
    }

    @Test
    fun `tree root executor runs the bare command while branches keep their own executors`(env: Env) {
        val manager = env.process().command()
        val ownership = MinestomCommandOwnership()
        val platform = MinestomCommandAPIPlatform(manager, ownership)
        CommandAPI.installPlatform(platform)
        try {
            val queried = AtomicInteger()
            val applied = mutableListOf<Difficulty>()

            commandTree("difficulty") {
                executes { _, _ -> queried.incrementAndGet() }
                then(
                    EnumArgument("value", Difficulty.entries).executes { _, args ->
                        val value: Difficulty by args
                        applied += value
                    },
                )
            }

            val console = manager.consoleSender
            assertTrue(runCommand(console, "difficulty"))
            assertEquals(1, queried.get())
            assertTrue(applied.isEmpty())

            assertTrue(runCommand(console, "difficulty high"))
            assertEquals(listOf(Difficulty.HIGH), applied)
            assertEquals(1, queried.get())
        } finally {
            platform.close()
            CommandAPI.uninstallPlatform(platform)
        }
    }

    @Test
    fun `input matching no branch is rejected instead of falling through to the root executor`(env: Env) {
        val manager = env.process().command()
        val ownership = MinestomCommandOwnership()
        val platform = MinestomCommandAPIPlatform(manager, ownership)
        CommandAPI.installPlatform(platform)
        try {
            val queried = AtomicInteger()
            val applied = mutableListOf<Difficulty>()

            commandTree("difficulty-fallback") {
                executes { _, _ -> queried.incrementAndGet() }
                then(
                    EnumArgument("value", Difficulty.entries).executes { _, args ->
                        val value: Difficulty by args
                        applied += value
                    },
                )
            }

            runCommand(manager.consoleSender, "difficulty-fallback bogus")

            assertTrue(applied.isEmpty())
            assertEquals(0, queried.get())
        } finally {
            platform.close()
            CommandAPI.uninstallPlatform(platform)
        }
    }

    @Test
    fun `tree with only a root executor and no branches registers and executes`(env: Env) {
        val manager = env.process().command()
        val ownership = MinestomCommandOwnership()
        val platform = MinestomCommandAPIPlatform(manager, ownership)
        CommandAPI.installPlatform(platform)
        try {
            val runs = AtomicInteger()
            commandTree("ping") {
                executes { _, _ -> runs.incrementAndGet() }
            }

            assertTrue(runCommand(manager.consoleSender, "ping"))
            assertEquals(1, runs.get())
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

    private fun profile(name: String) = GameProfile(UUID.randomUUID(), name)

    private enum class Tag {
        LOW,
        HIGH,
    }

    private enum class Difficulty {
        PEACEFUL,
        HIGH,
    }
}
