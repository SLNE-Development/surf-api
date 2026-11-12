# Pagination and search dialogs

## `paginatedDialog` — Paper only

`platform/paper/api/src/main/kotlin/dev/slne/surf/api/paper/dialog/pagination/PaginatedDialog.kt`.
There is no Minestom counterpart, and no cursor-based variant anywhere.

```kotlin
suspend fun <T> paginatedDialog(
    player: Player,
    query: DialogQuery<PageState, T>,
    titleBuilder: SurfComponentBuilder.(PageState, PageResult<T>) -> Unit = { _, result ->
        if (result.totalPages == 0) text("Keine Ergebnisse")
        else text("Seite ${result.page} von ${result.totalPages}")
    },
    searchable: Boolean = false,
    itemBuilder: (T) -> Pair<Component, Dialog>,
    scope: CoroutineScope,
)
```

It is a `suspend` function and it **shows** the dialog itself — it returns `Unit`, not a `Dialog`.
Internally it is a `composableDialog(player, PageState(), scope) { … }` whose renderer:

1. reads `state()`,
2. `remember(state.page, state.search) { query.execute(state) }` — so a page is fetched once per
   `(page, search)` pair and then served from the store's memo,
3. builds `base { title { titleBuilder(this, state, page) } }`, adding a `text("search")` input
   seeded with `state.search ?: ""` when `searchable`,
4. builds `type { multiAction { columns(1) … } }`: one button per item from
   `itemBuilder(item)` → `(label, dialog)`, whose action is `playerCallback { player.showDialog(dialog) }`.

Navigation buttons, all German and not configurable:

| Button | Shown when | Action |
| --- | --- | --- |
| `Zurück` | `state.page > 1` | `setState { copy(page = page.page - 1) }` inside `scope.launch` |
| `Weiter` | `state.page < page.totalPages` | `setState { copy(page = page.page + 1) }` |
| `Suche zurücksetzen` | `searchable` | `setState { copy(search = null, page = 1) }` |
| `Suchen` | `searchable` | `customPlayerClick { response, _ -> setState { copy(search = response.getText("search") ?: "", page = 1) } }` |

Note that the page/forward handlers read `page.page` — the page number reported by the
`PageResult`, not `state.page`.

A caller needs a `DialogQuery<PageState, T>` doing its own filtering, slicing and `totalPages`
arithmetic; `PageResult.totalItems` is optional and `paginatedDialog` never reads it. A worked
query, including wildcard search handling, lives in
`local/paper-plugin-test/src/main/kotlin/dev/slne/surf/surfapi/bukkit/test/command/dialog/subcommands/PaginatedDialogTest.kt`
(that module still uses the old `dev.slne.surf.surfapi.bukkit` package names — read it for the
dialog usage, not for package conventions). Its call site:

```kotlin
plugin.launch {
    paginatedDialog(
        player = player,
        query = PaginatedDialogTest.PaginatedEntryQuery,
        searchable = true,
        itemBuilder = { entry -> buildText { text(entry.name) } to entry.dialog },
        scope = plugin.scope,
    )
}
```

## `searchDialog` — both platforms, same shape

`paper/dialog/search/SearchDialog.kt` and `minestom/dialog/search/SearchDialog.kt`. Both **return**
a `Dialog`; the caller shows it. Both declare the same parameters:

```kotlin
fun searchDialog(
    title: SurfComponentBuilder.() -> Unit,
    externalTitle: SurfComponentBuilder.() -> Unit = title,
    onSearch: (player: Player, query: String) -> Unit,
    onClose: (player: Player, query: String) -> Unit,
    body: DialogBodyBuilder.() -> Unit = { },
    searchButton: SearchDialogAction.() -> Unit = { /* "Suchen" + tooltip */ },
    cancelButton: SearchDialogAction.() -> Unit = { /* "Abbrechen" + tooltip */ },
    searchInput: SearchInput.() -> Unit,
    canCloseWithEscape: Boolean = false,
    afterAction: /* platform */ = …AfterAction.NONE,
): Dialog
```

`SearchDialogAction(label, tooltip)` holds two `SurfComponentBuilder` blocks. `SearchInput` holds
`key = "search"`, `label = { text("Suche") }`, `initialValue = ""` and an `inputModifier` applied
last.

Differences:

| | Paper | Minestom |
| --- | --- | --- |
| `Player` | `org.bukkit.entity.Player` | `net.minestom.server.entity.Player` |
| `afterAction` type | `DialogBase.DialogAfterAction` | `net.minestom.server.dialog.DialogAfterAction` |
| `SearchInput.inputModifier` | `DialogInputBuilder.TextDialogInput.() -> Unit` | `DialogInputBuilder.TextInputBuilder.() -> Unit` |
| Closes the dialog with | `player.clearDialogs(false)` (`@NmsUseWithCaution`, hence the file-level `@file:OptIn`) | `player.closeDialog()` |
| Structure | `input { }` nested inside `body { }`, `type { }` nested inside `base { }` — resolves through the outer receiver, but misleading | flat: `base { … }` then `type { … }` |

Both wire the buttons the same, surprising way: the **search** button is the confirmation's `no`
and the **cancel** button is its `yes`. Keep that in mind when changing either file — swapping them
changes which button the client renders where.

Both close the dialog before invoking the callback, so `onSearch`/`onClose` are free to open another
dialog or an inventory straight away (the Minestom file documents this explicitly). Both read the
query with `response.getText(key) ?: ""`.

Usage, from the Minestom file's own KDoc:

```kotlin
player.showDialog(
    searchDialog(
        title = { primary("Search a player...") },
        onSearch = { player, query -> showResults(player, query) },
        onClose = { player, _ -> showOverview(player) },
        searchInput = { initialValue = lastQuery },
    )
)
```

Every default label is hard-coded German (`Suchen`, `Abbrechen`, `Suche`,
`Klicke hier, um die Suche zu starten.`, `Klicke hier, um die Suche abzubrechen.`). Nothing goes
through the message-bundle layer, so a localisation pass has to touch these two files and
`PaginatedDialog.kt`.
