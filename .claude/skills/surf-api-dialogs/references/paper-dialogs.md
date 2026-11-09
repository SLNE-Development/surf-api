# The Paper dialog package

`platform/paper/api/src/main/kotlin/dev/slne/surf/api/paper/dialog/`. Every file starts with
`@file:Suppress("UnstableApiUsage")` because the whole Paper dialog API is marked unstable.

| File | Holds |
| --- | --- |
| `DialogBuilder.kt` | `dialog { }`, `Dialog { }`, `DialogRegistryEntry.Builder.base { }`, `.type { }`, `Player.clearDialogs(…)` |
| `CommonDialogs.kt` | `noticeDialog { }`, `noticeDialog(title, notice, width)`, `noticeDialogWithBuilder(title, width) { }` |
| `builder/DialogBaseBuilder.kt` | `DialogBaseBuilder`, `dialogBase { }`, `DialogBase { }` |
| `builder/DialogBodyBuilder.kt` | `DialogBodyBuilder` + `PlainDialogMessageBuilder`, `ItemDialogMessageBuilder`; `dialogBody { }`, `DialogBody { }` |
| `builder/DialogInputBuilder.kt` | `DialogInputBuilder` + four nested input builders; `dialogInput { }`, `DialogInput { }` |
| `builder/DialogTypeBuilder.kt` | `DialogTypeBuilder` + four nested type builders; `dialogType { }`, `DialogType { }` |
| `builder/DialogActionBuilder.kt` | `DialogActionBuilder`; `dialogAction { }`, `DialogAction { }` |
| `builder/DialogActionButtonBuilder.kt` | `DialogActionButtonBuilder`; `actionButton { }`, `ActionButton { }` |
| `composition/DialogScope.kt`, `composition/DialogStore.kt`, `composition/dsl.kt` | the re-rendering state model |
| `state/DialogState.kt` | `interface DialogState` — an empty marker |
| `query/queries.kt`, `query/results.kt`, `query/states.kt` | `DialogQuery`, `CursorDialogQuery`, `PageResult`, `CursorResult`, `PageState`, `CursorState` |
| `pagination/PaginatedDialog.kt` | `paginatedDialog(…)` |
| `search/SearchDialog.kt` | `searchDialog(…)`, `SearchDialogAction`, `SearchInput` |

Every builder exposes two top-level factories with the same body — a lowercase one (`dialogBase`)
and a capitalised one (`DialogBase`). Both exist on Paper; Minestom only kept the lowercase forms.

## Entry points

```kotlin
fun dialog(builder: DialogRegistryEntry.Builder.() -> Unit): Dialog = Dialog.create { it.empty().apply(builder) }
fun Dialog(builder: DialogRegistryEntry.Builder.() -> Unit): Dialog   // identical

fun DialogRegistryEntry.Builder.base(block: DialogBaseBuilder.() -> Unit)
fun DialogRegistryEntry.Builder.type(block: DialogTypeBuilder.() -> Unit)

@NmsUseWithCaution
fun Player.clearDialogs(showEmptyDialogBefore: Boolean = false)   // SurfPaperNmsCommonBridge
```

`noticeDialog(title: Component, notice: Component, width: @Range(1..1024) Int? = null)` builds the
base and `type { notice() }` in one call; `noticeDialogWithBuilder(title, width) { … }` takes a
`SurfComponentBuilder` block for the notice text instead of a `Component`.

## `DialogBaseBuilder`

Public mutable fields: `title`, `externalTitle`, `canCloseWithEscape`, `afterAction`
(`DialogBase.DialogAfterAction`). Functions: `title(Component)`, `title { }`,
`externalTitle(Component)`, `externalTitle { }`, `preventClosingWithEscape(prevent: Boolean = true)`
(sets `canCloseWithEscape = !prevent`), `afterAction(…)`, `body { }`, `input { }`. Repeated `body`
or `input` blocks append.

`build()` requires a non-null `title`, then calls `DialogBase.builder(title).pause(false)` — **pause
is hard-coded to false and cannot be changed from here** — and applies only the fields that were
set. There is no `canCloseWithEscape(Boolean)` function; assign the field or use
`preventClosingWithEscape`.

