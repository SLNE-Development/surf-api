---
name: surf-api-messages
description: Use when producing player-facing text in surf-api - building a chat message with buildText or sendText, reaching for a Colors constant or a prefix, adding a click or hover event, formatting a collection, map or Duration into a Component, parsing or writing MiniMessage through the configured instance, adding a MiniMessage tag, building a Pagination or SuspendPagination, sending a repeating action bar, converting text to small caps, measuring text in pixels, using Rarity, or deciding between the SurfComponentBuilder DSL and raw Adventure.
---

# surf-api messages and text

Every claim below comes from `common/core/api/src/main/kotlin/dev/slne/surf/api/core/`
(`messages/`, `minimessage/`, `actionbar/`, `font/`, `rarity/`) and its `common/core/impl`
counterpart. Read those files when in doubt; they are the only source of truth.

## "Component" means two different things here

- **Adventure's `net.kyori.adventure.text.Component`** - an immutable text object. That is what this
  skill is about. When this file says *text component*, it means this.
- **`dev.slne.surf.api.di.SurfComponent`** - the bean **lifecycle** interface
  (`load`/`enable`/`disable`) a bean implements when it has to start and stop. Nothing to do with
  text. The `surf-api-components` skill covers it; do not explain it from here.

The package `foundation/di/` is the lifecycle one. Text lives in `core/messages/`.

## Build text with `buildText`, send it with `sendText`

`SurfComponentBuilder` wraps Adventure's `TextComponent.Builder` and adds the Surf vocabulary -
the palette, the prefixes, collection and time formatting, click helpers. Two entry points:

```kotlin
import dev.slne.surf.api.core.messages.CommonComponents
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.sendText

// build a TextComponent
val line = buildText {
    variableKey("Name")
    append(CommonComponents.MAP_SEPARATOR)
    variableValue(player.name)
}

// build and send in one step
audience.sendText {
    appendSuccessPrefix()
    success("Du hast ")
    variableValue(amount)
    success(" Münzen erhalten.")
}
```

`buildText { }` returns a `TextComponent`. `SurfComponentBuilder()` returns the builder itself when
you need to hand it around; `SurfComponentBuilder { }` is the same thing as `buildText { }`.
`build()` is just `asComponent()` on the wrapped Adventure builder, so the builder stays mutable and
can be built more than once.

The colour helpers are **member extension functions** on `SurfComponentBuilder`
(`primary`, `secondary`, `info`, `note`, `success`, `warning`, `error`, `variableKey`,
`variableValue`, `spacer`, `darkSpacer`, plus the sixteen `MinecraftComponentBuilderColors`). Each
comes in `String`, `Boolean`, `Char` and `Number` overloads with a trailing
`vararg decoration: TextDecoration`. They only exist inside a builder receiver - outside one, use
`Colors.PRIMARY` and `Component.text(...)`.

### When to use raw Adventure instead

Use the builder for anything a player reads: it keeps the palette and the prefix consistent. Drop to
raw Adventure for a shared constant (`Component.text(...)` in a `val`, as `CommonComponents` and
`Colors` do), for a component type the builder does not produce (`Component.keybind`,
`Component.selector` - `translatable` is on the builder), and in an API signature, where the
parameter should be `ComponentLike`: `SurfComponentBuilder` implements it, so a builder is accepted
wherever a component is. The platform APIs take both - `SidebarBuilder` has
`line(text: Component)` and `buildLine { }`.

Full builder surface, the Adventure DSLs (`title`, `book`, `bossBar`, `sound`, `resourcePackRequest`),
the audience and pointer extensions, `CommonComponents`, `SurfMessageBundle` and the kotlinx
serializers: `references/component-builder.md`.

## The palette

`Colors` is an interface whose companion object holds every constant, so they are all reached as
`Colors.PRIMARY`:

| Constant | Use for |
| --- | --- |
| `PRIMARY` `#3b92d1` | branded headings, titles |
| `SECONDARY` `#5b5b5b` | subtitles, de-emphasised text |
| `INFO` `#97B3F7` | neutral system information, not a reply to an action |
| `NOTE` `#6EA6D9` | supplemental tips next to a primary message |
| `SUCCESS` `#65ff64` | positive outcome of a user action, only |
| `WARNING` `#ffa64d` | caution before something can go wrong |
| `ERROR` `#ee3d51` | failures |
| `VARIABLE_KEY` / `VARIABLE_VALUE` | the two halves of `Key: value` |
| `SPACER` / `DARK_SPACER` | `-`, `...`, `/` and the `>>`, `\|` of a prefix |

