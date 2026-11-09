# MiniMessage, small caps and pixel widths

`minimessage/SurfMiniMessageHolder.kt`, `font/small-caps.kt`, `messages/DefaultFontInfo.kt`.

## The configured instance

```kotlin
import dev.slne.surf.api.core.minimessage.miniMessage

val component = miniMessage.deserialize("<prefix:success><success>Fertig!")
val string = miniMessage.serialize(component)
```

`miniMessage` is a top-level `val` delegating to `SurfMiniMessageHolder.miniMessage()`, which holds
one `MiniMessage` built once in an `object` initialiser. It is built with `MiniMessage.builder()` and
`editTags { }`, so the **entire standard tag set stays available** - colours, `<bold>`, `<gradient>`,
`<rainbow>`, `<click>`, `<hover>`, `<insert>`, `<lang>`, `<newline>`, everything - and these are
added on top:

| Tag | Kind | Effect |
| --- | --- | --- |
| `<primary>` | styling | `Colors.PRIMARY` |
| `<secondary>` | styling | `Colors.SECONDARY` |
| `<info>` | styling | `Colors.INFO` |
| `<note>` | styling | `Colors.NOTE` |
| `<success>` | styling | `Colors.SUCCESS` |
| `<warning>` | styling | `Colors.WARNING` |
| `<error>` | styling | `Colors.ERROR` |
| `<variable_key>` | styling | `Colors.VARIABLE_KEY` |
| `<variable_value>` | styling | `Colors.VARIABLE_VALUE` |
| `<spacer>` | styling | `Colors.SPACER` (GRAY) |
| `<dark_spacer>` | styling | `Colors.DARK_SPACER` (DARK_GRAY) |
| `<prefix_color>` | styling | `Colors.PREFIX_COLOR` (= PRIMARY) |
| `<prefix>` | self-closing inserting | `Colors.PREFIX`. With an argument: `info`, `success`, `warning`, `error` insert the matching prefix. Anything else throws `context.newException("Unknown prefix type: …")` |
| `<small>` | modifying | runs `toSmallCaps()` over the content |

The holder's own KDoc lists `<note>` only in the code, not in the doc block - the tag is there.

### `<small>` drops children

`SmallCapsTag.apply` returns `current.content(current.content().toSmallCaps()).children(emptyList())`
for a `TextComponent`, and `current.children(emptyList())` for anything else. So
`<small>abc<red>def</red></small>` loses `def` entirely, and `<small>` around a non-text component
empties it. Keep `<small>` around plain text only.

### Adding a tag

Everything lives in the `editTags { }` block of `SurfMiniMessageHolder`. A colour tag is one map
entry; anything else is a `tagBuilder.tag(name) { argumentQueue, context -> … }` returning a `Tag`
(`Tag.styling`, `Tag.selfClosingInserting`, `Tag.inserting`, or a `Modifying` implementation).
Read arguments with `queue.peek()?.lowerValue()` or `queue.popOr("message")`, and reject bad input
with `throw context.newException(message, queue)` so MiniMessage reports the position. There is no
per-call resolver plumbing in this repository - a new tag is global to the instance.

## Untrusted input

`deserialize` on a player-authored string hands that player every tag, including
`<click:run_command:/op them>` and `<hover:show_text:…>`. The clients of this instance today:

- `SpongeConfigSerializers`' `Component` serializer - operator-authored, trusted.
- `platform/paper/api/.../command/args/MiniMessageArgument.kt` and the Minestom equivalent - parse
  the raw argument and turn a `ParsingException` into a command error with
  `e.detailMessage()`, `e.originalText()` and `e.endIndex()`. **These parse player input.** Only
  register a `miniMessageArgument` behind a permission - see the `surf-api-commands` skill for how
  to declare one.
- `platform/paper/api/.../util/bukkit-util.kt` builds a player's display name by deserializing their
  LuckPerms prefix concatenated with their name - operator-configured prefix, but the name is
  concatenated into the same string.

Safe patterns, in order of preference:

1. Build with `buildText { }` and never go through MiniMessage.
2. Keep the template in MiniMessage and inject the untrusted part as a component:
   `miniMessage.deserialize(template, Placeholder.component("name", Component.text(raw)))`.