## `DialogBodyBuilder`

- `plain { }` → `PlainDialogMessageBuilder` (`message`, `width`; `message` required)
- `plainMessage(message: Component, width: Int? = null)`
- `plainMessage(plain: PlainMessageDialogBody)`
- `plainMessage(width: Int? = null) { }` — `SurfComponentBuilder` block
- `item { }` → `ItemDialogMessageBuilder`

`ItemDialogMessageBuilder` fields are all nullable: `item: ItemStack?`,
`description: PlainMessageDialogBody?`, `showDecorations`, `showTooltip`, `width`/`height`
(`@Range(1..256)`). Functions `item(ItemStack)`, `item(type: ItemType, amount: Int = 1) { }` (a
Bukkit `ItemStack` block), `description(PlainMessageDialogBody)`, `description { }`,
`simpleDescription(message, width)`, `simpleDescription(width) { }`, plus the setters. `item` is
required.

## `DialogInputBuilder`

| Function | Nested builder | Required | Optional fields |
| --- | --- | --- | --- |
| `text(key) { }` | `TextDialogInput` | `label` | `width`, `labelVisible`, `initial`, `maxLength`, `multiline(MultilineOptions)` / `multiline(maxLines, height)` |
| `simpleText(key, label)` / `simpleText(key) { }` | – | – | – |
| `boolean(key) { }` | `BooleanDialogInput` | `label` | `initial`, `onTrue`, `onFalse` |
| `simpleBoolean(key, label, defaultValue)` / `simpleBoolean(key, defaultValue) { }` | – | – | – |
| `numberRange(key, range) { }` | `NumberRangeDialogInput` | `label` | `initial`, `width`, `labelFormat`, `step` |
| `simpleNumberRange(key, range, label)` / `simpleNumberRange(key, range) { }` | – | – | – |
| `singleOption(key) { }` | `SingleOptionDialogInput` | `label` | `width`, `labelVisible`; `option(OptionEntry)`, `option(key, label, selected = false)`, `option(key, selected = false) { }` |
| `addInput(input: DialogInput)` | – | – | – |

`numberRange`/`simpleNumberRange` are generic over `N : Number, N : Comparable<N>` and convert the
`ClosedRange` bounds with `toFloat()`. `text`, `boolean`, `numberRange` and `singleOption` return the
built `DialogInput` as well as adding it.

## `DialogTypeBuilder`

- `notice()`, `notice(button: ActionButton)`, `notice { }`
- `dialogList(vararg keys: TypedKey<Dialog>) { }`, `dialogList(vararg dialogs: Dialog) { }`,
  `dialogList { }` → `DialogListTypeBuilder` (`exitAction`, `columns`, `buttonWidth`, `dialog(Dialog)`,
  `dialog(TypedKey<Dialog>)`, `dialog(vararg TypedKey<Dialog>)`, `addAll(…)`). Keys are resolved
  eagerly through `RegistryAccess.registryAccess().getRegistry(RegistryKey.DIALOG).getOrThrow(key)`,
  so a missing key throws at build time. The dialogs are collected in a `mutableSetOf` and wrapped
  in `RegistrySet.valueSet(RegistryKey.DIALOG, dialogs)`.
- `confirmation { }` → `DialogConfirmationTypeBuilder` (`yes`, `no`, both required),
  `confirmation(yes: ActionButton, no: ActionButton)`
- `multiAction { }` / `multiAction(actions: List<ActionButton>) { }` / `multiAction(vararg actions) { }`
  → `DialogMultiActionTypeBuilder` (`action(…)`, `exitAction`, `columns`). An empty action list is
  **not** rejected here.
- `serverLinks { }` → `ServerLinksTypeBuilder` with non-null defaults `columns = 2`,
  `buttonWidth = 150`, nullable `exitAction`; builds `DialogType.serverLinks(exitAction, columns, buttonWidth)`.

`build()` requires that one of them ran: *"Dialog type must be built"*.

## `DialogActionButtonBuilder`

