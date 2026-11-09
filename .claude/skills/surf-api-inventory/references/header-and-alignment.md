# The header: fonts, geometry and alignment

Sources: `view/settings/ViewFontMetrics.kt`, `view/settings/ViewHeaderGeometry.kt`,
`view/settings/align/` and `view/settings/SurfViewSettingsDefaults.kt`, over the shared math in
`common/core/api/.../inventory/framework/internal/` (`ViewSlotGeometry`, `TextAlignmentMath`,
`DefaultFontWidths`, `ShiftGlyphs`, `ViewTextLayout`, `ViewContainerRendering`).

## The model in four sentences

A chest inventory has no API for drawing anything above the slots, so surf-api draws **everything**
as glyphs inside the inventory *title*. Horizontal placement is a cursor shift: emit shift glyphs
whose advances are powers of two (`ShiftGlyphs`, via `shift(amount)` / `GlyphShift`), then the
visual, then the inverse shift. Vertical placement is impossible at runtime - a glyph sits wherever
its font provider's `ascent` in the resource pack puts it - so a texture that belongs in row 3 needs
its own glyph character and text that belongs in row 3 needs its own *font*. Every offset is
measured from the **title origin**: the pixel the client starts drawing the title at, with negative
values to its left.

## Measuring text

`layoutViewText(component, charSpacing, uppercase, spaceShift, advance)` (core) walks the component
tree per part and per Unicode code point, applies the transformations, and returns a
`LaidOutViewText(component, width)` - the component to emit and its exact pixel advance. It asks
`advance(codePoint, bold)` for every glyph, which is how the same code serves the proportional
vanilla font and a fixed-width pack font. Bold adds `DefaultFontWidths.BOLD_EXTRA_ADVANCE = 1` per
glyph. Translatable parts are measured on their fallback (or key) and keybind parts on the keybind
name, and neither is transformed, because what the client draws depends on its language file.

`ViewFontMetrics` is the description of the font the text is rendered in:

| Property | Default | Meaning |
| --- | --- | --- |
| `charSize` | `DefaultFontWidths.DEFAULT_ADVANCE` (6) | advance of a glyph with no `charWidths` entry |
| `charSpacing` | `0` | pixels inserted *before* every glyph, as a shift glyph inside the text; negative tightens |
| `uppercase` | `false` | uppercase before rendering, for a capitals-only font |
| `spaceAsShift` | `false` | render a space as a shift instead of a space glyph |
| `charWidths` | `emptyMap()` | per-code-point advance overrides |

plus `spaceShift` (= `advance(' ')` when `spaceAsShift`, else `null`) and
`advance(codePoint, bold = false)`.

Three ready-made instances:

- `ViewFontMetrics.VANILLA` - the client font: `charSize = 6`, `charWidths =
  DefaultFontWidths.advances`. The client font brings its own space provider, so spaces stay
  spaces.
- `ViewFontMetrics.VANILLA_ROW` - `VANILLA.copy(spaceAsShift = true)`, for the per-row fonts, which
  are copies of the client font's glyph sheets and therefore carry no space provider. This is the
  default `rowFontMetrics`.
- `ViewFontMetrics.SURF_MENU` - the pack's menu font: `charSize = MENU_CHAR_SIZE` (9),
  `charSpacing = MENU_CHAR_SPACING` (-1) → 8px per glyph, `uppercase = true`,
  `spaceAsShift = true`, `charWidths = mapOf(' '.code to MENU_SPACE_SIZE)` (4).

`DefaultFontWidths.advances` (`Map<Int, Int>`) is measured off `ascii.png`, `accented.png` and
`nonlatin_european.png`, so Latin, Latin Extended, Greek and Cyrillic are exact. Anything outside
those sheets - CJK from the Unifont fallback, custom pack glyphs - measures as 6 and needs an
override. It is `@InternalSurfApi`; reach it through `ViewFontMetrics.VANILLA.charWidths` instead of
importing it.

## Geometry

`ViewHeaderGeometry` is all horizontal, all relative to the title origin:

| Property | Default | Meaning |
| --- | --- | --- |
| `titleLeftShift` | `-8` | left edge of the inventory, i.e. of the title area |
| `titlePadding` | `0` | padding kept free on each side of the title area |
| `titleWidth` | `176` | width of the inventory = width the title is aligned in |
| `slotOriginX` | `-1` | left edge of slot column 0 |
| `slotSize` | `18` (`ViewSlotGeometry.SLOT_SIZE`) | pitch of the slot grid |
| `slotGlyphInset` | `0` | transparent columns on the left of a slot overlay texture |
| `rowTextPadding` | `1` | padding on each side of a column-span text |

Derived members: `slotAreaWidth` (9 × `slotSize`), `columnShift(column)` =
`slotOriginX + column * slotSize`, `slotGlyphShift(column)` = that plus `slotGlyphInset`, and the
two option factories below. `ViewHeaderGeometry.DEFAULT` is the vanilla chest, where the inventory
centre and the slot-grid centre coincide - a centred title sits exactly above a centred row text.
Only change this for a pack that replaces the inventory background with a different-sized texture:

