package dev.slne.surf.api.minestom.server.impl.command

import dev.slne.surf.api.minestom.command.CommandAPI
import dev.slne.surf.api.minestom.command.CommandAPICommand
import dev.slne.surf.api.minestom.command.CommandTree
import dev.slne.surf.api.minestom.command.argument.Argument
import dev.slne.surf.api.minestom.command.argument.ArgumentKind
import dev.slne.surf.api.minestom.command.argument.IntegerArgument
import dev.slne.surf.api.minestom.command.argument.LiteralArgument
import dev.slne.surf.api.minestom.command.argument.PlayerArgument
import dev.slne.surf.api.minestom.command.argument.StringArgument
import dev.slne.surf.api.minestom.command.argument.SuggestionMode
import dev.slne.surf.api.minestom.command.dsl.anyExecutionInfo
import dev.slne.surf.api.minestom.command.dsl.anyExecutor
import dev.slne.surf.api.minestom.command.dsl.booleanArgument
import dev.slne.surf.api.minestom.command.dsl.commandAPICommand
import dev.slne.surf.api.minestom.command.dsl.consoleExecutorSuspend
import dev.slne.surf.api.minestom.command.dsl.doubleArgument
import dev.slne.surf.api.minestom.command.dsl.enumArgument
import dev.slne.surf.api.minestom.command.dsl.floatArgument
import dev.slne.surf.api.minestom.command.dsl.floatRangeArgument
import dev.slne.surf.api.minestom.command.dsl.greedyStringArgument
import dev.slne.surf.api.minestom.command.dsl.includeSafeSuggestions
import dev.slne.surf.api.minestom.command.dsl.includeSafeSuggestionsAsync
import dev.slne.surf.api.minestom.command.dsl.includeSafeSuggestionsWithTooltips
import dev.slne.surf.api.minestom.command.dsl.includeSafeSuggestionsWithTooltipsAsync
import dev.slne.surf.api.minestom.command.dsl.includeSuggestions
import dev.slne.surf.api.minestom.command.dsl.includeSuggestionsAsync
import dev.slne.surf.api.minestom.command.dsl.includeSuggestionsWithTooltips
import dev.slne.surf.api.minestom.command.dsl.includeSuggestionsWithTooltipsAsync
import dev.slne.surf.api.minestom.command.dsl.integerRangeArgument
import dev.slne.surf.api.minestom.command.dsl.literalArgument
import dev.slne.surf.api.minestom.command.dsl.longArgument
import dev.slne.surf.api.minestom.command.dsl.multiLiteralArgument
import dev.slne.surf.api.minestom.command.dsl.playerArgument
import dev.slne.surf.api.minestom.command.dsl.playerExecutorSuspend
import dev.slne.surf.api.minestom.command.dsl.replaceSafeSuggestions
import dev.slne.surf.api.minestom.command.dsl.replaceSafeSuggestionsAsync
import dev.slne.surf.api.minestom.command.dsl.replaceSafeSuggestionsWithTooltips
import dev.slne.surf.api.minestom.command.dsl.replaceSafeSuggestionsWithTooltipsAsync
import dev.slne.surf.api.minestom.command.dsl.replaceSuggestions
import dev.slne.surf.api.minestom.command.dsl.replaceSuggestionsAsync
import dev.slne.surf.api.minestom.command.dsl.replaceSuggestionsWithTooltips
import dev.slne.surf.api.minestom.command.dsl.replaceSuggestionsWithTooltipsAsync
import dev.slne.surf.api.minestom.command.dsl.textArgument
import dev.slne.surf.api.minestom.command.dsl.uuidArgument
import dev.slne.surf.api.minestom.command.exception.CommandValidationException
import dev.slne.surf.api.minestom.command.executor.CommandArguments
import dev.slne.surf.api.minestom.command.suggestion.ArgumentSuggestions
import dev.slne.surf.api.minestom.command.suggestion.StringTooltip
import dev.slne.surf.api.minestom.command.suggestion.SuggestionInfo
import dev.slne.surf.api.minestom.command.suggestion.SuggestionsBranch
import dev.slne.surf.api.minestom.command.suggestion.Tooltip
import java.util.UUID
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.text.Component
import net.minestom.server.command.builder.CommandResult
import net.minestom.server.coordinate.Pos
import net.minestom.server.crypto.ChatSession
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.network.player.GameProfile
import net.minestom.server.network.player.PlayerConnection
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import net.minestom.testing.TestConnection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@EnvTest
class KotlinDslIntegrationTest {
    @Test
    fun `message command resolves player suggestions and dispatches through a suspend player executor`(
        env: Env,
    ) = runTest {
        env.process().connection().setPlayerProvider { connection, profile ->
            TestPermissionPlayer(connection, profile, setOf("lobby.command.message"))
        }
        try {
            withPlatform(env) { manager ->
                val directory = PlayerDirectory(listOf("Ranger", "Zeta"))
                val deliveries = mutableListOf<Triple<Player, Player, String>>()
                val testScope = this

                commandAPICommand("message") {
                    withPermission("lobby.command.message")
                    playerArgument("target") {
                        replaceSuggestionsAsync { info -> directory.search(info.currentArg) }
                    }
                    greedyStringArgument("message")
                    playerExecutorSuspend(scope = { testScope }) { player, args ->
                        val target: Player by args
                        val message: String by args
                        deliveries += Triple(player, target, message)
                    }
                }

                val instance = env.createEmptyInstance()
                var sender: Player? = null
                var target: Player? = null
                try {
                    sender = connect(env.createConnection(profile("Sender")), instance)
                    target = connect(env.createConnection(profile("Ranger")), instance)

                    val result = runCommand(sender, "message Ranger hello there")

                    assertTrue(result)
                    assertTrue(deliveries.isEmpty())
                    runCurrent()
                    assertEquals(listOf(Triple(sender, target, "hello there")), deliveries)
                } finally {
                    target?.remove()
                    sender?.remove()
                    env.destroyInstance(instance)
                }
            }
        } finally {
            env.process().connection().setPlayerProvider(null)
        }
    }

