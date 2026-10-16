package dev.slne.surf.api.minestom.dialog.search

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.minestom.server.dialog.Dialog
import net.minestom.server.dialog.DialogAfterAction
import net.minestom.server.dialog.DialogBody
import net.minestom.server.dialog.DialogInput
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

@EnvTest
class SearchDialogTest {

    @Test
    fun `a search dialog asks for a single query`() {
        val dialog = searchDialog(
            title = { text("Search") },
            onSearch = { _, _ -> },
            onClose = { _, _ -> },
            searchInput = { initialValue = "red" },
        )

        val confirmation = assertIs<Dialog.Confirmation>(dialog)
        val input = assertIs<DialogInput.Text>(confirmation.metadata().inputs().single())

        assertEquals("search", input.key())
        assertEquals("red", input.initial())
    }

    @Test
    fun `a search dialog keeps its title and body`() {
        val dialog = searchDialog(
            title = { text("Search") },
            onSearch = { _, _ -> },
            onClose = { _, _ -> },
            body = { plainMessage(Component.text("Who are you looking for?")) },
            searchInput = { },
        )

        assertEquals("Search", plain(dialog.metadata().title()))
        assertEquals(
            listOf(DialogBody.PlainMessage(Component.text("Who are you looking for?"), 200)),
            dialog.metadata().body()
        )
    }

    private fun plain(component: Component) =
        PlainTextComponentSerializer.plainText().serialize(component)

    @Test
    fun `a search dialog stays open until a button is pressed`() {
        val dialog = searchDialog(
            title = { text("Search") },
            onSearch = { _, _ -> },
            onClose = { _, _ -> },
            searchInput = { },
        )

        assertFalse(dialog.metadata().canCloseWithEscape())
        assertEquals(DialogAfterAction.NONE, dialog.metadata().afterAction())
    }

    @Test
    fun `the query is collected under the key it was asked for`() {
        val dialog = searchDialog(
            title = { text("Search") },
            onSearch = { _, _ -> },
            onClose = { _, _ -> },
            searchInput = {
                key = "player"
                inputModifier = { maxLength(16) }
            },
        )

        val input = assertIs<DialogInput.Text>(dialog.metadata().inputs().single())

        assertEquals("player", input.key())
        assertEquals(16, input.maxLength())
    }
}