Nullable `label` (required), `tooltip`, `width` (`@Range(1..1024)`), `action`. `action(DialogAction)`
or `action { }`. Builds `ActionButton.builder(label)` and applies only what was set.

## `DialogActionBuilder`

Every function assigns the single `action` field; the last call wins. `build()` requires one.

| Function | Produces |
| --- | --- |
| `staticAction(clickEvent: ClickEvent<*>)` | `DialogAction.staticAction` |
| `showDialog(dialog: DialogLike)` / `showDialog { }` | `ClickEvent.showDialog` |
| `copyToClipboard(text)`, `openUrl(String)`, `openUrl(URL)`, `suggestCommand(command)` | the matching `ClickEvent` |
| `callback(callback: ClickCallback<Audience>)` | `ClickEvent.callback` |
| `playerCallback(callback: ClickCallback<Player>)` | `ClickCallback.widen(…, Player::class.java)`; a non-player audience gets *"Only players can use this action!"* |
| `commandTemplate(template)` | `DialogAction.commandTemplate` |
| `customClick(id: Key, additions: BinaryTagHolder? = null)` | `DialogAction.customClick` |
| `customClick(id: Key) { /* CompoundBinaryTag.Builder */ }` | serialises the tag with `TagStringIO` |
| `customClick(options: ClickCallback.Options? = null, callback: DialogActionCallback)` | server-side callback; `options` defaults to `ClickCallback.Options.builder().build()` |
| `customPlayerClick(options: ClickCallback.Options? = null) { response: DialogResponseView, player: Player -> }` | same, player-filtered with the same error message |

`DialogResponseView` here is **Paper's** `io.papermc.paper.dialog.DialogResponseView`. The repository
reads it with `response.getText(key)` only (`PaginatedDialog.kt`, `SearchDialog.kt`).

## Composition: state that re-renders

```kotlin
suspend fun <S : DialogState> composableDialog(
    player: Player,
    initialState: S,
    scope: CoroutineScope,
    content: suspend DialogScope<S>.() -> Dialog,
)
```

`composition/dsl.kt` builds a `DialogStore(initialState, player.uniqueId, renderer = content, scope)`
and calls `open()`, which renders once and shows the dialog.

`DialogStore<S : DialogState>` keeps `currentState`, a `mounted` flag and a
`ConcurrentHashMap<List<Any?>, Any?>` memo. `update { transform }` applies the transform and
re-renders; `rerender()` builds a fresh `DialogScope(this, scope)`, runs the renderer and calls
`server.getPlayer(playerUuid)?.showDialog(dialog)` — an offline player is silently skipped.

`DialogScope<S>` is what a renderer sees:

- `state(): S`
- `suspend setState(transform: S.() -> S)` — delegates to `store.update`, so it re-renders
- `suspend <T> remember(vararg keys: Any?, block: suspend CoroutineScope.() -> T): T` — keyed by
  `keys.toList()`, cached in the store's map

Caveats worth knowing before you build on this: `mounted` is only ever `true` (nothing unmounts a
store), the memo is never evicted, and `remember` keys are only compared for equality — a key list
that does not change will never recompute. `DialogState` itself is an empty marker interface.

## Query types

```kotlin
fun interface DialogQuery<S : DialogState, T>       { suspend fun execute(state: S): PageResult<T> }
fun interface CursorDialogQuery<S : DialogState, T> { suspend fun execute(state: S): CursorResult<T> }

data class PageResult<T>(val items: List<T>, val page: Int, val totalPages: Int, val totalItems: Int? = null)
data class CursorResult<T>(val items: List<T>, val nextCursor: String?, val hasMore: Boolean)

data class PageState(val page: Int = 1, val limit: Int = 10, val search: String? = null) : DialogState
data class CursorState(val cursor: String? = null, val history: List<String?> = emptyList(), val limit: Int = 10, val search: String? = null) : DialogState
```

`PageState` validates in its `init` block: `page >= 1` and `limit > 0`. `CursorState` validates
nothing. The three cursor types have no caller in the repository — only the page types are used, by
`paginatedDialog`.
