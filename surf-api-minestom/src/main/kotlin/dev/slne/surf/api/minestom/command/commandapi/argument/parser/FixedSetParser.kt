package dev.slne.surf.api.minestom.command.commandapi.argument.parser

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import java.util.concurrent.CompletableFuture

/**
 * Reads one word and resolves it against a fixed set of accepted spellings.
 *
 * Matching is case-sensitive, since each spelling is the canonical form its owning argument
 * stringifies back to; accepting other casings would let two spellings map to the same value.
 * Suggestions are offered case-insensitively, which costs nothing since they are only a typing
 * convenience.
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