3. If a string placeholder is unavoidable, use `Placeholder.unparsed`, never `Placeholder.parsed`.

## The other instance

`MiniMessage.miniMessage()` - the vanilla singleton - has none of the Surf tags. It is used by the
gradient-frame helpers in `platform/paper/impl/.../scoreboard/SurfScoreboardBuilderImpl.kt` and
`platform/paper/impl/.../sidebar/SidebarBuilderImpl.kt`, which build
`"<gradient:$firstHex:$secondHex:$phase><text></gradient>"` with a `Placeholder.component("text", …)`,
and by `platform/velocity/api/.../command/args/MiniMessageArgument.kt`. Those three are the known
inconsistency: everything else in the repository goes through the Surf instance. When touching them,
switch to `dev.slne.surf.api.core.minimessage.miniMessage` unless there is a reason not to - the
gradient tag is standard, so the Surf instance parses those strings identically.

## Small caps

```kotlin
import dev.slne.surf.api.common.font.toSmallCaps

"Server neu gestartet".toSmallCaps()   // "sᴇʀᴠᴇʀ ɴᴇᴜ ɢᴇsᴛᴀʀᴛᴇᴛ"
```

`CharSequence.toSmallCaps(): String` is `smallCapsMap[c.lowercaseChar()] ?: c` per character.
Consequences worth knowing:

- **Its KDoc is wrong.** It claims uppercase letters are left unchanged; the lookup goes through
  `lowercaseChar()`, so `A` becomes `ᴀ` and capitalisation is lost. Capitalise by leaving the first
  letter out of the call if you need it.
- `s` and `x` have no small-caps code point in Unicode and map to themselves, so they render at full
  lowercase height next to their neighbours. `q` uses `ǫ` and `f` uses `ғ`, which are also
  approximations.
- Digits map to themselves; everything outside `a-z0-9` is untouched.
- The result is ordinary text: no resource pack, no font key, and it survives every serializer.

## When the font key matters

`builder.font(key: Key?)` and `Component.font(key)` select a font from the client's resource packs.
Nothing in the message layer sets one. The inventory framework does, heavily: shift glyphs
(`ShiftGlyphs`, powers-of-two advances decomposed greedily) and the per-row header fonts
(`surf:menu_font_row_1` … `_row_6`, each declaring an `ascent` that centres its glyphs in one slot
row) only render correctly with the Surf resource pack and with the font set. All of that is
`@InternalSurfApi` under `inventory/framework/internal/` and belongs to the `surf-api-inventory` skill.

## The two width tables

There are two, measured differently, for different surfaces. Using the wrong one produces text that
is off by a few pixels per glyph.

**`messages/DefaultFontInfo`** - chat. An enum of `(character, length)` pairs covering ASCII letters,
digits and punctuation, adapted from a SpigotMC forum post. `pixelWidth(text)` sums `length + 1` per
character, `getDefaultFontInfo(c)` is a linear scan over `entries` falling back to `DEFAULT` (4), and
`getBoldLength()` adds one except for `SPACE`. Non-ASCII characters all measure as `DEFAULT`, and
`pixelWidth` ignores bold. The pagination footer is its only user.

**`inventory/framework/internal/DefaultFontWidths`** - inventory headers. `@InternalSurfApi`.
Advances measured off the three vanilla font sheets (`ascii.png`, `accented.png`,
`nonlatin_european.png`), so Latin, Latin Extended, Greek and Cyrillic are exact;
`DEFAULT_ADVANCE = 6`, `SPACE_ADVANCE = 4`, `BOLD_EXTRA_ADVANCE = 1`, and anything off those sheets
(CJK, custom glyphs) measures as `DEFAULT_ADVANCE` unless a `charWidths` override map is passed.
`TextAlignmentMath` holds the alignment arithmetic on top of it.

They agree where it is easy to check - a normal glyph is 5 + 1 = 6 in one and `DEFAULT_ADVANCE` 6 in
the other, a space 3 + 1 = 4 and `SPACE_ADVANCE` 4 - and disagree on the fallback for an unlisted
character: 4 + 1 = 5 versus 6. For new chat-side measuring, prefer the measured numbers in
`DefaultFontWidths` and say why; do not quietly swap the table the pagination footer uses, because
the rendered width of every existing frame depends on it.
