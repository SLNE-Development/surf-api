package dev.slne.surf.api.minestom.command.commandapi.argument.parser

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
 * Reads a namespaced key and resolves it through [lookup].
 *
 * [registryName] only appears in the failure message. A key that resolves to `null` is reported at
 * the position the key started at, so the client underlines the key rather than the rest of the line.
 *
 * [keys] is queried on every completion rather than captured once, because a registry may gain
 * entries after the argument was built.
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
