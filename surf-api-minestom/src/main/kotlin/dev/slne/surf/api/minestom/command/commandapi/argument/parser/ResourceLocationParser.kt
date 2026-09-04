package dev.slne.surf.api.minestom.command.commandapi.argument.parser

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.kyori.adventure.key.InvalidKeyException
import net.kyori.adventure.key.Key

/**
 * Reads a namespaced key such as `minecraft:stone`, defaulting the namespace to `minecraft`.
 */
internal object ResourceLocationParser : ArgumentType<Key> {
    private val INVALID = SimpleCommandExceptionType(LiteralMessage("Invalid identifier"))

    override fun parse(reader: StringReader): Key = readKey(reader)

    /**
     * Reads a namespaced key from [reader], consuming only characters allowed in a key and
     * leaving the rest of the input untouched.
     *
     * A key with no separator, or with the separator as its first character, defaults its
     * namespace to `minecraft`. The path must not be empty.
     *
     * @throws CommandSyntaxException if the consumed text is not a valid key
     */
    fun readKey(reader: StringReader): Key {
        val start = reader.cursor
        while (reader.canRead() && isAllowed(reader.peek())) {
            reader.skip()
        }

        val text = reader.string.substring(start, reader.cursor)
        val separator = text.indexOf(':')
        val namespace = if (separator >= 1) text.substring(0, separator) else Key.MINECRAFT_NAMESPACE
        val value = if (separator >= 0) text.substring(separator + 1) else text
        if (value.isEmpty()) invalid(reader, start)

        return try {
            Key.key(namespace, value)
        } catch (cause: InvalidKeyException) {
            invalid(reader, start)
        }
    }

    private fun invalid(reader: StringReader, start: Int): Nothing {
        reader.cursor = start
        throw INVALID.createWithContext(reader)
    }

    private fun isAllowed(character: Char): Boolean = character in '0'..'9' ||
            character in 'a'..'z' ||
            character == '_' || character == ':' || character == '/' ||
            character == '.' || character == '-'
}
