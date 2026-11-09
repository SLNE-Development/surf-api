---
name: surf-api-inventory
description: Use when building or fixing a surf-api inventory menu - writing a surfView or paginatedSurfView, subclassing AbstractSurfView or AbstractPaginatedSurfView, registering or opening a view through the ViewFrame, choosing between onInit/onOpen/onFirstRender/onClick/onClose/onUpdate, placing items with slot/layoutSlot/availableSlot, declaring view state (state, mutableState, computedState, lazyState, initialState) or a stateful/toggle/triple button, supplying a pagination source and getting the page buttons or page indicator right, centring or aligning a title or row text, blocking cells, picking ViewRows/PaginationViewRows/SurfViewSettings, tuning ViewFontMetrics, ViewHeaderGeometry or TextAlignment, using ViewIcon, or diagnosing "Missing layout target", "Pagination source must be configured", "Cannot access view during DSL configuration", a title that lands off centre or a menu showing stale data.
---

# surf-api inventory framework

Every claim below comes from
`platform/paper/api/src/main/kotlin/dev/slne/surf/api/paper/inventory/framework/` and
`common/core/api/src/main/kotlin/dev/slne/surf/api/core/inventory/framework/internal/`. Read those
files when in doubt; they are the only source of truth.

The framework is a Kotlin layer on top of the **InventoryFramework** fork
(`dev.slne.forks.inventoryframework`, package `me.devnatan.inventoryframework`). IF supplies
`View`, `ViewFrame`, the `Context` hierarchy, the item component builders, states and `Pagination`;
surf-api supplies the DSL, the settings, and the header rendering that draws titles, row text and
slot overlays as glyphs inside the inventory title.

## Where things live

| Package under `…/inventory/framework/` | What |
| --- | --- |
| `view/` | `AbstractSurfView`, `SurfViewBuilder.kt` (`surfView`, `paginatedSurfView`), `SurfViewContext`, `SurfViewDSLImpl`, `SurfViewRef`, `SurfViewLifecycleDsl.kt`, `ViewNavigationHistory` |
| `dsl/` | extensions on IF's contexts and builders: `slot`, `layoutSlot`, `withItem`, `onItemClick`, `cancel`, `layout { }` |
| `view/pagination/` | `AbstractPaginatedSurfView`, `pagination { }`, `PaginationDslBuilder`, `PaginationSourceType`, button glyph, page indicator |
| `view/state/`, `view/button/` | `StateDsl.kt`, `StateHandle`, `StateRegistry`; `statefulButton`/`toggleButton`/`tripleButton`, `ViewButtonHandle` |
| `view/container/` | `ViewContainer`, `ViewContainerComponent`, the concrete components, `ViewContainerDSL.kt` |
| `view/settings/` | `SurfViewSettings`, `SimpleViewSettings`, `PaginatedViewSettings`, `ViewRows`, `ViewFontMetrics`, `ViewHeaderGeometry`, `align/TextAlignment`, `builder/` |
| `view/icon/`, `ViewFrameAccessor.kt`, `extensions.kt` | `ViewIcon`; `viewFrame`, `register`, `open`, `titleBuilder`, `modifyConfig`, `outlineItem` |

Everything is top-level functions with **context parameters**, so each one needs an import. Import
`…inventory.framework.view.*` for the lifecycle DSL, `…framework.dsl.*` for the slot/item DSL, and
the specific `view.container.dsl`, `view.state`, `view.button` and `view.pagination` members used.

## Declaring a view

Two ways, same result. The DSL is the normal one:

```kotlin
val statsView = surfView("Statistiken") {
    val clicks = mutableState(0)

    settings {
        rows(ViewRows.FIVE)
        cancelAllInteractions()
    }

    containerDefaults {
        header { primary("Statistiken") }
        rowText(2, text("Deine Werte", Colors.GREEN), TextAlignment.LEFT, columns = 0..2)
        blockAllSlots(ViewRows.FIVE, setOf(1))
    }

    onFirstRender {
        slot(1, 2) {
            withItem(ItemType.DIAMOND) { displayName { text("Diamant") } }
            onItemClick { clicks.increment(this) }
        }
    }

    onClose { player.sendMessage(text("${clicks[this]} Klicks")) }
}
```

`surfView(header: String, block: context(SurfViewContext) () -> Unit): AbstractSurfView` builds a
`SurfViewContext`, runs the block against it, then instantiates an anonymous `SurfViewDSLImpl` and
resolves the `SurfViewRef`. Nothing in the block sees the view instance yet - that is what the ref
is for. `paginatedSurfView` does the same with `PaginatedSurfViewContext` and
`PaginatedSurfViewDSLImpl`.

