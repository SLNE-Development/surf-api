# The component builder in full

`messages/builder/SurfComponentBuilder.kt`, `messages/builder/colors/`, `messages/CommonComponents.kt`,
`messages/adventure/`. "Component" here always means Adventure's text `Component`.

## Entry points

| Call | Returns |
| --- | --- |
| `buildText { }` | `TextComponent` - the one you want |
| `SurfComponentBuilder { }` | `TextComponent`, identical (`buildText` just forwards) |
| `SurfComponentBuilder()` / `SurfComponentBuilder.builder()` | the mutable `SurfComponentBuilder` |
| `audience.sendText { }` | `Unit`, builds and sends |
| `audience.sendActionBar { }` | `Unit`, one-shot action bar (no repetition) |

`SurfComponentBuilder` is `@ApiStatus.NonExtendable` and implements `ComponentLike`, so it can be
passed anywhere a component is expected without calling `build()`. The implementation
(`SurfComponentBuilderImpl`) is `internal` and delegates every call to a wrapped
`TextComponent.Builder`; `build()` is `delegate.asComponent() as TextComponent`.

## Appending

```kotlin
append(component: Component)                 // and ComponentLike, Iterable<ComponentLike>,
append(builder: ComponentBuilder<*, *>)      // vararg Component, vararg ComponentLike
append(block: SurfComponentBuilder.() -> Unit)          // nested builder
suspend appendAsync(block: suspend SurfComponentBuilder.() -> Unit)
appendNewline()
appendNewline(amount: Int)
appendNewline(block: SurfComponentBuilder.() -> Unit)   // newline, then the block
suspend appendNewlineAsync(block: suspend SurfComponentBuilder.() -> Unit)
appendSpace()
```

`appendAsync` / `appendNewlineAsync` exist so a suspending renderer can nest a builder block that
itself suspends. In a plain `buildText { }` the lambda is `inline` and not `crossinline`, so suspend
calls inside it already compile from a suspending caller - the `Async` variants matter when the block
is stored or passed on.

## Text and colour

```kotlin
text(text: String,    color: TextColor? = null, vararg decoration: TextDecoration)
text(boolean: Boolean, color: TextColor? = null, vararg decoration: TextDecoration)
text(char: Char,       color: TextColor? = null, vararg decoration: TextDecoration)
text(number: Number,   color: TextColor? = null, vararg decoration: TextDecoration)
appendText(text: String, color: TextColor? = null)
appendText(text: String, color: TextColor? = null, block: TextComponent.Builder.() -> Unit)
note(any: Any, vararg decoration: TextDecoration)        // NOTE colour, via toString()
ellipsis(color: TextColor? = SPACER)                     // "..."
translatable(key: String, color: TextColor? = Colors.WHITE, vararg decoration: TextDecoration)
```

`text(number)` dispatches on the runtime type: `Double`, `Float`, `Int` and `Long` go to the matching
Adventure overload, anything else through `toString()`.

The colour vocabulary comes from `ComponentBuilderColors`, which `SurfComponentBuilder` extends. Each
name has the same four overloads (`String`, `Boolean`, `Char`, `Number`) plus
`vararg decoration: TextDecoration`:

- Surf: `primary`, `secondary`, `info`, `note`, `success`, `warning`, `error`, `variableKey`,
  `variableValue`, `spacer`, `darkSpacer`
- Minecraft (`MinecraftComponentBuilderColors`): `black`, `darkBlue`, `darkGreen`, `darkAqua`,
  `darkRed`, `darkPurple`, `gold`, `gray`, `darkGray`, `blue`, `green`, `aqua`, `red`,
  `lightPurple`, `yellow`, `white`

They are member extension functions on `SurfComponentBuilder`, declared inside the colour
interfaces, so they resolve only where the builder is the implicit receiver. `error` therefore
shadows `kotlin.error` inside a builder block.

Prefix helpers, one family per status colour (`info`, `success`, `warning`, `error`):

```kotlin
appendInfoPrefix()
appendNewInfoPrefixedLine(amount: Int = 1)
appendNewInfoPrefixedLine(block: SurfComponentBuilder.() -> Unit)
suspend appendNewInfoPrefixedLineAsync(block: suspend SurfComponentBuilder.() -> Unit)
```

## Style

