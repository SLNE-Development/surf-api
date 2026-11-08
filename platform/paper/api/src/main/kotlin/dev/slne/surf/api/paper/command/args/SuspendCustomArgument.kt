package dev.slne.surf.api.paper.command.args

import com.github.shynixn.mccoroutine.folia.SuspendingPlugin
import com.github.shynixn.mccoroutine.folia.scope
import com.mojang.brigadier.context.CommandContext
import dev.jorel.commandapi.CommandAPIBukkit
import dev.jorel.commandapi.CommandAPIHandler
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.CustomArgument
import dev.jorel.commandapi.executors.CommandArguments
import dev.slne.surf.api.paper.util.getCallingPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

/**
 * Base class for custom CommandAPI arguments whose parsing logic is executed asynchronously
 * using Kotlin coroutines.
 *
 * This abstraction allows implementing suspendable parsing logic while still integrating
 * with CommandAPI's synchronous argument parsing pipeline by returning a [Deferred].
 *
 * @param T The final parsed argument type.
 * @param B The base argument type produced by the underlying [Argument].
 * @property base The underlying base argument used for initial parsing.
 * @property scope The [CoroutineScope] used to execute the suspendable parsing logic.
 */
abstract class SuspendCustomArgument<T, B>(
    private val base: Argument<B>,
    private val scope: CoroutineScope = extractCallingPluginScopeOrThrow()
) : CustomArgument<Deferred<T>, B>(base, { _ ->
    throw UnsupportedOperationException("Parsing is performed asynchronously via the overridden parse method")
}) {

    /**
     * Parses the argument asynchronously.
     *
     * This method is executed inside the provided [CoroutineScope] and may perform
     * suspendable or asynchronous operations such as database lookups or network calls.
     *
     * Implementations should throw [CustomArgumentException] to signal a user-facing
     * parsing error.
     *
     * @param info Contextual information about the argument being parsed.
     * @return The parsed argument value.
     * @throws CustomArgumentException If parsing fails with a user-facing error.
     */
    abstract suspend fun CoroutineScope.parse(info: CustomArgumentInfo<B>): T

    /**
     * Synchronously invoked by CommandAPI to parse the argument.
     *
     * Internally, this method delegates to [parse] by launching a coroutine in [scope]
     * and returning a [Deferred] representing the eventual parsing result.
     *
     * Any thrown [CustomArgumentException] is converted into a Brigadier
     * [com.mojang.brigadier.exceptions.CommandSyntaxException].
     *
     * @param cmdCtx The Brigadier command context.
     * @param key The argument node name.
     * @param previousArgs Previously parsed command arguments.
     * @return A [Deferred] that completes with the parsed argument value.
     */
    final override fun <CommandSourceStack : Any> parseArgument(
        cmdCtx: CommandContext<CommandSourceStack>,
        key: String,
        previousArgs: CommandArguments
    ): Deferred<T> {
        val customResult = CommandAPIHandler.getRawArgumentInput(cmdCtx, key)
        val parsedInput = base.parseArgument(cmdCtx, key, previousArgs)

        return scope.async {
            try {
                val sender = CommandAPIBukkit.get<CommandSourceStack>()
                    .getCommandSenderFromCommandSource(cmdCtx.source).source
                val info = CustomArgumentInfo(sender, previousArgs, customResult, parsedInput)
                parse(info)
            } catch (e: CustomArgumentException) {
                throw e.toCommandSyntax(customResult, cmdCtx)
            }
        }
    }

    companion object {
        /**
         * Extracts the [CoroutineScope] of the calling plugin.
         *
         * The calling plugin must implement [SuspendingPlugin]. If it does not,
         * an exception is thrown to prevent executing coroutines without
         * proper lifecycle management.
         *
         * @return The plugin's coroutine scope.
         * @throws IllegalArgumentException If the plugin does not implement [SuspendingPlugin].
         */
        private fun extractCallingPluginScopeOrThrow(): CoroutineScope {
            val plugin = getCallingPlugin(2)

            require(plugin is SuspendingPlugin) {
                "Failed to extract CoroutineScope: plugin '${plugin.name}' does not implement SuspendingPlugin. " +
                        "Provide a CoroutineScope explicitly or use a SuspendingPlugin."
            }

            return plugin.scope
        }
    }
}