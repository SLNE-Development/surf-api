# The container, its components and the container DSL

Sources: `view/container/ViewContainer.kt`, `view/container/component/`,
`view/container/dsl/`, `view/AbstractSurfView.kt`, `view/ViewNavigationHistory.kt`.

## ViewContainer

`@PublishedApi internal class ViewContainer` is an ordered, thread-safe
(`CopyOnWriteArrayList`) set of `ViewContainerComponent`s whose `render()` composes the inventory
title. `addChild` uses `addIfAbsent`, so duplicates by `equals`/`hashCode` are silently dropped -
every component must implement both (the interface redeclares them abstract). Besides
`addChild`/`removeChild` it has `hasComponentOfType<T>()`, `removeChildrenOfType<T>()`,
`removeChildren { predicate }` and `lastRenderedTitle`, the component `render()` produced last.

`render()` partitions the children on `hasExactWidth`, renders the exact ones first and the
estimated ones after, wraps each in `appendShiftedComponent(positionalShift, textureWidth)`, sets
`surf:menu` as the font of the whole result, and stores it in `lastRenderedTitle`.

The container lives in a `lazyState` on `AbstractSurfView`, so it is **per view session**.

## ViewContainerComponent

```kotlin
interface ViewContainerComponent {
    val positionalShift: Int
    val textureWidth: Int
    val hasExactWidth: Boolean get() = true
    fun SurfComponentBuilder.renderComponent()
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}
```

Implement it for a new header visual; see `references/header-and-alignment.md` for why
`textureWidth` has to be exact and what `hasExactWidth = false` buys you.

| Component | Visibility | Shift / width | Notes |
| --- | --- | --- | --- |
| `ViewContainerTitleComponent` | public | `textAlignment.calculateShift(width, geometry.titleOptions(...))` / laid-out width | `title`, `font`, `textAlignment`, `geometry`, `defaultColor`, `metrics`; `String` secondary constructor; equality on title + font + alignment; `hasExactWidth = false` |
| `ViewContainerRowTextComponent` | public | `alignment.calculateShift(width, geometry.columnSpanOptions(columns, …))` / laid-out width | `row` (1-6, validated), `text`, `font`, `alignment`, `columns` (0-8, non-empty, validated), `geometry`, `defaultColor`, `metrics`; equality on row + text + font + alignment + columns; `hasExactWidth = false` |
| `ViewContainerGlyphComponent` | internal | `-48` / `222` | the per-`ViewRows` background texture; added only when `backgroundGlyph` is on |
| `ViewBlockCellComponent` | public | `geometry.slotGlyphShift(column)` / `slotSize + 1` | `column` 0-8 and `row` 1-6, both validated; glyph per row from the internal `BlockRow` enum; `forSlot(slot, geometry)`; equality on column + row only (geometry is not part of it) |
| `PaginationButtonGlyphComponent` | internal | `geometry.columnShift(2) + 0` / `89` | sealed, four states |
| `PaginationPageIndicatorComponent` | internal | centred in the 58px gap between the arrows / laid-out width | `hasExactWidth = false` |
| `ViewContainerBackHintComponent` | deprecated, hidden | `0` / `0` | renders nothing; the back arrow is part of the inventory texture now |

Blocking a cell is **cosmetic**: it draws a texture over the cell and changes neither its contents
nor its click handling. Whether a blocked slot can be interacted with is decided purely by the
`cancelOn*` settings.

## Modifying the container

`AbstractSurfView.modifyContainer(context) { }` (protected) runs the block against a
`ViewContainerModificationContext` and then pushes the new title: `config.title(container.render())`
inside `modifyConfig` for an `OpenContext`, `context.updateTitleForEveryone(container.render())`
for anything else.

From the DSL there are two entry points:

- `containerDefaults { }` on the view context - runs inside the open, right after the background
  glyph and the title defaults have been applied.
- `Context.modifyContainer { }` (`view/SurfViewLifecycleDsl.kt`, needs the `AbstractSurfViewRef`
  context parameter the lifecycle blocks already carry) - from any callback:

```kotlin
onClick {
    modifyContainer { header(text("Seite 2", Colors.GOLD)) }
}
```

In a subclass use `modifyContainer(context) { }` or the convenience
`updateHeader(context, header, alignment = null)` (`Component` or `String`).

### Title defaults on open

`applyContainerDefaults(open)` does, in order: add `ViewContainerGlyphComponent(settings.rows)` when
`backgroundGlyph` is on, `applyHeader(header)`, then the `containerDefaults()` hook. The header it
uses is the title already on the config if there is one, otherwise `Component.text(defaultHeader)` -
so `onInit { title(...) }` or whatever opened the view wins over the constructor argument. After
`onViewOpen`, `adoptExternalHeader(open)` compares the config title against
`container.lastRenderedTitle` and, if they differ, re-applies it as the container's header. That is
what makes `modifyConfig { title(...) }` inside `onViewOpen` survive; anywhere else it replaces the
whole rendered container and the header falls apart.