The Adventure builder surface is mirrored one-for-one and returns `SurfComponentBuilder`:
`color`, `colorIfAbsent`, `decorate(decoration)`, `decorate(vararg)`, `decoration(decoration, flag)`,
`decoration(decoration, state)`, `decorationIfAbsent`, `decorations(map)`, `decorations(set, flag)`,
`font(Key?)`, `hoverEvent(HoverEventSource<*>?)`, `insertion(String?)`, `mergeStyle` (three
overloads), `resetStyle`, `style(Style)`, `style(Consumer<Style.Builder>)`, `shadowColor(ARGBLike?)`,
`shadowColorIfAbsent`, `mapChildren`, `mapChildrenDeep`, `applicableApply`, `apply`, `applyDeep`,
`content()`, `content(String)`, `children()`.

Style applies to the builder, i.e. to the component being built - not retroactively to children that
already carry their own colour. That is why `renderHeader` in the pagination renderer does
`append(title.colorIfAbsent(Colors.PRIMARY))`.

## Clicks and callbacks

```kotlin
clickOpensUrl(url: String)
clickRunsCommand(command: String)
clickSuggestsCommand(command: String)
clickCopiesToClipboard(value: String)
clickEvent(event: ClickEvent<*>?)
clickCallback(callback: ClickCallback<Audience>)
clickCallbackWithOptions { }
```

The same four `click*` helpers also exist as extensions on any Adventure
`ComponentBuilder<C, B>` in `messages/adventure/component-extension.kt`.

For a typed or permission-gated callback (`messages/adventure/callback-extension.kt`):

```kotlin
builder.clickCallbackTyped<Player> { player -> /* ... */ }

builder.clickCallbackTypedWithOptions<Player> {
    requiresPermissionOrElse("surf.admin") { audience ->
        audience.sendText { appendErrorPrefix(); error("Keine Rechte.") }
    }
    callback { player -> /* ... */ }
    options {
        uses(1)
        lifetime(5.minutes)   // Kotlin Duration overload added by this file
    }
}
```

`ClickCallbackWithOptionsBuilder` builds nothing if `callback` was never set - it returns an empty
callback. `requiresPermission(p)` without an else branch silently does nothing on failure.

## `CommonComponents`

Constants: `ELLIPSIS` (`...`), `MAP_SEPARATOR` (` -> `, SPACER), `TIME_SEPARATOR` (` : `, SPACER),
`EM_DASH` (`—`, SPACER), `DISCORD_LINK` (clickable `discord.gg/castcrafter`), `DISCONNECT_HEADER`,
`RETRY_LATER_FOOTER`, `ISSUE_FOOTER`. All German, all `@JvmField`.

Disconnect and kick screens - `renderKickDisconnectMessage` and `renderDisconnectMessage` have
overloads for a plain `TextComponent.Builder`, for a `SurfComponentBuilder`, and with an
`issue: Boolean` that picks the footer. From inside a builder:

```kotlin
buildText {
    appendKickDisconnectMessage(
        messageRenderer = { error("Du wurdest gekickt.") },
        footerRenderer = { appendDisconnectFooterTryAgainLater(issue = false) },
    )
}
```

Collections, maps and durations - as builder calls or as free extensions:

```kotlin
// builder
appendCollection(collection) { text(it) }
appendCollectionNewLine(collection, linePrefix = Colors.PREFIX) { text(it) }
appendMap(map, keyFormatter = { text(it) }, valueFormatter = { text(it) },
          linePrefix = Colors.PREFIX, keyValueSeparator = CommonComponents.MAP_SEPARATOR)
appendTime(duration, showSeconds = true, shortForms = false,
           separator = CommonComponents.TIME_SEPARATOR, timeColor = Colors.VARIABLE_VALUE)

// extensions
iterable.joinToComponent { text(it) }                        // ", " separated, SPACER comma
iterable.joinToComponentNewLine(linePrefix) { text(it) }     // one per line, "— " bullet
map.joinToComponent(keyFormatter, valueFormatter)            // one per line, key -> value
```

Both newline forms emit the separator *before the first element as well*, so the output opens with a
newline, the prefix and `— `. `joinToComponent` on a map applies `colorIfAbsent(VARIABLE_KEY)` to
every key and `colorIfAbsent(VARIABLE_VALUE)` to every value.

`formatTime` walks centuries, decades, years, days, hours, minutes and seconds, skipping every unit
whose value is 0, and writes German singular/plural (`1 Tag` / `2 Tage`) or short forms
(`Jh`, `Jz`, `J`, `d`, `h`, `m`, `s`) when `shortForms = true`.

