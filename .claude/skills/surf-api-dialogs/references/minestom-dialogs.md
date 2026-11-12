# The Minestom dialog package

`platform/minestom/api/src/main/kotlin/dev/slne/surf/api/minestom/dialog/`. Builds
`net.minestom.server.dialog.Dialog` records directly — no registry, no `UnstableApiUsage`
suppression. Covered by `platform/minestom/api/src/test/kotlin/.../dialog/DialogBuilderTest.kt`,
which is the best source of compiling examples.

| File | Holds |
| --- | --- |
| `DialogBuilder.kt` | `dialog { }`, `DialogEntryBuilder.base { }`, `.type { }` |
| `CommonDialogs.kt` | `noticeDialog { }`, `noticeDialog(title, notice, width)`, `noticeDialogWithBuilder(title, width) { }` |
| `builder/DialogEntryBuilder.kt` | collects the two halves |
| `builder/DialogBaseBuilder.kt` | `DialogBaseBuilder`, `dialogBase { }` |
| `builder/DialogBodyBuilder.kt` | `DialogBodyBuilder` + `PlainDialogMessageBuilder`, `ItemDialogMessageBuilder`; `dialogBody { }` |
| `builder/DialogInputBuilder.kt` | `DialogInputBuilder` + `InputBuilder` base and four subclasses; `dialogInput { }` |
| `builder/DialogTypeBuilder.kt` | `DialogTypeBuilder` + four nested type builders (no standalone factory) |
| `builder/DialogActionBuilder.kt` | `DialogActionBuilder`; `dialogAction { }` |
| `builder/DialogActionButtonBuilder.kt` | `DialogActionButtonBuilder`; `actionButton { }` |
| `callback/DialogCallbacks.kt` | the server-side callback registry |
| `callback/DialogCallbackOptions.kt` | use count and lifetime |
| `callback/DialogCallbackContext.kt` | `data class (player: Player, payload: BinaryTag?)` |
| `callback/DialogResponseView.kt` | typed reads over the reported values |
| `search/SearchDialog.kt` | `searchDialog(…)`, `SearchDialogAction`, `SearchInput` |

Only the lowercase factories exist (`dialogBase`, `dialogBody`, `dialogInput`, `dialogAction`,
`actionButton`). There is no `dialogType`/`DialogType` factory: a `DialogTypeBuilder` stores a
`(DialogMetadata) -> Dialog` factory, because the Minestom records take the metadata and the type
together, so the type can only be built once the base is known.

## Entry point

```kotlin
fun dialog(block: DialogEntryBuilder.() -> Unit): Dialog = DialogEntryBuilder().apply(block).build()
fun DialogEntryBuilder.base(block: DialogBaseBuilder.() -> Unit)
fun DialogEntryBuilder.type(block: DialogTypeBuilder.() -> Unit)
```

`DialogEntryBuilder.build()` requires both: *"Dialog base must be built"*, *"Dialog type must be
built"*. It then calls `type.build(base.build())`.

`noticeDialog { }` applies the caller's block and then forces `type { notice() }`, so passing a
`type` of your own to it is pointless.

## `DialogBaseBuilder`

Unlike Paper's, the fields have concrete defaults and are always passed to the `DialogMetadata`
constructor:

| Field | Default | Setter |
| --- | --- | --- |
| `title: Component?` | `null`, **required** | `title(Component)`, `title { }` |
| `externalTitle: Component?` | `null` | `externalTitle(Component)`, `externalTitle { }` |
| `canCloseWithEscape: Boolean` | `true` | `canCloseWithEscape(Boolean)`, `preventClosingWithEscape(prevent = true)` |
| `pause: Boolean` | `false` | `pause(Boolean)` — whether a single-player world pauses |
| `afterAction: DialogAfterAction` | `DialogAfterAction.CLOSE` | `afterAction(…)` |
| body | `emptyList()` | `body { }`, appends across calls |
| inputs | `emptyList()` | `input { }`, appends across calls |

`build()` returns `DialogMetadata(title, externalTitle, canCloseWithEscape, pause, afterAction, body, inputs)`.
Both `pause` and `canCloseWithEscape` are settable here, which Paper's builder does not allow.

## `DialogBodyBuilder`