    @Test
    fun `ExecutorDsl exposes ExecutionInfo executes and suspend facades alongside the executor package`(
        env: Env,
    ) = runTest {
        withPlatform(env) { manager ->
            val infoInputs = mutableListOf<String>()
            val executesCalls = mutableListOf<String>()
            val suspendCalls = mutableListOf<String>()
            val testScope = this

            CommandAPICommand("execution-info-facade")
                .anyExecutionInfo { info -> infoInputs += info.input }
                .register()
            CommandAPICommand("executes-facade")
                .executes { _, _ -> executesCalls += "invoked" }
                .register()
            CommandAPICommand("console-suspend-facade")
                .consoleExecutorSuspend(scope = { testScope }) { _, _ -> suspendCalls += "invoked" }
                .register()

            val argumentExecutesCalls = mutableListOf<String>()
            CommandAPICommand("executes-facade-argument")
                .literalArgument("go") {
                    executesConsole { _, _ -> argumentExecutesCalls += "invoked" }
                }
                .register()

            assertTrue(runCommand(manager.consoleSender, "execution-info-facade"))
            assertEquals(listOf("execution-info-facade"), infoInputs)

            assertTrue(runCommand(manager.consoleSender, "executes-facade"))
            assertEquals(listOf("invoked"), executesCalls)

            assertTrue(runCommand(manager.consoleSender, "console-suspend-facade"))
            assertTrue(suspendCalls.isEmpty())
            runCurrent()
            assertEquals(listOf("invoked"), suspendCalls)

            assertTrue(runCommand(manager.consoleSender, "executes-facade-argument go"))
            assertEquals(listOf("invoked"), argumentExecutesCalls)
        }
    }

