package dev.slne.surf.api.minestom.dialog.search

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.minestom.dialog.base
import dev.slne.surf.api.minestom.dialog.builder.DialogBodyBuilder
import dev.slne.surf.api.minestom.dialog.builder.DialogInputBuilder
import dev.slne.surf.api.minestom.dialog.dialog
import dev.slne.surf.api.minestom.dialog.type
import net.minestom.server.dialog.Dialog
import net.minestom.server.dialog.DialogAfterAction
import net.minestom.server.entity.Player

/**
 * The label and tooltip of one of a search dialog's buttons.
 */
data class SearchDialogAction(
    var label: SurfComponentBuilder.() -> Unit,
    var tooltip: SurfComponentBuilder.() -> Unit
)

/**
 * The text field a search dialog collects its query in.
 *
 * @property key the key the query is reported back under
 * @property label the label shown next to the field
 * @property initialValue the query the field starts out with
 * @property inputModifier applied to the field last, for anything the other properties do not cover
 */
data class SearchInput(
    var key: String = "search",
    var label: SurfComponentBuilder.() -> Unit = {
        text("Suche")
    },
    var initialValue: String = "",
    var inputModifier: DialogInputBuilder.TextInputBuilder.() -> Unit = {}
)

/**
 * Builds a dialog that asks for a single query and hands it to [onSearch] or [onClose].
 *
 * The dialog closes itself before either callback runs, so a callback is free to open an inventory
 * or another dialog straight away.
 *
 * ```
 * player.showDialog(
 *     searchDialog(
 *         title = { primary("Search a player...") },
 *         onSearch = { player, query -> showResults(player, query) },
 *         onClose = { player, _ -> showOverview(player) },
 *         searchInput = { initialValue = lastQuery },
 *     )
 * )
 * ```
 *
 * @param title the title shown above the dialog
 * @param externalTitle the title shown wherever the dialog is listed, defaulting to [title]
 * @param onSearch called with the query once the search button is pressed
 * @param onClose called with the query once the cancel button is pressed
 * @param body anything shown above the search field
 * @param searchButton the button that runs the search
 * @param cancelButton the button that abandons the search
 * @param searchInput the text field the query is collected in
 * @param canCloseWithEscape whether the dialog can be dismissed without pressing a button
 * @param afterAction what the client does with the dialog once a button was pressed
 */
fun searchDialog(
    title: SurfComponentBuilder.() -> Unit,
    externalTitle: SurfComponentBuilder.() -> Unit = title,
    onSearch: (player: Player, query: String) -> Unit,
    onClose: (player: Player, query: String) -> Unit,
    body: DialogBodyBuilder.() -> Unit = { },
    searchButton: SearchDialogAction.() -> Unit = {
        label = {
            text("Suchen")
        }
        tooltip = {
            text("Klicke hier, um die Suche zu starten.")
        }
    },
    cancelButton: SearchDialogAction.() -> Unit = {
        label = {
            text("Abbrechen")
        }
        tooltip = {
            text("Klicke hier, um die Suche abzubrechen.")
        }
    },
    searchInput: SearchInput.() -> Unit,
    canCloseWithEscape: Boolean = false,
    afterAction: DialogAfterAction = DialogAfterAction.NONE
): Dialog = dialog {
    val input = SearchInput().apply(searchInput)

    val search = SearchDialogAction(
        label = { },
        tooltip = { }
    ).apply(searchButton)

    val cancel = SearchDialogAction(
        label = { },
        tooltip = { }
    ).apply(cancelButton)

    base {
        title(title)
        externalTitle(externalTitle)
        canCloseWithEscape(canCloseWithEscape)
        afterAction(afterAction)

        body(body)

        input {
            text(input.key) {
                label(input.label)
                initial(input.initialValue)

                input.inputModifier(this)
            }
        }
    }

    type {
        confirmation {
            no {
                label(search.label)
                tooltip(search.tooltip)

                action {
                    customPlayerClick { response, player ->
                        player.closeDialog()

                        onSearch(player, response.getText(input.key) ?: "")
                    }
                }
            }

            yes {
                label(cancel.label)
                tooltip(cancel.tooltip)

                action {
                    customPlayerClick { response, player ->
                        player.closeDialog()

                        onClose(player, response.getText(input.key) ?: "")
                    }
                }
            }
        }
    }
}