The other way is a subclass of `AbstractSurfView(defaultHeader)` (or
`AbstractPaginatedSurfView(header)`), overriding `onViewInit`, `onViewOpen`, `onViewRender`,
`onViewClick`, `onViewClose`, `onViewUpdate` and `containerDefaults`. The IF methods themselves are
**final** - they apply the settings, build the container and record navigation history before
delegating. A subclass gets `modifyContainer(context) { }` and
`updateHeader(context, header, alignment)` as protected members.

### Register, then open

```kotlin
statsView.register()          // JavaPlugin#onLoad / onLoadAsync
statsView.open(player)        // or open(player, data), open(players), open(players, data)
```

`register()` is `viewFrame.with(this)`, `open` is `viewFrame.open(javaClass, player[, data])`, and
`viewFrame` is `ViewFrameAccessor.INSTANCE.viewFrame()` - a `requiredService<ViewFrameAccessor>()`
lookup, implemented per platform (`@AutoService` in `platform/*/impl/.../ViewFrameAccessorImpl`).
Register during load; opening an unregistered view fails in IF.

## Lifecycle hooks and which context you get

| DSL function | Context receiver | Runs |
| --- | --- | --- |
| `onInit { }` | `ViewConfigBuilder` | once, at registration. `layout { }`, extra IF config |
| `onOpen { }` | `OpenContext` | per open, before the inventory exists. `cancel()` to refuse |
| `onFirstRender { }` | `RenderContext` | once per session. Place slots here |
| `onClick { }` | `SlotClickContext` | on any click in the view. `cancel()` |
| `onClose { }` | `CloseContext` | on close. `cancel()` to refuse |
| `onUpdate { }` | `Context` | on `update()` / state change |
| `containerDefaults { }` | `ViewContainerModificationContext` | during open, after the glyph and title defaults |

Every block also carries the view's `SurfViewRef` / `PaginatedSurfViewRef` as a second context
parameter, which is what makes `view`, `modifyContainer { }`, `header(...)` and the row-text
helpers resolve. `view` / `getRegisteredView()` throws
`"Cannot access view during DSL configuration"` when touched outside a callback.

`onInit` cannot change size or type: `AbstractSurfView.onInit` applies the cancel flags, calls
`onViewInit(config)`, and *then* does `config.size(settings.rows.rows)` and
`config.type(ViewType.CHEST)`. Row count comes from `settings`, nothing else.

## Settings

`SurfViewSettings` is a sealed interface with two implementations - `SimpleViewSettings` and
`PaginatedViewSettings`, both data classes - built through `settings { }`
(`SimpleViewSettingsBuilder` / `PaginatedViewSettingsBuilder`, or the standalone
`simpleViewSettings { }` / `paginatedViewSettings { }`). Defaults all live in
`SurfViewSettingsDefaults`.

| Setting | Default | Note |
| --- | --- | --- |
| `rows(ViewRows \| Int)` | `ViewRows.FIVE` | simple views only; paginated derives it |
| `font(Key)` | `minecraft:default` | title font; pair with `headerFontMetrics` |
| `headerTextAlignment(TextAlignment)` | `CENTER` | |
| `headerTextColor(TextColor)` | `0x404040` | vanilla title grey, for text without its own colour |
| `headerFontMetrics(ViewFontMetrics)` | `VANILLA` | |
| `rowFontMetrics(ViewFontMetrics)` | `VANILLA_ROW` | |
| `headerGeometry(ViewHeaderGeometry)` / `headerGeometry { copy(...) }` | vanilla chest | |
| `backgroundGlyph(Boolean)` | `false` | only for a pack drawing its own inventory texture |
| `rowFonts(Map<Int, Key>)` / `rowFont(row, Key)` | `surf:menu_font_row_1…6` | |
| `cancelOnClick/Drag/Drop/Pickup(Boolean)`, `cancelAllInteractions()` | all `true` | a `false` just skips the IF call |
| `navigateBackOnOutsideClick(Boolean)` | `true` | outside click pops `ViewNavigationHistory` |

`PaginatedViewSettingsBuilder` adds `paginationViewRows`, `paginationEmptyRows`,
`paginationButtonPosition` (`paginationButtonsAtTop()` / `…AtBottom()`),
`paginationPageIndicator` and `paginationSwitchSound` - see `references/pagination.md`.
`ViewRows` is `ONE…SIX` with `rows` (1-6) and a background `glyph`; `byRows(n)` / `byRowsOrNull(n)`.

