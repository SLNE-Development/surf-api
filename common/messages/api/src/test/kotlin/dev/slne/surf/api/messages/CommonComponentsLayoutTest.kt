package dev.slne.surf.api.messages

import dev.slne.surf.api.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommonComponentsLayoutTest {

    private val header = "CASTCRAFTER\nCOMMUNITY SERVER\n\n"
    private val retryLaterFooter = "\n\nBitte versuche es später erneut."
    private val linePrefix = Component.text(">")

    @Test
    fun `kick message separates headline, message and footer by three newlines`() {
        val message = CommonComponents.renderKickDisconnectMessage(
            message = Component.text("Grund"),
            issue = false,
        )

        assertEquals(
            "${header}DU WURDEST VOM SERVER GEWORFEN\n\n\nGrund\n\n\n$retryLaterFooter",
            plain(message),
        )
    }

    @Test
    fun `kick message with the issue footer ends with the support hint`() {
        val message = CommonComponents.renderKickDisconnectMessage(
            message = Component.text("Grund"),
            issue = true,
        )

        val text = plain(message)

        assertTrue(text.startsWith("${header}DU WURDEST VOM SERVER GEWORFEN\n\n\nGrund"))
        assertTrue("kontaktiere den Support in unserem Discord." in text)
    }

    @Test
    fun `disconnect message puts the uppercase reason above the help text`() {
        val message = CommonComponents.renderDisconnectMessage(
            disconnectReason = "WARTUNG",
            help = Component.text("Hilfe"),
            issue = false,
        )

        assertEquals("${header}WARTUNG\n\n\nHilfe\n\n\n$retryLaterFooter", plain(message))
    }

    @Test
    fun `block overloads render the same text as the component overloads`() {
        val fromBlocks = CommonComponents.renderDisconnectMessage(
            disconnectReason = "WARTUNG",
            suggestHelp = { text("Hilfe") },
            footerRenderer = { text("Ende") },
        )
        val fromComponents = CommonComponents.renderDisconnectMessage(
            disconnectReason = "WARTUNG",
            help = Component.text("Hilfe"),
            footer = Component.text("Ende"),
        )

        assertEquals(plain(fromComponents), plain(fromBlocks))
    }

    @Test
    fun `appending a kick message writes the same text as rendering it`() {
        val appended = SurfComponentBuilder {
            appendKickDisconnectMessage(Component.text("Grund"), Component.text("Ende"))
        }
        val rendered = CommonComponents.renderKickDisconnectMessage(
            message = Component.text("Grund"),
            footer = Component.text("Ende"),
        )

        assertEquals(plain(rendered), plain(appended))
    }

    @Test
    fun `collection is joined with commas`() {
        val formatted = CommonComponents.formatCollection(listOf("a", "b", "c")) {
            Component.text(it)
        }

        assertEquals("a, b, c", plain(formatted))
    }

    @Test
    fun `collection on new lines starts with a line separator`() {
        val formatted = CommonComponents.formatCollectionNewLine(
            components = listOf(Component.text("a"), Component.text("b")),
            linePrefix = linePrefix,
        )

        assertEquals("\n>— a\n>— b", plain(formatted))
    }

    @Test
    fun `empty collection on new lines is empty`() {
        val formatted = CommonComponents.formatCollectionNewLine(emptyList(), linePrefix)

        assertEquals("", plain(formatted))
    }

    @Test
    fun `map puts each entry on its own line`() {
        val formatted = CommonComponents.formatMap(
            entries = listOf(
                Component.text("Name") to Component.text("Alice"),
                Component.text("Alter") to Component.text("25"),
            ),
            linePrefix = linePrefix,
        )

        assertEquals("\n>— Name -> Alice\n>— Alter -> 25", plain(formatted))
    }

    @Test
    fun `map colors keys and values that have no color`() {
        val formatted = CommonComponents.formatMap(
            entries = listOf(Component.text("Name") to Component.text("Alice")),
            linePrefix = linePrefix,
        )

        assertEquals(Colors.VARIABLE_KEY, findText(formatted, "Name")?.color())
        assertEquals(Colors.VARIABLE_VALUE, findText(formatted, "Alice")?.color())
    }

    @Test
    fun `empty map is empty`() {
        assertEquals("", plain(CommonComponents.formatMap(emptyList(), linePrefix)))
    }

    @Test
    fun `builder appends collections and maps like the formatters`() {
        val built = SurfComponentBuilder {
            appendCollection(listOf("a", "b")) { Component.text(it) }
            appendCollectionNewLine(listOf("c"), linePrefix) { Component.text(it) }
            appendMap(
                map = mapOf("k" to "v"),
                keyFormatter = { Component.text(it) },
                valueFormatter = { Component.text(it) },
                linePrefix = linePrefix,
            )
        }

        assertEquals("a, b\n>— c\n>— k -> v", plain(built))
    }

    private fun findText(component: Component, content: String): Component? {
        if (component is TextComponent && component.content() == content) return component

        return component.children().firstNotNullOfOrNull { child -> findText(child, content) }
    }

    private fun plain(component: Component): String {
        return PlainTextComponentSerializer.plainText().serialize(component)
    }
}