    @Test
    fun `replaceSuggestionsAsync stores a suspending provider that resolves against the current argument`(
        env: Env,
    ) = runTest {
        val directory = PlayerDirectory(listOf("Ranger", "Zeta"))
        val argument = PlayerArgument("target")
            .replaceSuggestionsAsync { info -> directory.search(info.currentArg) }

        val mode = argument.toDefinition().suggestions
        check(mode is SuggestionMode.Replace)
        val info = suggestionInfo(env, currentArg = "Ra")

        assertEquals(listOf("Ranger"), mode.provider.suggest(info).map(StringTooltip::suggestion))
    }

    @Test
    fun `every new argument builder exists on the CommandAPICommand receiver family`() {
        val definition = CommandAPICommand("primitive-catalog")
            .longArgument("along", min = 1, max = 10)
            .floatArgument("afloat", min = 1f, max = 10f)
            .doubleArgument("adouble", min = 1.0, max = 10.0)
            .enumArgument("mode", CatalogMode.entries)
            .uuidArgument("id")
            .integerRangeArgument("irange")
            .floatRangeArgument("frange")
            .multiLiteralArgument("choice", "left", "right")
            .textArgument("text")
            .booleanArgument("flag", optional = true)
            .greedyStringArgument("rest", optional = true)
            .anyExecutor { _, _ -> }
            .toDefinition()

        val kinds = definition.paths.single().arguments.associate { it.nodeName to it.kind }
        assertEquals(ArgumentKind.Long(1, 10), kinds.getValue("along"))
        assertEquals(ArgumentKind.Float(1f, 10f), kinds.getValue("afloat"))
        assertEquals(ArgumentKind.Double(1.0, 10.0), kinds.getValue("adouble"))
        assertTrue(kinds.getValue("mode") is ArgumentKind.Enum<*>)
        assertEquals(ArgumentKind.Uuid, kinds.getValue("id"))
        assertEquals(ArgumentKind.IntegerRange, kinds.getValue("irange"))
        assertEquals(ArgumentKind.FloatRange, kinds.getValue("frange"))
        assertEquals(
            ArgumentKind.MultiLiteral(listOf("left", "right")),
            kinds.getValue("choice"),
        )
        assertEquals(ArgumentKind.Boolean, kinds.getValue("flag"))
        assertEquals(ArgumentKind.Text, kinds.getValue("text"))
        assertEquals(ArgumentKind.GreedyString, kinds.getValue("rest"))
        assertTrue(definition.paths.single().arguments.first { it.nodeName == "flag" }.optional)
    }

    @Test
    fun `every new argument builder exists on the CommandTree receiver family`() {
        val tree = CommandTree("primitive-catalog-tree")
            .longArgument("along") { anyExecutor { _, _ -> } }
            .floatArgument("afloat") { anyExecutor { _, _ -> } }
            .doubleArgument("adouble") { anyExecutor { _, _ -> } }
            .enumArgument("mode", CatalogMode.entries) { anyExecutor { _, _ -> } }
            .uuidArgument("id") { anyExecutor { _, _ -> } }
            .integerRangeArgument("irange") { anyExecutor { _, _ -> } }
            .floatRangeArgument("frange") { anyExecutor { _, _ -> } }
            .multiLiteralArgument("choice", "left", "right") { anyExecutor { _, _ -> } }
            .booleanArgument("flag") { anyExecutor { _, _ -> } }
            .textArgument("text") { anyExecutor { _, _ -> } }
            .greedyStringArgument("rest") { anyExecutor { _, _ -> } }
            .toDefinition()

        assertEquals(
            listOf(
                listOf("along"),
                listOf("afloat"),
                listOf("adouble"),
                listOf("mode"),
                listOf("id"),
                listOf("irange"),
                listOf("frange"),
                listOf("choice"),
                listOf("flag"),
                listOf("text"),
                listOf("rest"),
            ),
            tree.paths.map { path -> path.arguments.map { it.nodeName } },
        )
    }

