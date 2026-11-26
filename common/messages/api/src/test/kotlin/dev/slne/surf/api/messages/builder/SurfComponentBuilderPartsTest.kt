package dev.slne.surf.api.messages.builder

import dev.slne.surf.api.messages.Colors
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import kotlin.test.Test
import kotlin.test.assertEquals

class SurfComponentBuilderPartsTest {

    @Test
    fun `key-value pair separates key and value with a colon`() {
        val built = SurfComponentBuilder { keyValue("Coins", "42") }

        assertEquals("Coins: 42", plain(built))
    }

    @Test
    fun `key-value pair colors key, separator and value by role`() {
        val built = SurfComponentBuilder { keyValue("Coins", "42") }

        assertEquals(
            listOf(
                "Coins" to Colors.VARIABLE_KEY,
                ": " to Colors.SPACER,
                "42" to Colors.VARIABLE_VALUE,
            ),
            coloredParts(built),
        )
    }

    @Test
    fun `key-value pair accepts the value types of the role methods`() {
        val built = SurfComponentBuilder {
            keyValue("Aktiv", true)
            keyValue("Rang", 'A')
            keyValue("Coins", 42)
        }

        assertEquals("Aktiv: trueRang: ACoins: 42", plain(built))
    }

    @Test
    fun `em dash is surrounded by spaces and in spacer color`() {
        val built = SurfComponentBuilder {
            text("a")
            appendEmDash()
            text("b")
        }

        assertEquals("a — b", plain(built))
        assertEquals(Colors.SPACER, findText(built, " — ")?.color())
    }

    @Test
    fun `en dash has no spaces and is in spacer color`() {
        val built = SurfComponentBuilder {
            text(1)
            appendEnDash()
            text(5)
        }

        assertEquals("1–5", plain(built))
        assertEquals(Colors.SPACER, findText(built, "–")?.color())
    }

    @Test
    fun `note accepts the value types of the role methods in note color`() {
        val built = SurfComponentBuilder {
            note("Tipp")
            note(true)
            note('!')
            note(3)
        }

        assertEquals(
            listOf(
                "Tipp" to Colors.NOTE,
                "true" to Colors.NOTE,
                "!" to Colors.NOTE,
                "3" to Colors.NOTE,
            ),
            coloredParts(built),
        )
    }

    @Test
    fun `note applies the given decorations`() {
        val built = SurfComponentBuilder { note("Tipp", TextDecoration.ITALIC) }

        assertEquals(
            TextDecoration.State.TRUE,
            findText(built, "Tipp")?.decoration(TextDecoration.ITALIC),
        )
    }

    private fun coloredParts(component: Component): List<Pair<String, TextColor?>> {
        return component.children()
            .filterIsInstance<TextComponent>()
            .map { child -> child.content() to child.color() }
    }

    private fun findText(component: Component, content: String): Component? {
        if (component is TextComponent && component.content() == content) return component

        return component.children().firstNotNullOfOrNull { child -> findText(child, content) }
    }

    private fun plain(component: Component): String {
        return PlainTextComponentSerializer.plainText().serialize(component)
    }
}
