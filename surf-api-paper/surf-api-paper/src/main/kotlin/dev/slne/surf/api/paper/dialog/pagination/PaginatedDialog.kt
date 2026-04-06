@file:Suppress("UnstableApiUsage")

package dev.slne.surf.api.paper.dialog.pagination

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.paper.dialog.base
import dev.slne.surf.api.paper.dialog.builder.actionButton
import dev.slne.surf.api.paper.dialog.composition.composableDialog
import dev.slne.surf.api.paper.dialog.dialog
import dev.slne.surf.api.paper.dialog.query.DialogQuery
import dev.slne.surf.api.paper.dialog.query.PageResult
import dev.slne.surf.api.paper.dialog.query.PageState
import dev.slne.surf.api.paper.dialog.type
import io.papermc.paper.dialog.Dialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

suspend fun <T> paginatedDialog(
    player: Player,
    query: DialogQuery<PageState, T>,
    titleBuilder: SurfComponentBuilder.(PageState, PageResult<T>) -> Unit = { _, result ->
        if (result.totalPages == 0) {
            text("Keine Ergebnisse")
        } else {
            text("Seite ${result.page} von ${result.totalPages}")
        }
    },
    searchable: Boolean = false,
    itemBuilder: (T) -> Pair<Component, Dialog>,
    scope: CoroutineScope
) = composableDialog(
    player = player,
    initialState = PageState(),
    scope = scope
) {
    val state = state()
    val page = remember(state.page, state.search) {
        query.execute(state)
    }

    dialog {
        base {
            title {
                titleBuilder(this, state, page)
            }

            if (searchable) {
                input {
                    text("search") {
                        label {
                            text("Suche")
                        }

                        initial(state.search ?: "")
                    }
                }
            }
        }

        type {
            multiAction {
                columns(1)

                // Item Buttons
                page.items.forEach { item ->
                    val (dialogTitle, dialog) = itemBuilder(item)

                    action(actionButton {
                        label(dialogTitle)

                        action {
                            playerCallback {
                                player.showDialog(dialog)
                            }
                        }
                    })
                }

                // Pagination Buttons
                if (state.page > 1) {
                    action(actionButton {
                        label {
                            text("Zurück")
                        }

                        action {
                            playerCallback {
                                scope.launch {
                                    setState { copy(page = page.page - 1) }
                                }
                            }
                        }
                    })
                }

                if (state.page < page.totalPages) {
                    action(actionButton {
                        label {
                            text("Weiter")
                        }

                        action {
                            playerCallback {
                                scope.launch {
                                    setState { copy(page = page.page + 1) }
                                }
                            }
                        }
                    })
                }

                if (searchable) {
                    action(actionButton {
                        label {
                            text("Suche zurücksetzen")
                        }

                        action {
                            playerCallback {
                                scope.launch {
                                    setState { copy(search = null, page = 1) }
                                }
                            }
                        }
                    })

                    action(actionButton {
                        label {
                            text("Suchen")
                        }

                        action {
                            customPlayerClick { response, _ ->
                                val search = response.getText("search") ?: ""

                                scope.launch {
                                    setState { copy(search = search, page = 1) }
                                }
                            }
                        }
                    })
                }
            }
        }
    }
}