    @Test
    fun `every new argument builder exists on the nested Argument receiver family`() {
        val tree = CommandTree("primitive-catalog-child")
            .then(
                StringArgument("prefix")
                    .longArgument("along") { anyExecutor { _, _ -> } }
                    .floatArgument("afloat") { anyExecutor { _, _ -> } }
                    .doubleArgument("adouble") { anyExecutor { _, _ -> } }
                    .enumArgument("mode", CatalogMode.entries) { anyExecutor { _, _ -> } }
                    .uuidArgument("id") { anyExecutor { _, _ -> } }
                    .integerRangeArgument("irange") { anyExecutor { _, _ -> } }
                    .floatRangeArgument("frange") { anyExecutor { _, _ -> } }
                    .multiLiteralArgument("choice", "left", "right") { anyExecutor { _, _ -> } }
                    .booleanArgument("flag") { anyExecutor { _, _ -> } }
                    .textArgument("text") { anyExecutor { _, _ -> } }
                    .greedyStringArgument("rest") { anyExecutor { _, _ -> } },
            )
            .toDefinition()

        assertEquals(
            listOf(
                listOf("prefix", "along"),
                listOf("prefix", "afloat"),
                listOf("prefix", "adouble"),
                listOf("prefix", "mode"),
                listOf("prefix", "id"),
                listOf("prefix", "irange"),
                listOf("prefix", "frange"),
                listOf("prefix", "choice"),
                listOf("prefix", "flag"),
                listOf("prefix", "text"),
                listOf("prefix", "rest"),
            ),
            tree.paths.map { path -> path.arguments.map { it.nodeName } },
        )
    }

    @Test
    fun `string suggestion DSL family covers vararg provider and async forms for replace and include`(
        env: Env,
    ) = runTest {
        val info = suggestionInfo(env, currentArg = "a")

        // A lambda literal resolves to the `replaceSuggestions(ArgumentSuggestions)` /
        // `includeSuggestions(ArgumentSuggestions)` member; an explicitly typed
        // `(SuggestionInfo) -> Collection<String>` selects the provider overload.
        val stringProvider: (SuggestionInfo) -> Collection<String> = { listOf("provided") }
        val replaceVararg = StringArgument("a").replaceSuggestions("apple", "avocado")
        val replaceProvider = StringArgument("a").replaceSuggestions(stringProvider)
        val replaceAsync = StringArgument("a").replaceSuggestionsAsync { _ -> listOf("async") }
        val includeVararg = StringArgument("a").includeSuggestions("apple")
        val includeProvider = StringArgument("a").includeSuggestions(stringProvider)
        val includeAsync = StringArgument("a").includeSuggestionsAsync { _ -> listOf("async") }

        assertEquals(listOf("apple", "avocado"), suggestedStrings(replaceVararg, info, replace = true))
        assertEquals(listOf("provided"), suggestedStrings(replaceProvider, info, replace = true))
        assertEquals(listOf("async"), suggestedStrings(replaceAsync, info, replace = true))
        assertEquals(listOf("apple"), suggestedStrings(includeVararg, info, replace = false))
        assertEquals(listOf("provided"), suggestedStrings(includeProvider, info, replace = false))
        assertEquals(listOf("async"), suggestedStrings(includeAsync, info, replace = false))
    }

    @Test
    fun `string tooltip suggestion DSL family covers vararg provider and async forms`(env: Env) = runTest {
        val info = suggestionInfo(env, currentArg = "a")
        val tooltip = Component.text("tip")

        val replaceVararg = StringArgument("a")
            .replaceSuggestionsWithTooltips(StringTooltip("apple", tooltip))
        val replaceProvider = StringArgument("a")
            .replaceSuggestionsWithTooltips { _ -> listOf(StringTooltip("provided", tooltip)) }
        val replaceAsync = StringArgument("a")
            .replaceSuggestionsWithTooltipsAsync { _ -> listOf(StringTooltip("async", tooltip)) }
        val includeVararg = StringArgument("a")
            .includeSuggestionsWithTooltips(StringTooltip("apple", tooltip))
        val includeProvider = StringArgument("a")
            .includeSuggestionsWithTooltips { _ -> listOf(StringTooltip("provided", tooltip)) }
        val includeAsync = StringArgument("a")
            .includeSuggestionsWithTooltipsAsync { _ -> listOf(StringTooltip("async", tooltip)) }

        assertEquals(listOf(StringTooltip("apple", tooltip)), replaceMode(replaceVararg).provider.suggest(info))
        assertEquals(listOf(StringTooltip("provided", tooltip)), replaceMode(replaceProvider).provider.suggest(info))
        assertEquals(listOf(StringTooltip("async", tooltip)), replaceMode(replaceAsync).provider.suggest(info))
        assertEquals(listOf(StringTooltip("apple", tooltip)), includeMode(includeVararg).provider.suggest(info))
        assertEquals(listOf(StringTooltip("provided", tooltip)), includeMode(includeProvider).provider.suggest(info))
        assertEquals(listOf(StringTooltip("async", tooltip)), includeMode(includeAsync).provider.suggest(info))
    }

