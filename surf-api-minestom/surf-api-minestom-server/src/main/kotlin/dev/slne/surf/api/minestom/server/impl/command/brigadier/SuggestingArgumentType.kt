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
 * Wraps [delegate] to serve the suggestion provider declared on [definition].
 *
 * Parsing and examples are delegated unchanged. Suggestions are computed in a coroutine launched on
 * [scope], so providers may suspend and the caller's thread is not blocked.
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
     * Suggests the provider's entries, filtered by the provider's policy. Include modes add the
     * [delegate]'s own suggestions; replace modes offer only the provider's.
     *
     * The returned future always completes normally. A failing provider is logged and the
     * suggestions collected up to that point are returned.
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

            // The delegate may build its suggestions from a different builder (a list offsets one
            // per element), so the results are merged instead of assumed to share [builder].
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
     * The arguments parsed so far in [context], in the order they were read.
     *
     * Nodes without a parsed value, such as literals, are omitted.
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

/**
 * Whether this mode keeps the suggestions of the argument's own type.
 */
private fun SuggestionMode<*>.includesBuiltIns(): Boolean =
    this is SuggestionMode.Include<*> || this is SuggestionMode.IncludeSafe<*>

/**
 * How the entries of a provider are matched against the typed input.
 *
 * Safe providers return typed values that the argument stringifies itself, so they are always
 * filtered by prefix.
 */
private fun SuggestionMode<*>.filterPolicy(): SuggestionFilter = when (this) {
    is SuggestionMode.Include<*> -> provider.filterPolicy
    is SuggestionMode.Replace<*> -> provider.filterPolicy
    else -> SuggestionFilter.PREFIX
}

/**
 * Whether this definition carries a suggestion provider of its own.
 */
internal fun ArgumentDefinition<*>.hasCustomSuggestions(): Boolean =
    suggestions != SuggestionMode.BuiltIns
