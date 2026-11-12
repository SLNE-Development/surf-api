# Pagination

`messages/pagination/` for the contracts, `common/core/impl/.../impl/messages/pagination/` for the
implementations. Every `Component` here is Adventure's text component.

## Which variant

`Pagination<T>` renders inline on the calling thread. `SuspendPagination<T>` has the identical shape
with `suspend` on everything, and is worth it only when a row renderer must suspend. Pick the plain
one unless a row needs I/O; the suspend one buys parallel row rendering and costs you a thread
switch and a context you have to think about.

| | `Pagination<T>` | `SuspendPagination<T>` |
| --- | --- | --- |
| render | `render(content, page = 1): List<Component>` | `suspend` same |
| joined | `renderComponent(content, page = 1): Component` | `suspend` same |
| row renderer | `PaginationRowRenderer<T>` / `.Simple<T>` | `SuspendPaginationRowRenderer<T>` / `.Simple<T>`, methods are `suspend fun CoroutineScope.…` |
| frame renderer | `PaginationRenderer` | `SuspendPaginationRenderer`, delegates to `PaginationRenderer.DEFAULT` for every part it does not override |
| threading | caller's thread | `withContext(Dispatchers.Default + CoroutineName("SuspendPagination-Rendering") + CoroutineExceptionHandler)`, header/rows/footer in parallel `async`s |

`renderComponent` is `Component.join(JoinConfiguration.newlines(), render(content, page))`. Use
`render` when the lines have to go out separately (one chat message per line, a dialog, a log).

## Building

Both companions are invokable, so the builder reads like a DSL:

```kotlin
val pagination = Pagination<ExampleData> {
    title { primary("Example Pagination") }
    rowRenderer { data, index ->
        listOf(
            buildText {
                append(CommonComponents.EM_DASH)
                appendSpace()
                variableValue(data.name)
                appendSpace()
                info("(${data.age})")
            }
        )
    }
}
```

`PaginationBuilder.builder<T>()` returns the builder when you want to configure it imperatively.
`Pagination` instances are `data class`es holding only their configuration, so build one once in a
field and render it per command invocation - that is what `local/paper-plugin-test`'s
`PaginationTest` does.

### Properties

| Property | Type | Default | Notes |
| --- | --- | --- | --- |
| `title` | `Component` | **required** | getter throws `Title must be set before building`. `title { }` sets it from a builder block |
| `rowRenderer` | `PaginationRowRenderer<T>` | **required** | getter throws `Row renderer must be set before building`. `rowRenderer(r)` and `rowRendererSimple(r)` are setters |
| `width` | `Int` | `Pagination.DEFAULT_WIDTH` = 33 | `require(value >= 3)` in the setter |
| `indent` | `Int` | `DEFAULT_INDENT` = 2 | `require(value >= 0)` |
| `resultsPerPage` | `Int` | `DEFAULT_RESULTS_PER_PAGE` = 6 | `require(value > 0)` |
| `renderer` | `PaginationRenderer` | `PaginationRenderer.DEFAULT` | the frame |
| `clickEventProvider` | `PaginationClickEventProvider<T>` | `.default()` | what a page button does |
| `firstPageButton` `previousPageButton` `nextPageButton` `lastPageButton` | `PageButton` | `Pagination.DEFAULT_*_PAGE_BUTTON` | `PageButton(text, enabledStyle, disabledStyle)` |

The defaults are `[<<]`, `[<]`, `[>]`, `[>>]`, enabled in `SUCCESS` with a German hover
(`Erste Seite`, `Vorherige Seite`, `Nächste Seite`, `Letzte Seite` in `INFO`), disabled in `GRAY`
with no hover and no click. The four `*PageButton(text, enabledStyle, disabledStyle)` builder
functions are shorthand for assigning a `PageButton`.

### Row renderers

`PaginationRowRenderer<T>` is a `fun interface`: `renderRow(value: T, index: Int): Collection<Component>`.
Returning several components makes one entry span several lines. `index` is the index into the whole
content collection, not into the page, so it is the number to display.

