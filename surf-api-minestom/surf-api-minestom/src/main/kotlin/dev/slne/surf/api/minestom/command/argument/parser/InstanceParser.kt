package dev.slne.surf.api.minestom.command.argument.parser

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minestom.server.MinecraftServer
import net.minestom.server.instance.Instance
import java.util.concurrent.CompletableFuture

/**
 * Parses an instance by UUID or dimension name among the currently registered instances.
 *
 * A dimension name resolves only while exactly one instance uses it. An unknown or ambiguous token
 * fails at the position where the token starts.
 */
internal object InstanceParser : ArgumentType<Instance> {
    private val UNKNOWN = DynamicCommandExceptionType { value ->
        LiteralMessage("Unknown or ambiguous instance '$value'; use its UUID")
    }

    override fun parse(reader: StringReader): Instance {
        val start = reader.cursor
        while (reader.canRead() && isAllowed(reader.peek())) {
            reader.skip()
        }
        val input = reader.string.substring(start, reader.cursor)

        return resolve(input) ?: run {
            reader.cursor = start
            throw UNKNOWN.createWithContext(reader, input)
        }
    }

    /**
     * Suggests the UUID of every instance and each dimension name used by exactly one instance.
     */
    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        val instances = MinecraftServer.getInstanceManager().instances
        val dimensionCounts = Object2IntOpenHashMap<String>()
        instances.forEach { instance -> dimensionCounts.addTo(instance.dimensionName, 1) }

        val values = ObjectArrayList<String>(instances.size * 2)
        instances.forEach { instance -> values += instance.uuid.toString() }
        instances.forEach { instance ->
            if (dimensionCounts.getInt(instance.dimensionName) == 1) {
                values += instance.dimensionName
            }
        }

        return builder.suggestMatching(values)
    }

    private fun resolve(input: String): Instance? {
        val instances = MinecraftServer.getInstanceManager().instances
        parseDashedUuid(input)?.let { uuid ->
            instances.singleOrNull { instance -> instance.uuid == uuid }?.let { return it }
        }
        return instances.singleOrNull { instance -> instance.dimensionName == input }
    }

    private fun isAllowed(character: Char): Boolean = character in '0'..'9' ||
            character in 'a'..'z' || character in 'A'..'Z' ||
            character == '_' || character == ':' || character == '/' ||
            character == '.' || character == '-'
}
