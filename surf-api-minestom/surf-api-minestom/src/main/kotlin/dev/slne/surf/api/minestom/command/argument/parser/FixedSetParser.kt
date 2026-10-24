package dev.slne.surf.api.minestom.command.argument.parser

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import java.util.concurrent.CompletableFuture

/**
 * Parses one word into the value mapped to it in [values].
 *
 * Parsing is case-sensitive: each key is the canonical spelling its argument stringifies to.
 * Suggestions match the typed prefix case-insensitively. An unknown word fails with
 * `Unknown value '<word>'`.
 */
internal class FixedSetParser<T>(private val values: Map<String, T>) : ArgumentType<T> {
    override fun parse(reader: StringReader): T {
        val start = reader.cursor
        val word = reader.readUnquotedString()

        return values[word] ?: run {
            reader.cursor = start
            throw UNKNOWN.createWithContext(reader, word)
        }
    }

    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> = builder.suggestMatching(values.keys)

    override fun getExamples(): Collection<String> = values.keys.take(2)

    private companion object {
        val UNKNOWN = DynamicCommandExceptionType { value ->
            LiteralMessage("Unknown value '$value'")
        }
    }
}