`PaginationRowRenderer.Simple<T>` is a nested `fun interface` with `render(value: T): Component` -
use `rowRendererSimple { }` for the one-line case.

The suspend versions declare their methods on `CoroutineScope`
(`suspend fun CoroutineScope.renderRow(value: T, index: Int): Collection<Component>`), so write them
as an object expression rather than a lambda:

```kotlin
val bans = SuspendPagination<BanEntry> {
    title { primary("Bans") }
    resultsPerPage = 10
    rowRenderer(object : SuspendPaginationRowRenderer<BanEntry> {
        override suspend fun CoroutineScope.renderRow(
            value: BanEntry,
            index: Int,
        ): Collection<Component> {
            val name = nameCache.lookup(value.uuid)   // suspends
            return listOf(
                buildText {
                    variableKey(name)
                    append(CommonComponents.MAP_SEPARATOR)
                    variableValue(value.reason)
                }
            )
        }
    })
}

val page = bans.renderComponent(entries, page = 1)
```

## Behaviour at the edges

- empty content renders a single line, `renderer.renderEmpty()` - the info prefix plus
  "Es wurden keine Ergebnisse gefunden."
- `page !in 1..pages` renders a single line, `renderer.renderUnknownPage(page, pages)` - the error
  prefix plus "Unbekannte Seite: N. Es gibt nur M Seite(n)." Nothing throws.
- pages are 1-based; `pages = (count + pageSize - 1) / pageSize`
- `forEachPageEntry` indexes directly when the content is a `List` that is also `RandomAccess`,
  otherwise it walks the iterator and skips the earlier pages. A `Sequence`-backed or linked
  collection therefore costs O(page × pageSize) to reach the page.

## The frame

`PaginationRenderer` is an interface of defaults - override only what you need and keep the rest:

```kotlin
val pagination = Pagination<Entry> {
    title { primary("Entries") }
    rowRendererSimple { buildText { variableValue(it.name) } }
    renderer = object : PaginationRenderer {
        override fun renderEmpty() = buildText {
            appendInfoPrefix()
            info("Keine Einträge.")
        }
    }
}
```

The default frame is: a strikethrough `*----*` rule of `width` characters in `DARK_SPACER`, a newline
plus `indent` spaces plus the title (`colorIfAbsent(PRIMARY)`, bold), a blank line, the rows (each
prefixed with `indent` spaces by `renderRow`), then the footer. A single-page result gets a plain
closing rule; a multi-page result gets the rule with the navigation row centred in it, using
`DefaultFontInfo.pixelWidth` over the plain text of the navigation to work out how many dashes go on
each side. Overriding `renderHeader` or `renderFooter` means re-deriving that geometry yourself.

`SuspendPaginationRenderer` mirrors it, every member delegating to `PaginationRenderer.DEFAULT`
except `renderRow` and `renderFooter`, which it reimplements - the footer `async`s all four buttons.

## Click events

`PaginationClickEventProvider<T>.getCallback(targetPage, pagination, content): ClickEvent<*>`. The
default re-renders the target page and sends it to whoever clicked:

```kotlin
ClickEvent.callback { clicker -> clicker.sendMessage(pagination.renderComponent(content, targetPage)) }
```

That means **the content collection is captured in the callback** and lives as long as Adventure
keeps the callback registered. For a large or mutable collection, supply your own provider that
re-runs the command instead:

```kotlin
clickEventProvider { targetPage, _, _ -> ClickEvent.runCommand("/bans $targetPage") }
```

The suspend default captures the `CoroutineScope` that rendered the page and `launch`es the re-render
on it. Once that scope is cancelled - the command finished, the plugin disabled - clicking a page
button does nothing, silently. Prefer a `runCommand` provider for anything long-lived.

## Not this API

`platform/paper/api/.../inventory/framework/view/pagination/` and the Minestom equivalent are a
*different* pagination: GUI pages inside an inventory view. Different types with similar names
(`PaginationButton`, `PaginationDsl`). The `surf-api-inventory` skill covers those.