- `plain { }` → `PlainDialogMessageBuilder` (`message` required, `width`)
- `plainMessage(message: Component, width: Int? = null)`
- `plainMessage(plain: DialogBody.PlainMessage)`
- `plainMessage(width: Int? = null) { }`
- `item { }` → `ItemDialogMessageBuilder`

A `null` width becomes `DialogBody.PlainMessage.DEFAULT_WIDTH` (200 in the current Minestom build,
as `DialogBuilderTest` asserts).

`ItemDialogMessageBuilder`: `item: ItemStack?` (required), `description: DialogBody.PlainMessage?`,
`showDecorations = true`, `showTooltip = true`, `width = 16`, `height = 16` (`DEFAULT_ITEM_SIZE`).
`item(ItemStack)` or `item(material: Material, amount: Int = 1)` — a Minestom `Material`, and no
configuration block, where Paper takes an `ItemType` plus an `ItemStack.() -> Unit`.

## `DialogInputBuilder`

All four control builders extend `InputBuilder(key)`, which owns `label` / `label { }` and
`requireLabel()` — a missing label fails with *"Dialog input '<key>' must have a label"*.

| Function | Builder | Fields and defaults |
| --- | --- | --- |
| `input(input: DialogInput)` | – | adds a prebuilt control |
| `boolean(key) { }` | `BooleanInputBuilder` | `initial = false`, `onTrue = "true"`, `onFalse = "false"` |
| `text(key) { }` | `TextInputBuilder` | `width = DialogInput.DEFAULT_WIDTH`, `labelVisible = true`, `initial = ""`, `maxLength = 32`, `multiline(maxLines, height)` |
| `numberRange(key) { }` | `NumberRangeInputBuilder` | `width = DialogInput.DEFAULT_WIDTH`, `labelFormat = "options.generic_value"`, `range(start, end)` **required**, `initial`, `step` |
| `numberRange(key, range: ClosedRange<N>) { }` | same | sets `range` from the Kotlin range, then runs the block |
| `singleOption(key) { }` | `SingleOptionInputBuilder` | `width`, `labelVisible = true`; `option(id, display: Component? = null, initial = false)`, `option(id, initial = false) { }`; at least one option required |

`numberRange` without a range throws *"Dialog input '<key>' must declare a range"*;
`singleOption` without options throws *"Dialog input '<key>' must offer at least one option"*.

## `DialogTypeBuilder`

- `notice()` (uses `Dialog.Notice.DEFAULT_ACTION`), `notice(button)`, `notice { }`
- `dialogList { }` / `dialogList(dialogs: Collection<Dialog>) { }` / `dialogList(vararg dialogs) { }`
  → `DialogListTypeBuilder`: a `mutableSetOf<Dialog>` keeping insertion order, `dialog(Dialog)`,
  `addAll(vararg)`, `addAll(Iterable)`, `exitAction`, `columns = 2`, `buttonWidth = 150`. Builds
  `Dialog.DialogList(metadata, HolderSet.Direct(dialogs.toList()), exitAction, columns, buttonWidth)`.
  There is **no** registry-key overload.
- `serverLinks { }` → `ServerLinksTypeBuilder` (`exitAction`, `columns = 2`, `buttonWidth = 150`)
- `confirmation(yes, no)` and `confirmation { }` → `DialogConfirmationTypeBuilder`, both buttons required
- `multiAction { }` / `multiAction(actions: List<DialogActionButton>) { }` / `multiAction(vararg actions) { }`
  → `DialogMultiActionTypeBuilder` (`action(…)`, `exitAction`, `columns = 2`). **Rejects an empty
  action list**: *"A multi action dialog must offer at least one action"* — Paper's equivalent does not.

`DEFAULT_COLUMNS = 2` and `DEFAULT_BUTTON_WIDTH = 150` are private constants of `DialogTypeBuilder`.

## `DialogActionButtonBuilder`

`label` (required), `tooltip`, `width = DialogActionButton.DEFAULT_WIDTH`, `action`. Builds
`DialogActionButton(label, tooltip, width, action)` — a button with no action is legal, as
`DialogBuilderTest` asserts for a confirmation's `no` button.

## `DialogActionBuilder`

