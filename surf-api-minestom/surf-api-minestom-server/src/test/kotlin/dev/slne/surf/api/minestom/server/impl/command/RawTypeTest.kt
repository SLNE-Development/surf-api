package dev.slne.surf.api.minestom.server.impl.command

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.slne.surf.api.minestom.command.argument.GreedyStringArgument
import dev.slne.surf.api.minestom.command.argument.IntegerArgument
import dev.slne.surf.api.minestom.command.argument.StringArgument
import dev.slne.surf.api.minestom.command.argument.TextArgument
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class RawTypeTest {
    @Test
    fun `integer delegates to brigadier and keeps its bounds`() {
        val raw = IntegerArgument("value", 1, 8).toDefinition().rawType
        val typed = assertInstanceOf(IntegerArgumentType::class.java, raw)

        assertEquals(1, typed.minimum)
        assertEquals(8, typed.maximum)
        assertEquals(4, typed.parse(StringReader("4")))
        assertThrows(CommandSyntaxException::class.java) { typed.parse(StringReader("9")) }
    }

    @Test
    fun `the three string shapes delegate to brigadier`() {
        assertEquals(
            StringArgumentType.StringType.SINGLE_WORD,
            (StringArgument("a").toDefinition().rawType as StringArgumentType).type,
        )
        assertEquals(
            StringArgumentType.StringType.QUOTABLE_PHRASE,
            (TextArgument("b").toDefinition().rawType as StringArgumentType).type,
        )
        assertEquals(
            StringArgumentType.StringType.GREEDY_PHRASE,
            (GreedyStringArgument("c").toDefinition().rawType as StringArgumentType).type,
        )
    }
}