    @Test
    fun `safe typed suggestion DSL family covers vararg provider and async forms for replace and include`(
        env: Env,
    ) = runTest {
        val info = suggestionInfo(env, currentArg = "1")

        // A lambda literal resolves to the `replaceSafeSuggestions(SafeSuggestions<T>)` /
        // `includeSafeSuggestions(SafeSuggestions<T>)` member; an explicitly typed
        // `(SuggestionInfo) -> Collection<T>` selects the provider overload.
        val replaceProviderFn: (SuggestionInfo) -> Collection<Int> = { listOf(3) }
        val includeProviderFn: (SuggestionInfo) -> Collection<Int> = { listOf(6) }
        val replaceVararg = IntegerArgument("n")
            .replaceSafeSuggestions(1, 2)
        val replaceProvider = IntegerArgument("n")
            .replaceSafeSuggestions(replaceProviderFn)
        val replaceAsync = IntegerArgument("n")
            .replaceSafeSuggestionsAsync { _ -> listOf(4) }
        val includeVararg = IntegerArgument("n")
            .includeSafeSuggestions(5)
        val includeProvider = IntegerArgument("n")
            .includeSafeSuggestions(includeProviderFn)
        val includeAsync = IntegerArgument("n")
            .includeSafeSuggestionsAsync { _ -> listOf(7) }

        assertEquals(listOf(1, 2), replaceSafeMode(replaceVararg).provider.suggest(info).map { it.suggestion })
        assertEquals(listOf(3), replaceSafeMode(replaceProvider).provider.suggest(info).map { it.suggestion })
        assertEquals(listOf(4), replaceSafeMode(replaceAsync).provider.suggest(info).map { it.suggestion })
        assertEquals(listOf(5), includeSafeMode(includeVararg).provider.suggest(info).map { it.suggestion })
        assertEquals(listOf(6), includeSafeMode(includeProvider).provider.suggest(info).map { it.suggestion })
        assertEquals(listOf(7), includeSafeMode(includeAsync).provider.suggest(info).map { it.suggestion })
    }

    @Test
    fun `safe typed tooltip suggestion DSL family covers vararg provider and async forms`(env: Env) = runTest {
        val info = suggestionInfo(env, currentArg = "1")
        val tooltip = Component.text("tip")

        val replaceVararg = IntegerArgument("n")
            .replaceSafeSuggestionsWithTooltips(Tooltip(1, tooltip))
        val replaceProvider = IntegerArgument("n")
            .replaceSafeSuggestionsWithTooltips { _ -> listOf(Tooltip(2, tooltip)) }
        val replaceAsync = IntegerArgument("n")
            .replaceSafeSuggestionsWithTooltipsAsync { _ -> listOf(Tooltip(3, tooltip)) }
        val includeVararg = IntegerArgument("n")
            .includeSafeSuggestionsWithTooltips(Tooltip(4, tooltip))
        val includeProvider = IntegerArgument("n")
            .includeSafeSuggestionsWithTooltips { _ -> listOf(Tooltip(5, tooltip)) }
        val includeAsync = IntegerArgument("n")
            .includeSafeSuggestionsWithTooltipsAsync { _ -> listOf(Tooltip(6, tooltip)) }

        assertEquals(listOf(Tooltip(1, tooltip)), replaceSafeMode(replaceVararg).provider.suggest(info))
        assertEquals(listOf(Tooltip(2, tooltip)), replaceSafeMode(replaceProvider).provider.suggest(info))
        assertEquals(listOf(Tooltip(3, tooltip)), replaceSafeMode(replaceAsync).provider.suggest(info))
        assertEquals(listOf(Tooltip(4, tooltip)), includeSafeMode(includeVararg).provider.suggest(info))
        assertEquals(listOf(Tooltip(5, tooltip)), includeSafeMode(includeProvider).provider.suggest(info))
        assertEquals(listOf(Tooltip(6, tooltip)), includeSafeMode(includeAsync).provider.suggest(info))
    }