`applyHeader(header, alignment)` removes every `ViewContainerTitleComponent` and adds a fresh one
built from the view's `settings` (`font`, `headerGeometry`, `headerTextColor`,
`headerFontMetrics`), keeping the alignment of the title currently rendered when `alignment` is
`null`, and falling back to `settings.headerTextAlignment`.

## The container DSL

All of `view/container/dsl/ViewContainerDSL.kt` takes `ViewContainerModificationContext` as a
context parameter; the convenience overloads additionally take `AbstractSurfViewRef`, which is how
they read the view's settings.

Components:

- `addChild(component)`, `removeChild(component)`, `hasComponentOfType<T>()` /
  `hasComponentOfType(clazz)`, `removeChildrenOfType<T>()` / `removeChildrenOfType(type)`.

Title:

- `header(header: Component, alignment: TextAlignment? = null)`,
  `header(header: String, alignment = null)`,
  `header(alignment = null) { SurfComponentBuilder }`.

Row text (rows one-based, columns zero-based and inclusive):

- `rowText(row, text: Component, alignment = CENTER, columns = 0 until 9)` - uses the view's
  `rowFont(row)`, `headerGeometry`, `headerTextColor`, `rowFontMetrics`, and
  `require(row <= settings.rows.rows)`.
- `rowText(row, text: String, …)` and `rowText(row, alignment, columns) { SurfComponentBuilder }`.
- `rowText(row, text, font, alignment, columns, geometry, defaultColor, metrics)` - the explicit
  form, for use outside a view's DSL scope or with a font other than the view's row font.
- `rowTextAt(row, column, text)` - left-aligned from `column` to the last column.
- `clearRowText(row)`, `clearRowTexts()`.

Block cells (every one takes a trailing `geometry: ViewHeaderGeometry =
SurfViewSettingsDefaults.DEFAULT_HEADER_GEOMETRY` - pass the view's when it is not the default):

- `blockCell(column, row, geometry)`, `blockSlot(slot, geometry)`.
- `blockRow(row, exemptColumns: IntArray, geometry)` and
  `blockRow(row, exemptColumns: Collection<Int> = emptyList(), geometry)`.
- `blockColumn(column, exemptRows: IntArray, rows = 6, geometry)` and the `Collection<Int>` form.
- `blockAllSlots(rows: ViewRows, exemptSlots: Collection<Int> = emptyList(), geometry)` and
  `blockAllSlots(rows: Int, …)`.
- `unblockCell(column, row)`, `unblockSlot(slot)`, `unblockAllSlots()`.

```kotlin
containerDefaults {
    header { primary("Statistiken") }
    rowText(1, "<- Klick da mal drauf")
    rowText(2, text("links", Colors.GREEN), TextAlignment.LEFT, columns = 0..2)
    rowText(5, columns = 3..5) {
        text("Guthaben: ", Colors.DARK_SPACER)
        text("1.250", Colors.GREEN)
    }
    blockAllSlots(ViewRows.FIVE, setOf(1))
}
```

The file ends with a block of `DeprecationLevel.HIDDEN` binary-compatibility shims for the
signatures that existed before `geometry` (and `blockColumn`'s `rows`) were added, plus a no-op
`backHint()`. They are invisible to Kotlin callers; never write them in new code.

## Navigation history

`ViewNavigationHistory` (internal object) keeps one `ArrayDeque<NavEntry>` per player UUID, where
`NavEntry(viewClass, data)`, plus a set of UUIDs with a pending back navigation.
`AbstractSurfView.onOpen` records every open: a consumed back navigation records nothing,
`viewer.isSwitching` pushes forward, anything else resets the deque to a single entry.

With `navigateBackOnOutsideClick` on (the default), an outside click is intercepted *before*
`onViewClick`: `popToPrevious` drops the current entry and returns the one below. If there is one,
the history is marked and `openForPlayer(target.viewClass, target.data)` runs; if not, the history
is cleared and the view closes. `onClose` clears the history unless the viewer is switching views or
a back navigation is pending.

So: chained menus opened through `openForPlayer` / `openForEveryone` get a working back gesture for
free, and nothing is drawn in the header for it - the arrow is part of the inventory texture.
`IFConfinedContext.openForPlayer(view: RootView[, initialData])` and
`IFContext.openForEveryone(other: RootView[, initialData])` in `framework/dsl/` are the type-safe
wrappers that take a view instance instead of a `Class`.
