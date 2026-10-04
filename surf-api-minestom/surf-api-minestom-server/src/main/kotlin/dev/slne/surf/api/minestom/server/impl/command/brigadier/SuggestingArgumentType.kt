package dev.slne.surf.api.minestom.server.impl.command.brigadier

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import dev.slne.surf.api.minestom.command.argument.ArgumentDefinition
import dev.slne.surf.api.minestom.command.argument.SuggestionMode
import dev.slne.surf.api.minestom.command.exception.ComponentMessage
import dev.slne.surf.api.minestom.command.executor.CommandArguments
import dev.slne.surf.api.minestom.command.executor.ParsedArgument
import dev.slne.surf.api.minestom.command.suggestion.SafeSuggestions
import dev.slne.surf.api.minestom.command.suggestion.StringTooltip
import dev.slne.surf.api.minestom.command.suggestion.SuggestionFilter
import dev.slne.surf.api.minestom.command.suggestion.SuggestionInfo
import dev.slne.surf.api.minestom.coroutine.minestomAsyncScope
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import net.minestom.server.MinecraftServer
import net.minestom.server.command.CommandSender
import java.util.concurrent.CompletableFuture

/**
 * Adds an argument's declared suggestion provider to the values its raw type parses.
 *
 * Parsing is delegated untouched; only [listSuggestions] differs. Providers may suspend, so the
 * returned future is completed from a coroutine rather than on the netty thread.
 */
internal class SuggestingArgumentType(
    val delegate: ArgumentType<Any>,
    private val definition: ArgumentDefinition<*>,
    private val scope: () -> CoroutineScope = { minestomAsyncScope },
) : ArgumentType<Any> {

    override fun parse(reader: StringReader): Any = delegate.parse(reader)

    override fun <S> parse(reader: StringReader, source: S): Any = delegate.parse(reader, source)

    override fun getExamples(): Collection<String> = delegate.examples

    /**
     * An `include` mode keeps the raw type's own suggestions and adds the provider's on top; a
     * `replace` mode offers only the provider's.
     */
    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        val sender = context.source as? CommandSender ?: return Suggestions.empty()
        val provider = definition.suggestions
        val future = CompletableFuture<Suggestions>()

        scope().launch {
            var builtIns: Suggestions? = null

            try {
                if (provider.includesBuiltIns()) {
                    builtIns = delegate.listSuggestions(context, builder).await()
                }

                val entries = resolve(provider, sender, context, builder)
                val filtered = if (provider.filterPolicy() == SuggestionFilter.PREFIX) {
                    entries.filter { entry ->
                        entry.suggestion.startsWith(builder.remaining, ignoreCase = true)
                    }
                } else {
                    entries
                }

                filtered.forEach { entry ->
                    builder.suggest(entry.suggestion, entry.tooltip?.let(::ComponentMessage))
                }
            } catch (failure: Throwable) {
                MinecraftServer.LOGGER.error(
                    "Failed suggestions for argument '{}'",
                    definition.nodeName,
                    failure,
                )
            }

            // The raw type is free to answer from a builder of its own - a list type offsets one
            // per element - so the two results are merged rather than assumed to share a builder.
            val own = builder.build()
            future.complete(
                if (builtIns == null) own else Suggestions.merge(builder.input, listOf(builtIns, own)),
            )
        }

        return future
    }

    private suspend fun <S> resolve(
        mode: SuggestionMode<*>,
        sender: CommandSender,
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): List<StringTooltip> {
        val info = SuggestionInfo(
            sender = sender,
            previousArgs = previousArguments(context),
            currentInput = builder.input,
            currentArg = builder.remaining,
        )

        @Suppress("UNCHECKED_CAST")
        val stringify = definition.stringify as (Any?) -> String

        @Suppress("UNCHECKED_CAST")
        return when (mode) {
            SuggestionMode.BuiltIns -> emptyList()
            is SuggestionMode.Include<*> -> mode.provider.suggest(info)
            is SuggestionMode.Replace<*> -> mode.provider.suggest(info)
            is SuggestionMode.IncludeSafe<*> ->
                (mode.provider as SafeSuggestions<Any?>).suggest(info)
                    .map { tooltip -> StringTooltip(stringify(tooltip.suggestion), tooltip.tooltip) }

            is SuggestionMode.ReplaceSafe<*> ->
                (mode.provider as SafeSuggestions<Any?>).suggest(info)
                    .map { tooltip -> StringTooltip(stringify(tooltip.suggestion), tooltip.tooltip) }
        }
    }

    /**
     * The arguments already parsed on this path, in the order they were read.
     *
     * Only values Brigadier actually produced are present; a provider sees what the sender has
     * typed so far and nothing more.
     */
    private fun <S> previousArguments(context: CommandContext<S>): CommandArguments {
        val parsed = ObjectArrayList<ParsedArgument>()

        context.nodes.forEach { node ->
            val name = node.node.name
            val value = runCatching { context.getArgument(name, Any::class.java) }.getOrNull()
                ?: return@forEach

            parsed += ParsedArgument(
                name = name,
                value = value,
                raw = context.input.substring(node.range.start, node.range.end),
                present = true,
            )
        }

        return CommandArguments.of(parsed)
    }
}

/** Whether [SuggestionMode] keeps the suggestions the argument's own parser offers. */
private fun SuggestionMode<*>.includesBuiltIns(): Boolean =
    this is SuggestionMode.Include<*> || this is SuggestionMode.IncludeSafe<*>

/**
 * How the entries a provider returned are matched against what has been typed.
 *
 * A safe provider returns typed values this argument stringifies itself, so it has no say in the
 * policy and is always filtered by prefix.
 */
private fun SuggestionMode<*>.filterPolicy(): SuggestionFilter = when (this) {
    is SuggestionMode.Include<*> -> provider.filterPolicy
    is SuggestionMode.Replace<*> -> provider.filterPolicy
    else -> SuggestionFilter.PREFIX
}

/** Whether definition carries a suggestion provider of its own. */
internal fun ArgumentDefinition<*>.hasCustomSuggestions(): Boolean =
    suggestions != SuggestionMode.BuiltIns