## Pagination, in one breath

```kotlin
val shopView = paginatedSurfView("Shop") {
    settings {
        paginationViewRows(PaginationViewRows.THREE)   // 3 content rows, 4 rows total
        paginationEmptyRows(PaginationEmptyRows.ONE)
        paginationButtonsAtBottom()
    }

    layoutTarget('I')

    pagination<String> {
        computedSource { context -> shopRepository.offersFor(context.player) }
        itemFactory { entry ->
            withItem(ItemType.PAPER) { displayName { text(entry) } }
        }
    }
}
```

`layoutTarget` and `pagination { }` are both mandatory - missing either throws at view construction
("Missing layout target for paginated view" / "Missing pagination configuration"). **The layout is
generated, not yours**: content rows get `" IIIIIII "`, so a page holds **7 items per row**, and any
`onInit { layout { } }` is overwritten.

The one rule to get right: `source { … }` is a **snapshot taken once for the whole registered
view**, not per open. Use `computedSource` (re-read on every page load) or `lazySource` (once per
viewer session) for anything that can change. Full table, timings, button geometry and the page
indicator: **`references/pagination.md`**.

## State

State factories are called **in the DSL block**, never inside a callback
(`StateRegistry.register` rejects it), and return a `StateHandle<S>` that resolves after the view is
built: `state(v)`, `mutableState(v)`, `mutableState(intLiteral)` (→ `MutableIntState`),
`computedState { }`, `lazyState { }`, `initialState<T>()`, `initialState<T>(key)`. Read and write
with `handle[context]` / `handle[context] = v`, plus `increment(context)` / `decrement(context)` on
int state. A `MutableState` write triggers IF's re-render of everything watching it; a plain
`state`/`computedState` does not. Details and the `DeferredState` resolution order:
**`references/state-and-buttons.md`**, which also covers `statefulButton`, `toggleButton`,
`tripleButton`, their `computed*` variants, `ViewButtonHandle` and the `button(handle)` placement.

## Header, container and geometry

Everything above the slots - title, row text, slot overlays, the pagination bar - is a glyph
rendered **inside the inventory title**. `ViewContainer` holds the `ViewContainerComponent`s and
`render()` composes them, shifting the cursor by `positionalShift` before and
`-(textureWidth + positionalShift)` after each one. A glyph's *vertical* position is baked into the
resource pack's `ascent`, so rows are addressed by picking a per-row font or a per-row glyph, never
by arithmetic.

Out of the box a title is already centred correctly: vanilla font, `ViewFontMetrics.VANILLA`,
`TextAlignment.CENTER`, vanilla geometry. You only have to touch the machinery when you change the
font or the inventory texture - and then `font` and `headerFontMetrics` must describe the same font,
or the title is measured wrong and lands off centre:

```kotlin
settings {
    font(key("surf", "menu"))
    headerFontMetrics(ViewFontMetrics.SURF_MENU)
}
```

The concrete components are `ViewContainerTitleComponent`, `ViewContainerRowTextComponent`,
`ViewContainerGlyphComponent`, `ViewBlockCellComponent` and (deprecated, a no-op)
`ViewContainerBackHintComponent`. The container DSL gives `header(...)`, `rowText(...)`,
`rowTextAt(...)`, `clearRowText(s)`, `blockCell/blockSlot/blockRow/blockColumn/blockAllSlots`,
`unblock*`, `addChild`/`removeChild`/`removeChildrenOfType`. See
**`references/container-components.md`**; for the pixel math, `charWidths` overrides, custom
geometry and alignment recipes, **`references/header-and-alignment.md`**.

## Icons

`viewIcon(icon: ViewIconType, color: ViewIconColor) { }` builds a `PAPER` item whose `ITEM_MODEL`
data component is `nexo:surf_menu_icon_<color>_<icon>`. Colours: `RED`, `BLUE`, `WHITE`, `GREEN`,
`YELLOW`. Types: `ARROW_DOWN/UP/LEFT/RIGHT`, `ARROW_PREVIOUS`, `ARROW_NEXT`, `CROSS`, `CHECK`,
`USERS`, `HOME`, `COG`, `BELL`, `PLUS`, `MINUS`, `EQUALS`, `MENU`, `RELOAD`, `QUESTION_MARK`,
`EXCLAMATION_MARK`, `QUESTION`, `SEARCH`, `CIRCLE`. The `init` block runs *before* the item model is
applied, so it cannot override the model.

## Paper vs Minestom