```kotlin
settings {
    backgroundGlyph(true)
    headerGeometry { copy(titleWidth = 223, titleLeftShift = -48) }
}
```

`ViewSlotGeometry` (core) holds the grid constants - `COLUMNS = 9`, `MAX_ROWS = 6`,
`SLOT_SIZE = 18` - and `columnShift`, `spanWidth`, `columnOf(slot)`, `rowOf(slot)` (**one-based**),
`slotOf(column, row)`.

## Alignment

`TextAlignment` is `LEFT`, `RIGHT`, `CENTER`, each delegating to `TextAlignmentMath`:

```
leftAlignedShift   = leftShift + padding
rightAlignedShift  = leftShift + (containerWidth - 2*padding - textWidth) + padding
centerAlignedShift = leftShift + floorDiv(containerWidth - 2*padding - textWidth, 2) + padding
```

`floorDiv` means a title wider than the container overflows evenly, and an odd free space leaves the
extra pixel on the right - the way the client centres text itself. With the defaults, a centred
title is at `-8 + (176 - textWidth) / 2`.

`TextAlignmentOptions(leftShift, padding, containerWidth, charSize, charSpacing, charWidths)` is
what `calculateShift(textWidth, options)` consumes;
`TextAlignment.calculateTextWidth(text, options)` measures a plain string that is rendered verbatim.
Build the options from the geometry rather than by hand:

- `geometry.titleOptions(charWidths, charSize, charSpacing)` - the full inventory width, what the
  title uses.
- `geometry.columnSpanOptions(columns, charWidths, charSize, charSpacing)` - a span of slot columns,
  what row text uses (`require(!columns.isEmpty())`).

## Rows come from fonts

`SurfViewSettingsDefaults.rowFont(row)` is `surf:menu_font_row_1` … `surf:menu_font_row_6`: copies
of the vanilla font whose `ascent` centres their glyphs vertically in that slot row.
`DEFAULT_ROW_FONTS` maps 1..6 to them, and `SurfViewSettings.rowFont(row)` falls back to that when
`rowFonts` has no entry. Override one row with `rowFont(row, key)` or the whole map with
`rowFonts(map)` - and keep `rowFontMetrics` describing whatever you put there.

Two other fonts are named in the defaults: `ASCENDED_HEADER_FONT`
(`surf:menu_font_normal_ascended`), the vanilla font one line lower, for a header that should not
sit on the vanilla title line; and `DEFAULT_MENU_FONT` (`surf:menu`), which `ViewContainer.render()`
sets as the font of the whole composed title so the shift and texture glyphs resolve.

## Recipes

**Centre a title.** Nothing to do: `font = minecraft:default`, `headerFontMetrics = VANILLA`,
`headerTextAlignment = CENTER`, vanilla geometry. The title is centred over the slot grid, drawn in
`headerTextColor` (`0x404040`, vanilla's grey - white would be invisible on the vanilla
background) for any part that carries no colour of its own.

**Use a pack font for the title.** Set both, always together:

```kotlin
settings {
    font(key("surf", "menu"))
    headerFontMetrics(ViewFontMetrics.SURF_MENU)
}
```

A `font` without matching metrics is the single most common cause of a title a few pixels off
centre.

**Measure a glyph the table does not cover** (a custom pack glyph, CJK):

```kotlin
settings {
    headerFontMetrics(
        ViewFontMetrics.VANILLA.copy(
            charWidths = ViewFontMetrics.VANILLA.charWidths + ('ꐱ'.code to 18)
        )
    )
}
```

**Left-align the title, right-align a counter in row 2:**

```kotlin
settings { headerTextAlignment(TextAlignment.LEFT) }

containerDefaults {
    header { primary("Shop") }
    rowText(2, text("1/4", Colors.GRAY), TextAlignment.RIGHT, columns = 6..8)
}
```

Column spans are zero-based and inclusive; rows are one-based.

**Nudge something by hand.** `shift(amount)` (`view/util/GlyphShift.kt`) renders the minimal glyph
string for a pixel offset - positive right, negative left - for a component you compose yourself.
Prefer a `ViewContainerComponent` with a `positionalShift`, which the container resets for you.

## Why widths have to be exact

`appendShiftedComponent(positionalShift, textureWidth) { … }` emits `shift(positionalShift)`, the
visual, then `shift(-(textureWidth + positionalShift))`. That last shift only lands back on the
container origin if `textureWidth` equals what the visual really advanced; otherwise every component
after it inherits the drift. Components whose width comes from a metrics table - the title, row
text, the page counter - report `hasExactWidth = false`, and `ViewContainer.render()` partitions on
that flag and renders the inexact ones last, so a mis-measured text can shift nothing but itself.
A fixed-texture component (background glyph: `-48`/`222`; block cell: `slotSize + 1`; button bar:
`89`) must get its number right.
