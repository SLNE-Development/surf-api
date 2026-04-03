package dev.slne.surf.surfapi.velocity.api.command.args

import com.google.common.reflect.TypeToken
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.velocitypowered.api.command.CommandSource
import dev.jorel.commandapi.CommandAPIHandler
import dev.jorel.commandapi.SuggestionInfo
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.CommandAPIArgumentType
import dev.jorel.commandapi.executors.CommandArguments
import kotlinx.coroutines.*
import kotlinx.coroutines.future.future
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import java.util.concurrent.CompletableFuture

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
    private val scope: CoroutineScope = defaultScope
) : Argument<Deferred<T>>(base.nodeName, {
    base.rawType
}) {
    private val primitiveType by lazy { object : TypeToken<Deferred<T>>(javaClass) {} }

    override fun instance() = this

    @Suppress("UNCHECKED_CAST")
    override fun getPrimitiveType(): Class<Deferred<T>> {
        return primitiveType.rawType as Class<Deferred<T>>
    }

    protected abstract inner class SuspendArgumentSuggestions : ArgumentSuggestions<CommandSource> {
        @Throws(CommandSyntaxException::class)
        abstract suspend fun CoroutineScope.suggest(
            info: SuggestionInfo<CommandSource>,
            builder: SuggestionsBuilder
        )

        final override fun suggest(
            info: SuggestionInfo<CommandSource>,
            builder: SuggestionsBuilder
        ): CompletableFuture<Suggestions> {
            return scope.future {
                suggest(info, builder)
                builder.build()
            }
        }
    }

    /**
     * Parses the argument asynchronously.
     *
     * This method is executed inside the provided [CoroutineScope] and may perform
     * suspendable or asynchronous operations such as database lookups or network calls.
     *
     * Implementations should throw [com.mojang.brigadier.exceptions.CommandSyntaxException] / [dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException]
     * to signal a user-facing parsing error.
     *
     * @param info Contextual information about the argument being parsed.
     * @return The parsed argument value.
     */
    abstract suspend fun CoroutineScope.parse(info: CustomArgumentInfo<B>): T

    /**
     * Synchronously invoked by CommandAPI to parse the argument.
     *
     * Internally, this method delegates to [parse] by launching a coroutine in [scope]
     * and returning a [Deferred] representing the eventual parsing result.
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
            val sender = cmdCtx.source as CommandSource
            val info = CustomArgumentInfo(sender, previousArgs, customResult, parsedInput)
            parse(info)
        }
    }

    override fun getArgumentType(): CommandAPIArgumentType = base.argumentType

    protected fun stringCollectionSuspend(suggestions: suspend CoroutineScope.(SuggestionInfo<CommandSource>) -> Collection<String>): ArgumentSuggestions<CommandSource> {
        return ArgumentSuggestions.stringCollectionAsync {
            scope.future { suggestions(it) }
        }
    }

    @JvmRecord
    data class CustomArgumentInfo<B>(
        val sender: CommandSource,
        val previousArgs: CommandArguments,
        val input: String,
        val currentInput: B
    )

    companion object {
        private val logger = ComponentLogger.logger()

        private val defaultScopeExceptionHandler = CoroutineExceptionHandler { _, t ->
            if (t is CancellationException) return@CoroutineExceptionHandler
            logger.atError()
                .setCause(t)
                .log("Uncaught exception in command parsing coroutine")
        }

        private val defaultScope = CoroutineScope(
            Dispatchers.IO.limitedParallelism(16) // We are not planning to do blocking work but preparing in case a developer messes up
                    + SupervisorJob()
                    + CoroutineName("Velocity Default Command Parsing Scope")
                    + defaultScopeExceptionHandler
        )
    }
}