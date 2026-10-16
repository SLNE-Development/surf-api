package dev.slne.surf.api.minestom.command.argument.parser

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.kyori.adventure.key.Key
import java.util.concurrent.CompletableFuture

/**
 * Parses a namespaced key into the value [lookup] returns for it.
 *
 * A key for which [lookup] returns `null` fails at the position where the key starts, with a
 * message naming [registryName]. [keys] supplies the suggestions and is evaluated on every
 * request, so entries added to a registry later are included.
 */
internal class RegistryParser<T>(
    private val registryName: String,
    private val keys: () -> Iterable<String> = ::emptyList,
    private val lookup: (Key) -> T?,
) : ArgumentType<T> {
    override fun parse(reader: StringReader): T {
        val start = reader.cursor
        val key = ResourceLocationParser.readKey(reader)

        return lookup(key) ?: run {
            reader.cursor = start
            throw UNKNOWN.createWithContext(reader, "$registryName '$key'")
        }
    }

    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> = builder.suggestMatching(keys())

    private companion object {
        val UNKNOWN = DynamicCommandExceptionType { value ->
            LiteralMessage("Unknown $value")
        }
    }
}
