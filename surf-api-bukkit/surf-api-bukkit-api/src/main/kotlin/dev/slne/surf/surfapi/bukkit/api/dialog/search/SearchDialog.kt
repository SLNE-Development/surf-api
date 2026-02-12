@file:Suppress("UnstableApiUsage")
@file:OptIn(NmsUseWithCaution::class)

package dev.slne.surf.surfapi.bukkit.api.dialog.search

import dev.slne.surf.surfapi.bukkit.api.dialog.base
import dev.slne.surf.surfapi.bukkit.api.dialog.builder.DialogBodyBuilder
import dev.slne.surf.surfapi.bukkit.api.dialog.builder.DialogInputBuilder
import dev.slne.surf.surfapi.bukkit.api.dialog.clearDialogs
import dev.slne.surf.surfapi.bukkit.api.dialog.dialog
import dev.slne.surf.surfapi.bukkit.api.dialog.type
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.DialogBase
import org.bukkit.entity.Player

data class SearchDialogAction(
    var label: SurfComponentBuilder.() -> Unit,
    var tooltip: SurfComponentBuilder.() -> Unit
)

data class SearchInput(
    var key: String = "search",
    var label: SurfComponentBuilder.() -> Unit = {
        text("Suche")
    },
    var initialValue: String = "",
    var inputModifier: DialogInputBuilder.TextDialogInput.() -> Unit = {}
)

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
    afterAction: DialogBase.DialogAfterAction = DialogBase.DialogAfterAction.NONE
): Dialog = dialog {
    val searchInput = SearchInput().apply(searchInput)

    val searchButton = SearchDialogAction(
        label = { },
        tooltip = { }
    ).apply(searchButton)

    val cancelButton = SearchDialogAction(
        label = { },
        tooltip = { }
    ).apply(cancelButton)

    base {
        title(title)
        externalTitle(externalTitle)
        this.canCloseWithEscape = canCloseWithEscape
        this.afterAction = afterAction

        body {
            body()

            input {
                text(searchInput.key) {
                    label(searchInput.label)
                    initial(searchInput.initialValue)

                    searchInput.inputModifier(this)
                }
            }
        }

        type {
            confirmation {
                no {
                    label(searchButton.label)
                    tooltip(searchButton.tooltip)

                    action {
                        customPlayerClick { response, player ->
                            player.clearDialogs(false)

                            val query = response.getText(searchInput.key) ?: ""
                            onSearch(player, query)
                        }
                    }
                }

                yes {
                    label(cancelButton.label)
                    tooltip(cancelButton.tooltip)

                    action {
                        customPlayerClick { response, player ->
                            player.clearDialogs(false)

                            val query = response.getText(searchInput.key) ?: ""
                            onClose(player, query)
                        }
                    }
                }
            }
        }
    }
}