    @Test
    fun `branch suggestion DSL adapts a SuggestionsBranch for both replace and include modes`(env: Env) = runTest {
        val sender = env.process().command().consoleSender
        val branch = SuggestionsBranch.suggest(
            ArgumentSuggestions.strings("staff", "public"),
        )

        val replaced = StringArgument("mode").replaceSuggestions(branch)
        val included = StringArgument("mode").includeSuggestions(branch)
        val info = SuggestionInfo(sender, CommandArguments.empty(), "", "")

        assertEquals(listOf("staff", "public"), replaceMode(replaced).provider.suggest(info).map(StringTooltip::suggestion))
        assertEquals(listOf("staff", "public"), includeMode(included).provider.suggest(info).map(StringTooltip::suggestion))
    }

    @Test
    fun `a suggestion mode rejected at registration is not offered as a silently broken shortcut`() {
        val command = CommandAPICommand("rejected-literal")
            .withArguments(
                LiteralArgument("fixed")
                    .replaceSuggestions("unused"),
            )
            .anyExecutor { _, _ -> }

        assertThrows(CommandValidationException::class.java) {
            MinestomCommandCompiler().compile(command.toDefinition(), namespace = null)
        }
    }

    private suspend fun suggestedStrings(argument: Argument<String>, info: SuggestionInfo, replace: Boolean): List<String> =
        if (replace) {
            replaceMode(argument).provider.suggest(info).map(StringTooltip::suggestion)
        } else {
            includeMode(argument).provider.suggest(info).map(StringTooltip::suggestion)
        }

    private fun <T> replaceMode(argument: Argument<T>): SuggestionMode.Replace<T> {
        val mode = argument.toDefinition().suggestions
        check(mode is SuggestionMode.Replace<T>)
        return mode
    }

    private fun <T> includeMode(argument: Argument<T>): SuggestionMode.Include<T> {
        val mode = argument.toDefinition().suggestions
        check(mode is SuggestionMode.Include<T>)
        return mode
    }

    private fun <T> replaceSafeMode(argument: Argument<T>): SuggestionMode.ReplaceSafe<T> {
        val mode = argument.toDefinition().suggestions
        check(mode is SuggestionMode.ReplaceSafe<T>)
        return mode
    }

    private fun <T> includeSafeMode(argument: Argument<T>): SuggestionMode.IncludeSafe<T> {
        val mode = argument.toDefinition().suggestions
        check(mode is SuggestionMode.IncludeSafe<T>)
        return mode
    }

    private fun suggestionInfo(env: Env, currentArg: String): SuggestionInfo = SuggestionInfo(
        sender = env.process().command().consoleSender,
        previousArgs = CommandArguments.empty(),
        currentInput = currentArg,
        currentArg = currentArg,
    )

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

    private fun profile(name: String) = GameProfile(UUID.randomUUID(), name)

    private class PlayerDirectory(private val names: List<String>) {
        fun search(prefix: String): List<String> = names.filter { name -> name.startsWith(prefix, ignoreCase = true) }
    }

    private enum class CatalogMode {
        FIRST,
        SECOND,
    }
}