`platform/minestom/api/.../minestom/inventory/framework/` is the same 62 files with the same names,
package `dev.slne.surf.api.minestom.*`. Everything in this skill holds for both - the whole `view/`,
`view/state/`, `view/button/`, `view/container/`, `view/settings/` and `view/pagination/` surface,
defaults, glyphs and pixel math included (the math is shared through
`common/core/api/.../inventory/framework/internal/`). Only these differ:

- Platform types: `BukkitItemComponentBuilder` → `MinestomItemComponentBuilder`
  (`dsl/PaperItemComponentBuilderDSL.kt` → `dsl/MinestomItemComponentBuilderDSL.kt`), `Material`
  and `ItemStack` from `net.minestom.server.item`, `Player` from `net.minestom.server.entity`.
- The Paper item DSL has both `ItemType` and `Material` overloads of `withItem`/`renderWith`; the
  Minestom one has `Material` only, and its `init` block receives an `ItemDsl`, not an `ItemStack`.
- Pagination: no `suspendSource(plugin)` / `lazySuspendSource(plugin)` overloads - pass a
  `CoroutineScope`.
- Scheduling: Paper hops a tick with `player.scheduler.run(JavaPlugin.getProvidingPlugin(...))`
  (Folia-safe), Minestom with IF's own `View.nextTick { }`.
- Empty button items: `ItemStack.empty()` vs `ItemStack.AIR`; back-navigation uses the
  single-argument `openForPlayer` overload when the history entry carries no data, because Minestom
  IF rejects a null `initialData`.
- `ViewIcon` sets the model via `itemModel(key("nexo", …))` instead of
  `setData(DataComponentTypes.ITEM_MODEL, …)`.

## Traps

1. **`slot(row, column)` is 1-based, the container DSL is not.** IF's `SlotConverter` computes
   `(row - 1) * 9 + (column - 1)`, so `slot(1, 1)` is slot 0. But `blockCell(column, row)`,
   `rowText(row, …)` and `columns = 0..2` use **1-based rows and 0-based columns**. The
   "zero-based row index" wording in `RenderContextDSL.kt`'s KDoc is wrong.
2. **A static pagination `source` never refreshes.** It is evaluated once, when the view's
   pagination state is created at init. Stale menus almost always mean `source` where
   `computedSource` was needed.
3. **Never set the title with `modifyConfig { title(...) }`** outside `onViewOpen` - it replaces the
   whole rendered container, dropping the alignment shift, the row texts and the header textures.
   Use `header(...)` in `containerDefaults`, `modifyContainer { header(...) }` from a callback, or
   `updateHeader(context, …)` in a subclass. A title set on the config during open *is* adopted as
   the container's header (`adoptExternalHeader`).
4. **A paginated view owns its layout and its border.** 7 content columns, a generated layout, and
   `containerDefaults` already blocks every cell outside the content area. Override
   `applyContainerDefaults` / the DSL `containerDefaults` for extras only.
5. **`font` and `*FontMetrics` must match.** Also `rowFonts` and `rowFontMetrics`. A mismatch
   silently mis-measures text, which shows up as "the title is a few pixels off".
6. **The container DSL defaults to `DEFAULT_HEADER_GEOMETRY`, not the view's.** If you customised
   `headerGeometry`, pass it to `blockCell`/`blockRow`/`blockAllSlots`/`rowText` explicitly.
7. **`ViewContainer` de-duplicates by `equals`.** Two row texts with the same component, font,
   alignment and column span collapse into one; block cells compare on column and row only.
8. **State handles are DSL-time only**, and `onFirstRender` really is *first* render - use
   `onUpdate` or a state change for anything that must re-run.
9. **`onClick` does not see outside clicks** while `navigateBackOnOutsideClick` is `true`; the click
   is consumed by the back navigation instead.
10. **`tripleButton` requires exactly three states** and every button state needs an appearance
    (`item(...)` or `renderItem { }`), both checked when the view is built, not when it opens.

## References

- `references/pagination.md` - sources and when each is evaluated, layout generation, button slots
  and glyphs, page indicator, overlay refresh, row/empty-row arithmetic.
- `references/state-and-buttons.md` - the state factories, `StateHandle`, `StateRegistry`,
  `DeferredState`, and the full stateful/toggle/triple button DSL.
- `references/header-and-alignment.md` - the title origin, pixel math, `ViewFontMetrics`,
  `ViewHeaderGeometry`, `TextAlignment`, row fonts, shift glyphs, recipes.
- `references/container-components.md` - `ViewContainer`, the components, the container DSL,
  `modifyContainer`, navigation history.