`PREFIX` is `» | ` in `PREFIX_COLOR` (= `PRIMARY`); `INFO_PREFIX`, `SUCCESS_PREFIX`,
`WARNING_PREFIX` and `ERROR_PREFIX` are the same shape in their own colour. In a builder, use
`appendInfoPrefix()` / `appendSuccessPrefix()` / `appendWarningPrefix()` / `appendErrorPrefix()`, and
`appendNewInfoPrefixedLine { }` (and the three siblings) to start a new prefixed line.

## MiniMessage: one configured instance

`dev.slne.surf.api.core.minimessage.miniMessage` - a top-level `val` delegating to
`SurfMiniMessageHolder.miniMessage()`. It is the **standard tag set plus** these:

| Tag | Effect |
| --- | --- |
| `<primary>` `<secondary>` `<info>` `<note>` `<success>` `<warning>` `<error>` `<variable_key>` `<variable_value>` `<spacer>` `<dark_spacer>` `<prefix_color>` | styling tag setting that `Colors` colour |
| `<prefix>` | self-closing, inserts `Colors.PREFIX`. Takes a type: `<prefix:info>`, `<prefix:success>`, `<prefix:warning>`, `<prefix:error>`. Any other value throws a parsing exception |
| `<small>` | modifying tag running `toSmallCaps()` over the text and **dropping the children** of what it wraps |

There are no `TagResolver`s registered beyond those, and no restricted-tag variant anywhere in the
repository.

**The rule for user-supplied input: never deserialize it.** Everything the standard tag set can do -
`<click:run_command>`, `<hover>`, `<insert>` - is available to whatever string you pass to
`deserialize`. Config files are trusted (an operator wrote them); `miniMessageArgument` in the
command layer parses player input and is therefore only safe behind a permission. To put untrusted
text *into* a template, pass it as a component placeholder
(`miniMessage.deserialize(template, Placeholder.component("name", Component.text(raw)))`) or build the
whole thing with `buildText` and skip MiniMessage entirely.

Watch which instance you have: parts of the Paper sidebar and scoreboard impl call
`MiniMessage.miniMessage()` - the vanilla instance, where the Surf tags do not exist. Detail and the
full tag list: `references/minimessage-and-fonts.md`.

## Pagination

Two parallel APIs under `messages/pagination/`, identical in shape:

- **`Pagination<T>`** - `render(content, page = 1): List<Component>` and
  `renderComponent(content, page = 1): Component` (joined with newlines). Everything is computed
  inline on the calling thread. Reach for this one by default.
- **`SuspendPagination<T>`** - the same two functions, `suspend`. Use it only when rendering a row
  has to suspend (a database or cache lookup per row). It renders inside
  `withContext(Dispatchers.Default + ...)` and `async`s the header, every row and the footer in
  parallel, so a row renderer must not touch main-thread-only game state.

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

