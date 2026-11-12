# Pagination

Sources: `view/pagination/` (`AbstractPaginatedSurfView`, `PaginationDsl.kt`,
`PaginationDslBuilder`, `PaginationSourceType`, `PaginationButton`,
`PaginationButtonGlyphComponent`, `PaginationPageIndicator`, `PaginationPageIndicatorComponent`),
`view/settings/Pagination*.kt`, and IF's `PaginationImpl` / `StateAccessImpl`.

## Shape of a paginated view

```kotlin
val listView = paginatedSurfView("Einträge") {
    settings {
        paginationViewRows(PaginationViewRows.THREE)
        paginationEmptyRows(PaginationEmptyRows.ONE)
        paginationButtonsAtBottom()
    }

    layoutTarget('I')

    containerDefaults {
        header { primary("Einträge") }
    }

    pagination<MyEntry> {
        computedSource { context -> repository.entriesFor(context.player) }
        itemFactory { entry ->
            withItem(ItemType.PAPER) { displayName { text(entry.name) } }
            onItemClick { openForPlayer(detailView, entry) }
        }
        onPageSwitch { context, pagination ->
            logger.info("page ${pagination.currentPage()}")
        }
        horizontal()
    }
}
```

`PaginatedSurfViewContext` stores three things the DSL must fill: `layoutTarget`, the
`paginationStateBuilder` factory, and `settings`. `getRegisteredLayoutTarget()` and
`getRegisteredPaginationStateBuilder()` throw `IllegalStateException` ("Missing layout target for
paginated view." / "Missing pagination configuration for paginated view.") when they are not, and
that happens while `PaginatedSurfViewDSLImpl` is being constructed - i.e. at the
`paginatedSurfView(...)` call, not on open.

`pagination<T> { }` itself throws `"Pagination source must be configured."` if no source was set,
and `PaginationDslBuilder.applyTo` throws `"Pagination item factory must be configured."` if neither
`itemFactory` nor `elementFactory` was.

## Sources: which one, and when it runs

`PaginationDslBuilder` sets exactly one `PaginationSourceType`; the last call wins. `PaginationDsl`
then maps it onto an IF `PaginationStateBuilder`:

| DSL call | Parameter type | `PaginationSourceType` | IF builder | Evaluated |
| --- | --- | --- | --- | --- |
| `source(items)` | `Iterable<T>` | `Static` | `buildPaginationState(list)` | **once per registered view**, at init |
| `source { }` | `() -> Iterable<T>` | `Static` | `buildPaginationState(list)` | **once per registered view**, at init |
| `computedSource { }` | `(Context) -> Iterable<T>` | `Computed` | `buildComputedPaginationState` | on every page load and every update |
| `asyncSource { }` | `(Context) -> CompletableFuture<Iterable<T>>` | `ComputedAsync` | `buildComputedAsyncPaginationState` | on every page load, awaited |
| `lazySource { }` | `(Context) -> Iterable<T>` | `Lazy` | `buildLazyPaginationState` | once per viewer session, on first page load |
| `lazyAsyncSource { }` | `(Context) -> CompletableFuture<Iterable<T>>` | `LazyAsync` | `buildLazyAsyncPaginationState` | once per viewer session, awaited |
| `suspendSource(scope \| plugin) { }` | `suspend CoroutineScope.(Context) -> Iterable<T>` | `ComputedAsync` | computed async | on every page load |
| `lazySuspendSource(scope \| plugin) { }` | `suspend CoroutineScope.(Context) -> Iterable<T>` | `LazyAsync` | lazy async | once per viewer session |

The suspend variants wrap the block in `scope.future { }` (`kotlinx-coroutines-jdk8`); the `plugin`
overloads use `plugin.scope` from mccoroutine-folia and exist on Paper only.

Why "once per registered view" for a static source: `PaginationDsl` calls
`sourceType.provider()` *inside* the factory it stores on the context, and that factory is invoked
from `AbstractPaginatedSurfView.createPagination()`, which runs from the `by lazy paginationState`
the first time `onViewInit` touches it. A view is registered once, so the list is captured once.
IF then marks that pagination `isStatic` and never re-reads it. A menu that shows yesterday's data
is this bug.

For live data pick by refresh cost:

- `computedSource` - cheap, in-memory, always current.
- `lazySource` - one database or network hit per menu session.
- `asyncSource` / `suspendSource` - anything blocking; IF awaits the future before rendering the
  page, and `Pagination.isLoading` is true meanwhile.

## Item factories

- `itemFactory { entry -> … }` - `BukkitItemComponentBuilder.(T) -> Unit`. Use the item DSL inside
  it (`withItem`, `renderWith`, `onItemClick`, `onItemRender`).
- `elementFactory { context, builder, index, value -> … }` - when the render context or the
  zero-based index matters.

Setting one clears the other, so the later call wins.

Optional: `onPageSwitch { context, pagination -> }` (`BiConsumer<Context, Pagination>`),
`orientation(Pagination.Orientation)`, `horizontal()`, `vertical()`.

## Rows, the button row and the layout

`PaginationViewRows` fixes both numbers: `ONE`→2 total rows, `TWO`→3, `THREE`→4, `FOUR`→5,
`FIVE`→6, i.e. `actualRows = contentRows + 1`; the extra row is the navigation row.
`PaginatedViewSettings.rows` is that `actualRows`, so a paginated view's height is never set
directly. `PaginationEmptyRows` (`NONE`/`ONE`/`TWO`/`THREE`, `byRows(0..3)`) then takes rows *out of
the content*, at the inventory edge opposite the buttons; the height does not grow. The constructor
requires at least one content row left:

```
paginationButtonRow     = BOTTOM ? rows.rows : 1
paginationContentRows   = BOTTOM ? (1 + emptyRows) until rows.rows
                                 : 2..(rows.rows - emptyRows)
paginationContentRowCount = rows.rows - 1 - emptyRows
```

`createLayout()` builds one 9-character string per row: a content row is
`" " + layoutTarget.repeat(7) + " "`, everything else is nine spaces. So **columns 0 and 8 are never
content**, and one page holds `7 * paginationContentRowCount` items. `onViewInit` is final and calls
`config.layout(*createLayout())` after `onPaginatedInit`, so a user layout is discarded.

`containerDefaults` is final too: it blocks every cell that is not content
(`y in paginationContentRows && x in 1..7` is the only exemption), passing the view's
`headerGeometry`, then calls the open `applyContainerDefaults()` hook - which is what the DSL's
`containerDefaults { }` block feeds.

## The navigation buttons

`PaginationButton` is internal: `LEFT(column = 2)`, `RIGHT(column = 6)`, with
`clickSlot(paginationRow) = column + (paginationRow - 1) * 9`. `onViewRender` puts an empty item
(`ItemStack.empty()`, Minestom `ItemStack.AIR`) in each, `updateOnStateChange(paginationState)`,
`displayIf(pagination::canBack)` / `displayIf(pagination::canAdvance)`, and on click calls
`pagination.back()` / `pagination.advance()` plus `playPageSwitchSound`. The arrows themselves are
not items - they are a glyph in the header.

`PaginationButtonGlyphComponent` (internal, sealed) draws the bar: `Disabled`, `DisabledLeft`
(only next available), `DisabledRight` (only previous available), `Enabled`, chosen by
`getByPaginationState(row, pagination, geometry)`. Glyph characters come from the shared
`PaginationButtonGlyphs` table in core, one per row 1-6. Constants worth knowing:
`TEXTURE_WIDTH = 89`, `GLYPH_INSET = 0`, `COUNTER_AREA_OFFSET = 15`, `COUNTER_AREA_WIDTH = 58`, and
`counterAreaShift(geometry) = geometry.columnShift(2) + GLYPH_INSET + 15`. The texture
(`menu/pagination/buttons.png`, 88x48 as four 88x12 bands) cannot line up with both slot centres at
once - its arrows are 73px apart, the slots 72 - so fix that in the texture, not in the offset.

## The page indicator

`PaginationPageIndicator` is a `fun interface`: `render(currentPage: Int, totalPages: Int):
Component?`. Both arguments are one-based and never below 1; returning `null` renders nothing for
that state. `PaginationPageIndicator.Default` renders `"$currentPage/$totalPages"` in
`Colors.WHITE`.

```kotlin
settings {
    paginationPageIndicator { current, total ->
        if (total <= 1) null else text("Seite $current von $total", Colors.GRAY)
    }
}
```

`noPaginationPageIndicator()` sets it to `null` and drops the component entirely.
`PaginationPageIndicatorComponent` centres the text in the 58px gap between the arrows rather than
on the slot grid, using `settings.rowFont(buttonRow)` for the vertical position and
`settings.rowFontMetrics` for the width, and reports `hasExactWidth = false`.

## When the overlay refreshes

`updatePaginationOverlay(context)` reads the pagination, swaps the button glyph and the page counter
in the container, and lets `modifyContainer` push the new title. It runs:

- from `onPageSwitch`, prepended to whatever handler the DSL registered (`prepend` from core util),
  so the overlay updates before user code;
- once after the initial render - for a static pagination immediately, scheduled a tick later
  (`player.scheduler.run(...)` on Paper, `nextTick { }` on Minestom); for a dynamic one through
  `InitialPaginationStateWatcher`, a `StateWatcher` on `pagination.id` that fires on the first
  non-loading value and then disarms itself via an `AtomicBoolean`.

`onViewUpdate` is final and calls `pagination.switchTo(pagination.currentPageIndex())` when that
page exists, which re-runs the element factories (and, for a computed source, the source) before
delegating to `onPaginatedUpdate`.

Page numbers come from IF: `pagination.currentPage()` / `lastPage()` are one-based,
`currentPageIndex()` is zero-based, and the overlay coerces them into `1..max(lastPage, 1)`.