## Free functions and Audience extensions

`messages/adventure/component-extension.kt`: `text(content/number/boolean/char/any, color = null)`
as top-level constructors, and `Component.plain()` (`PlainTextComponentSerializer`, loses all
styling - measurement and logging only).

`messages/adventure/audience-extension.kt`: `sendText { }`, `sendActionBar { }`, `openBook { }`,
`showBossBar { }`, `playSound { }`, `playSound(useSelfEmitter) { }`, `showTitle { }`, plus
`uuidOrNull()` / `uuid()`, `nameOrNull()` / `name()`, `displayNameOrNull()` / `displayName()`,
`testPermission(permission): TriState` and `hasPermission(permission): Boolean`. The non-`OrNull`
forms throw via `error(...)` when the pointer is absent, so prefer the nullable ones for arbitrary
audiences (the console has no UUID).

`messages/adventure/pointered-extensions.kt` replaces Adventure's `Optional`-returning
`Pointered.get` with a nullable `getPointer(pointer): T?`.

Other DSL builders in the same package, each with its own `@DslMarker`:

| Function | Builds |
| --- | --- |
| `title { title { } ; subtitle { } ; times { fadeIn(20); stay(100); fadeOut(40) } }` | `Title`. Times take ticks (`Long`), a `java.time.Duration` or a Kotlin `Duration` |
| `book { title { }; author { }; page { } }` | `Book` |
| `bossBar { }` | `BossBar` |
| `sound { }` | `Sound` |
| `resourcePackInfo { }`, `resourcePackRequest { pack { }; prompt { } }`, `addingResourcePackRequest(vararg infos)` | resource pack requests |
| `key(string)`, `key(namespace, value)`, `String.toKey()`, `Key.asMinimal()` | `Key` |
| `pointer<V>(key)`, `identity(uuid)` | pointers and identities |

`Title(block)`, `Book(block)`, `BossBar(block)` and `Sound(block)` are capitalised aliases for the
lowercase functions.

## `ComponentMessage`

`ComponentMessage(componentLike)` adapts a component to Brigadier's `Message`. `getString()` returns
a per-JVM random `[ComponentMessage <uuid>] ` prefix followed by Gson JSON; the platform command
layer recognises the prefix and recovers the component. It is a transport hack, not a format - never
log, store or parse it.

## Translations

`SurfMessageBundle(bundleClazz, pathToBundle, dataFolder, classLoader = bundleClazz.classLoader)`
copies the bundled `.properties` files into the data folder, fills in keys the operator's copy is
missing, and registers everything with Adventure's `GlobalTranslator`:

```kotlin
object MyMessages {
    private const val BUNDLE = "messages.MyBundle"
    private val bundle = SurfMessageBundle(javaClass, BUNDLE, plugin.dataPath).apply { load() }

    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Component) =
        bundle.getMessage(key, *params)
}
```

`getMessage(key, vararg params)` is just `Component.translatable(key, *params)`; `get` is the same as
an operator and `lazyMessage` wraps it in a `Supplier`. The translation happens client-side or in
`GlobalTranslator`, so the bundle must be loaded before any component reaches a player.

`pathToBundle` is annotated `@BundlePath` (pattern `^[a-zA-Z0-9_]+(\.[a-zA-Z0-9_]+)*$`) - dots, not
slashes. `@NoLowercase` in the same file marks parameters that must start with a capital, used by
`appendDisconnectMessage`.

**Known bug:** `load()` calls
`copyMissingBundles(dataFolderResourceBundles, dataFolderResourceBundles)` - it passes the data
folder list where the bundled list belongs, so bundles that only exist in the jar are never copied
out. Do not build on that call until it is fixed.

## Storage

`core/serializer/adventure/` holds kotlinx-serialization serializers for `Component`, `TextColor`,
`TextDecoration`, `ShadowColor`, `Key`, `Book`, `BossBar`, `Sound`, `Sound.Stop`, `Title` and
`ResourcePackInfo`. `AdventureComponentSerializer` encodes
`GsonComponentSerializer.gson().serialize(value.compact())` - version-independent JSON, the right
choice for a database or a config. The config layer's own `Component` serializer
(`SpongeConfigSerializers`) stores MiniMessage strings through the Surf instance instead, so a
component written by the config layer must be readable as MiniMessage.