sender.sendMessage(pagination.renderComponent(exampleDataList))
```

`title` and `rowRenderer` are required - the builder throws
`Title must be set before building` / `Row renderer must be set before building` at `build()`.
Defaults: `width = 33`, `indent = 2`, `resultsPerPage = 6`, the four German page buttons, and a
click event provider that re-sends the target page to the clicker. Page numbers are 1-based; an
out-of-range page renders the "Unbekannte Seite" line instead of throwing, and empty content renders
"Es wurden keine Ergebnisse gefunden."

Builder properties, custom renderers, overriding single parts of the frame, and the suspend variant's
pitfalls: `references/pagination.md`.

## Action bar

`ActionBarService` repeats one action bar for a `Duration`, recomputing the component every
`interval` (default `1.seconds`):

```kotlin
val job = player.sendActionBar(scope = plugin.scope, duration = 10.seconds) {
    info("Teleport in ")
    variableValue(secondsLeft)
    info("s")
}
```

`scope` is the owning plugin's scope - on Paper, MCCoroutine's `SuspendingPlugin.scope`. The returned
`Job` is the only handle you get.

It **guarantees nothing about overlapping sends.** There is no per-audience registry: a second call
for the same audience starts a second job, both write to the same single action bar slot, and the
loser is whichever ticked first. Worse, a job that finishes normally with `fadeOut = false` sends
`Component.empty()` - clearing the *other* job's action bar too. If a feature can send twice, keep
the returned `Job` and `cancel()` it before starting the next one.

What it does guarantee: positive `duration` and `interval` (`require`); a throwing `computation` is
logged at most every 900 ms and that tick is skipped, the loop continues; with
`autoCancelPlayer = true` (the default) a player audience going offline ends the job as
`AUTO_CANCELLED`; `onFinish` runs exactly once with an `ActionBarFinishReason` of `COMPLETED`,
`AUTO_CANCELLED`, `CANCELLED` or `FAILED`; a cancelled job does **not** clear the bar.

For a single frame use `Audience.sendActionBar { }` from `messages/adventure/` - no scope, no
repetition. `references/actionbar-and-sidebar.md` has the full contract plus the sidebar and
scoreboard consumers of this layer.

## Small caps, glyphs and fonts

- `CharSequence.toSmallCaps()` in `surf-api-common-api`'s `font/` maps every letter to the Unicode small-caps block via
  `lowercaseChar()` - so uppercase is folded in too and capitalisation is lost, whatever its KDoc
  claims. Digits, punctuation and non-Latin characters pass through; `s` and `x` have no small-caps
  code point and stay as themselves, so mixed text looks slightly uneven. This is **not** a font: it
  substitutes characters, works in vanilla clients, and survives serialization. The `<small>`
  MiniMessage tag is the same function, and it drops its children.
- `builder.font(key: Key?)` is Adventure's font passthrough. It only matters with a resource pack: a
  custom font is how the inventory framework draws shift glyphs and row-aligned header text. No
  message in this layer sets a font.
- `DefaultFontInfo` (in `messages/`) is the chat-side width table - `pixelWidth(text)` sums
  `length + 1` per character over a hand-written 5px table. The pagination footer uses it to centre
  the navigation row. It is approximate and ASCII-only.
- `DefaultFontWidths`, `TextAlignmentMath` and `ShiftGlyphs` under
  `inventory/framework/internal/` are a *different*, measured table for inventory headers and are
  `@InternalSurfApi`. Do not use them for chat, and do not use `DefaultFontInfo` for an inventory
  header - the `surf-api-inventory` skill owns that side.

## Rarity

`core/rarity/Rarity` is presentation only: six entries (`COMMON` … `MYTHIC`), each a German
`displayName` already coloured, and the enum implements `ComponentLike`, so
`buildText { append(rarity) }` works and `rarity.color` is there when you need the raw colour. It
carries no weight and does not implement `Weighted` - it is unused in the repository today, and the
`Rarity` enums in the `RandomSelector` and `Weighted` doc comments are unrelated examples.

## Traps

| Trap | What actually happens |
| --- | --- |
| `error("...")` inside a builder block | resolves to the ERROR-coloured text helper, **not** `kotlin.error`. It appends text and returns normally. Use `throw IllegalStateException(...)` to fail inside a builder |
| building text off the main thread | safe - Adventure components are immutable and the builder is local. **Reading game state** while building is not. `SuspendPagination` renders on `Dispatchers.Default`, sidebar renderers run on a background dispatcher: switch with `withContext(plugin.entityDispatcher(viewer))` before touching the world |
| `miniMessage.deserialize(playerInput)` | gives that player `<click:run_command>` and `<hover>`. Permission-gate it or pass the text as a `Placeholder.component` |
| the wrong MiniMessage instance | `MiniMessage.miniMessage()` has no `<primary>`, `<prefix>` or `<small>`; `dev.slne.surf.api.core.minimessage.miniMessage` has them |
| platform-specific serialization | `GsonComponentSerializer.gson()` (used by `ComponentMessage` and the kotlinx `AdventureComponentSerializer`) is portable. `Component.toNms()` is `PaperAdventure.asVanilla` and only exists inside a Paper NMS version module - see `surf-api-nms`. `PlainTextComponentSerializer` via `Component.plain()` loses all styling, so never round-trip through it |
| `joinToComponentNewLine` / `Map.joinToComponent` | prepend a newline **and** the line prefix before the *first* entry too, so the result starts on its own line |
| `appendTime` / `CommonComponents.formatTime` | German words, and omits any unit whose value is 0 - a 2-hour duration renders `2 Stunden`, not `0 Tage : 2 Stunden` |
| `ComponentMessage.getString()` | a Brigadier `Message` carrier: a per-JVM random UUID prefix plus Gson. Never parse or store it |

## References

Read on demand, not up front:

- `references/component-builder.md` - the whole `SurfComponentBuilder` surface, `CommonComponents`,
  collection/map/time formatting, click callbacks with options, the Adventure DSLs, audience
  extensions, `SurfMessageBundle` and the kotlinx serializers.
- `references/pagination.md` - every builder property with its default, the renderer and
  click-provider interfaces, how to override one part of the frame, and the suspend variant in full.
- `references/minimessage-and-fonts.md` - the tag table with exact names, writing a new tag,
  untrusted input, small caps, and the two pixel-width tables.
- `references/actionbar-and-sidebar.md` - `ActionBarService` contract and finish reasons, and how the
  Paper sidebar and the deprecated scoreboard consume this layer.
