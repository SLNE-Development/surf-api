@file:OptIn(NmsUseWithCaution::class)

package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.scope
import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.surfapi.bukkit.api.dialog.base
import dev.slne.surf.surfapi.bukkit.api.dialog.builder.actionButton
import dev.slne.surf.surfapi.bukkit.api.dialog.clearDialogs
import dev.slne.surf.surfapi.bukkit.api.dialog.dialog
import dev.slne.surf.surfapi.bukkit.api.dialog.pagination.paginatedDialog
import dev.slne.surf.surfapi.bukkit.api.dialog.query.DialogQuery
import dev.slne.surf.surfapi.bukkit.api.dialog.query.PageResult
import dev.slne.surf.surfapi.bukkit.api.dialog.query.PageState
import dev.slne.surf.surfapi.bukkit.api.dialog.type
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.test.plugin
import dev.slne.surf.surfapi.core.api.messages.adventure.buildText
import io.papermc.paper.dialog.Dialog
import net.kyori.adventure.text.Component

object PaginatedDialogTest {
    val paginationData = mutableListOf<PaginationEntry>()

    init {
        val amount = 10000

        for (i in 1..amount) {
            paginationData.add(
                PaginationEntry(
                    id = i,
                    name = "Eintrag $i",
                    displayName = Component.text("Eintrag $i")
                )
            )
        }
    }

    object PaginatedEntryQuery : DialogQuery<PageState, PaginationEntry> {
        enum class SearchType {
            EQUALS,
            CONTAINS,
            STARTS_WITH,
            ENDS_WITH
        }

        override suspend fun execute(state: PageState): PageResult<PaginationEntry> {
            val currentPage = state.page
            val pageSize = state.limit
            val search = state.search

            val searchType = when {
                search == null -> null
                search.startsWith("*") && search.endsWith("*") -> SearchType.CONTAINS
                search.startsWith("*") -> SearchType.ENDS_WITH
                search.endsWith("*") -> SearchType.STARTS_WITH
                else -> SearchType.EQUALS
            }

            val normalizedSearch = search?.trim('*')?.lowercase()

            val filteredData = if (normalizedSearch != null) {
                paginationData.filter { entry ->
                    val entryName = entry.name.lowercase()

                    when (searchType) {
                        SearchType.EQUALS -> entryName.equals(
                            normalizedSearch,
                            ignoreCase = true
                        )

                        SearchType.CONTAINS -> entryName.contains(
                            normalizedSearch,
                            ignoreCase = true
                        )

                        SearchType.STARTS_WITH -> entryName.startsWith(
                            normalizedSearch,
                            ignoreCase = true
                        )

                        SearchType.ENDS_WITH -> entryName.endsWith(
                            normalizedSearch,
                            ignoreCase = true
                        )

                        null -> true
                    }
                }
            } else {
                paginationData
            }

            val totalEntries = filteredData.size
            val totalPages = (totalEntries + pageSize - 1) / pageSize
            val fromIndex = (currentPage - 1) * pageSize
            val toIndex = minOf(fromIndex + pageSize, totalEntries)

            val pageEntries = if (fromIndex in 0 until totalEntries) {
                filteredData.subList(fromIndex, toIndex)
            } else {
                emptyList()
            }

            return PageResult(
                items = pageEntries,
                page = currentPage,
                totalPages = totalPages,
            )
        }
    }
}

fun paginationDialogTestCommand() = commandAPICommand("paginationdialog") {
    playerExecutor { player, _ ->
        plugin.launch {
            paginatedDialog(
                player = player,
                query = PaginatedDialogTest.PaginatedEntryQuery,
                searchable = true,
                itemBuilder = { entry ->
                    buildText {
                        text(entry.name)
                    } to entry.dialog
                },
                scope = plugin.scope
            )
        }
    }
}

data class PaginationEntry(
    val id: Int,
    val name: String,
    val displayName: Component,
) {
    val dialog: Dialog
        get() = dialog {
            base {
                title {
                    text(name)
                }

                externalTitle {
                    text(name)
                }
            }

            type {
                multiAction {
                    action(actionButton {
                        label {
                            text("Schließen")
                        }

                        action {
                            playerCallback {
                                it.clearDialogs()
                            }
                        }
                    })
                }
            }
        }
}