package dev.slne.surf.api.minestom.command.argument.parser

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.kyori.adventure.key.InvalidKeyException
import net.kyori.adventure.key.Key

/**
 * Parses a namespaced key such as `minecraft:stone`. The namespace defaults to `minecraft`.
 */
internal object ResourceLocationParser : ArgumentType<Key> {
    private val INVALID = SimpleCommandExceptionType(LiteralMessage("Invalid identifier"))

    override fun parse(reader: StringReader): Key = readKey(reader)

    /**
     * Reads a namespaced key from [reader], consuming only the characters a key may contain.
     *
     * The namespace defaults to `minecraft` when the separator is missing or leading. The path must
     * not be empty.
     *
     * @throws CommandSyntaxException when the consumed text is not a valid key; the cursor is reset
     * to the start of the key
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
