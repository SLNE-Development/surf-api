package dev.slne.surf.api.minestom.server.impl.command.parser

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.slne.surf.api.minestom.command.argument.ResourceLocationArgument
import net.kyori.adventure.key.Key
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * `ResourceLocationParser` is internal to the api module, so every case here reaches it through
 * [dev.slne.surf.api.minestom.command.argument.Argument.toDefinition]'s
 * publicly-typed `rawType`, rather than importing the parser directly.
 */
class ResourceLocationParserTest {
    private val parser = ResourceLocationArgument("key").toDefinition().rawType

    @Test
    fun `a bare path defaults to the minecraft namespace`() {
        assertEquals(Key.key("minecraft", "stone"), parser.parse(StringReader("stone")))
    }

    @Test
    fun `an explicit namespace is kept`() {
        assertEquals(Key.key("surf", "lobby"), parser.parse(StringReader("surf:lobby")))
    }

    @Test
    fun `parsing stops at the first character that cannot belong to a key`() {
        val reader = StringReader("minecraft:stone more")
        assertEquals(Key.key("minecraft", "stone"), parser.parse(reader))
        assertEquals(" more", reader.remaining)
    }

    @Test
    fun `an invalid character set is rejected`() {
        assertThrows(CommandSyntaxException::class.java) { parser.parse(StringReader("Stone")) }
    }

    @Test
    fun `a leading separator defaults to the minecraft namespace`() {
        assertEquals(Key.key("minecraft", "lobby"), parser.parse(StringReader(":lobby")))
    }

    @Test
    fun `a second separator inside the value is rejected`() {
        assertThrows(CommandSyntaxException::class.java) { parser.parse(StringReader("a:b:c")) }
    }

    @Test
    fun `a namespace containing a slash is rejected`() {
        assertThrows(CommandSyntaxException::class.java) { parser.parse(StringReader("a/b:c")) }
    }
}