| Function | Produces |
| --- | --- |
| `action(action: DialogAction)` | assigns directly |
| `openUrl(String)` / `openUrl(URL)` | `DialogAction.OpenUrl` |
| `runCommand(command)` | `DialogAction.RunCommand` |
| `suggestCommand(command)` | `DialogAction.SuggestCommand` |
| `copyToClipboard(value)` | `DialogAction.CopyToClipboard` |
| `changePage(page: Int)` | `DialogAction.ChangePage` |
| `showDialog(dialog)` / `showDialog { }` | `DialogAction.ShowDialog` |
| `commandTemplate(template)` | `DialogAction.DynamicRunCommand` — runs the template with the input values substituted |
| `customClick(id: Key, payload: BinaryTag? = null)` | `DialogAction.Custom` |
| `customClick(id: Key) { /* CompoundBinaryTag.Builder */ }` | same, tag built from the block |
| `dynamicCustomClick(id: Key, additions: CompoundBinaryTag? = null)` | `DialogAction.DynamicCustom` — client reports the inputs back |
| `callback(options = DEFAULT) { context: DialogCallbackContext -> }` | registers in `DialogCallbacks`, emits `DialogAction.Custom` |
| `playerCallback(options = DEFAULT) { player: Player -> }` | same, context unwrapped to its player |
| `customClick(options = DEFAULT, additions = null) { response: DialogResponseView, context: DialogCallbackContext -> }` | registers a *dynamic* custom action |
| `customPlayerClick(options = DEFAULT, additions = null) { response: DialogResponseView, player: Player -> }` | same, unwrapped |

Paper has no `runCommand` or `changePage`, and reaches server-side callbacks through Adventure's
`ClickCallback` instead.

## `DialogCallbacks`, end to end

```kotlin
object DialogCallbacks {
    fun register(options: DialogCallbackOptions = DEFAULT, callback: (DialogCallbackContext) -> Unit): Key
    fun action(options: DialogCallbackOptions = DEFAULT, callback: (DialogCallbackContext) -> Unit): DialogAction
    fun dynamicAction(options: DialogCallbackOptions = DEFAULT, additions: CompoundBinaryTag? = null, callback: (DialogCallbackContext) -> Unit): DialogAction
    fun unregister(key: Key): Boolean
    @InternalSurfApi fun dispatch(player: Player, key: Key, payload: BinaryTag?): Boolean
}
```

1. `register` purges expired registrations, mints `Key.key("surf_dialog_callback", UUID.randomUUID().toString())`
   and stores the callback in a `ConcurrentHashMap`.
2. `action` wraps the key in `DialogAction.Custom(key, null)`; `dynamicAction` in
   `DialogAction.DynamicCustom(key, additions)` — only the dynamic form makes the client send the
   input values back.
3. The client press arrives as `PlayerCustomClickEvent`;
   `platform/minestom/impl/src/main/kotlin/.../impl/dialog/DialogCallbackListener.kt` calls
   `DialogCallbacks.dispatch(event.player, event.key, event.payload)`.
4. `dispatch` consumes one use, drops the registration when exhausted or expired and returns `false`
   when nothing matched — a stale button does nothing and reports nothing to the player.

`DialogCallbackOptions(uses: Int = ONE_USE, lifetime: Duration = DEFAULT_LIFETIME)` with
`ONE_USE = 1`, `UNLIMITED_USES = -1`, `DEFAULT_LIFETIME = 12.hours`. The constructor requires
`uses == UNLIMITED_USES || uses > 0` and a positive lifetime. A button that may be pressed repeatedly
needs `DialogCallbackOptions(uses = DialogCallbackOptions.UNLIMITED_USES)` passed explicitly.

## `DialogResponseView`

A `@JvmInline value class` over `CompoundBinaryTag`:

- `getText(key): String?` — `StringBinaryTag`
- `getFloat(key): Float?` — any `NumberBinaryTag`, as `floatValue()`
- `getBoolean(key): Boolean?` — a `ByteBinaryTag != 0`, or a `StringBinaryTag` via
  `toBooleanStrictOrNull()`. A boolean control configured with custom `onTrue`/`onFalse` texts is
  therefore only readable when those texts happen to read as booleans.
- `EMPTY` and `of(payload: BinaryTag?)` — anything that is not a `CompoundBinaryTag` becomes `EMPTY`

Every read returns `null` for a missing or mistyped key rather than throwing.
