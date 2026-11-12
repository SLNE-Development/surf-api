---
name: surf-api-dialogs
description: Use when working on surf-api's dialog builders - writing or changing a `dialog { base { } type { } }` tree, a notice, confirmation, multiAction, dialogList or serverLinks dialog, dialog bodies and input controls, reading a pressed button's response, the Minestom `DialogCallbacks` registry and `DialogResponseView`, Paper's `composableDialog`/`DialogScope`/`DialogStore` state and `paginatedDialog`, either platform's `searchDialog`, or when a dialog compiles on one platform and not on the other and you need to know which concepts exist where.
---

# surf-api dialogs

Two implementations, in two packages, that have grown apart:

| | Paper | Minestom |
| --- | --- | --- |
| Package | `platform/paper/api/src/main/kotlin/dev/slne/surf/api/paper/dialog/` | `platform/minestom/api/src/main/kotlin/dev/slne/surf/api/minestom/dialog/` |
| Wraps | `io.papermc.paper.dialog.Dialog` built through `DialogRegistryEntry.Builder` | `net.minestom.server.dialog.Dialog` + `DialogMetadata` |
| Every file carries | `@file:Suppress("UnstableApiUsage")` | nothing special |
| Tests | none | `platform/minestom/api/src/test/kotlin/.../dialog/DialogBuilderTest.kt` |

They are **not** a common abstraction with two backends. There is no shared dialog interface, no
`expect`/`actual`, no `common/core` dialog package. Code written against one does not compile
against the other, and the two files that share a name (`DialogBuilder.kt`, `CommonDialogs.kt`,
`search/SearchDialog.kt`, the five `builder/*.kt`) differ in their types, their defaults and their
callback model. Never describe or write a "unified" dialog API.

## What exists on which platform

| Concept | Paper | Minestom |
| --- | --- | --- |
| `dialog { }` entry point | yes, plus a capitalised `Dialog { }` alias | yes, only lowercase |
| `base { }` / `type { }` | extensions on `DialogRegistryEntry.Builder` | extensions on `DialogEntryBuilder`, both required |
| notice / confirmation / multiAction / dialogList / serverLinks | all five | all five |
| `dialogList` from registry keys (`TypedKey<Dialog>`) | yes, resolved through `RegistryAccess` | no — direct dialog instances only (`HolderSet.Direct`) |
| Standalone `DialogType { }` / `dialogType { }` | yes | no — a type needs the metadata, so it only exists inside `type { }` |
| Server-side button callback | Adventure `ClickCallback` / Paper `DialogActionCallback` via `ClickEvent.callback` | own `DialogCallbacks` registry keyed under `surf_dialog_callback` |
| Callback lifetime/use limits | `ClickCallback.Options` | `DialogCallbackOptions(uses, lifetime)` — 1 use, 12 h by default |
| `DialogCallbackContext` | no | yes (`player`, raw `payload`) |
| Response view | Paper's `io.papermc.paper.dialog.DialogResponseView` | own `DialogResponseView` value class over `CompoundBinaryTag` |
| Closing a dialog from code | `Player.clearDialogs(showEmptyDialogBefore)`, `@NmsUseWithCaution` | `player.closeDialog()` (plain Minestom API) |
| `DialogScope`, `DialogStore`, `DialogState`, `composableDialog`, `remember` | yes | **no** |
| `DialogQuery`, `PageResult`, `PageState`, `paginatedDialog` | yes | **no** |
| `CursorDialogQuery`, `CursorResult`, `CursorState` | declared, **no caller anywhere in the repository** | no |
| `searchDialog` | yes | yes, same parameter list, different types |
| `DialogEntryBuilder` | no (Paper's own registry builder fills that role) | yes |

## Declaring a dialog

Minestom — both halves are required, and `DialogEntryBuilder.build()` throws without them
(`minestom/dialog/builder/DialogEntryBuilder.kt`). Adapted from `DialogBuilderTest.kt`:

```kotlin
val dialog = dialog {
    base {
        title { primary("Confirm") }
        preventClosingWithEscape()
        afterAction(DialogAfterAction.WAIT_FOR_RESPONSE)
        body { plainMessage { info("This cannot be undone.") } }
        input {
            text("message") {
                label { info("Message") }
                maxLength(64)
            }
        }
    }
    type {
        confirmation {
            yes {
                label { success("Yes") }
                action { customPlayerClick { response, player ->
                    player.closeDialog()
                    player.sendMessage(response.getText("message") ?: "")
                } }
            }
            no { label { error("No") } }
        }
    }
}

player.showDialog(dialog)
```

Paper — `dialog { }` applies the block to an empty `DialogRegistryEntry.Builder`, so `base` and
`type` are siblings there too. From `paper/dialog/pagination/PaginatedDialog.kt`:

```kotlin
dialog {
    base {
        title { text("Seite 1") }
        input { text("search") { label { text("Suche") }; initial("") } }
    }
    type {
        multiAction {
            columns(1)
            action(actionButton {
                label { text("Weiter") }
                action { playerCallback { it.sendMessage("clicked") } }
            })
        }
    }
}
```

`title`, `label`, `plainMessage` and friends take a `SurfComponentBuilder` block — `primary`,
`info`, `success`, `error`, `text`, `spacer` come from the messages layer, not from here; see the
messages skill.

## Bodies and inputs

Both platforms build bodies with `body { }` → `DialogBodyBuilder` (`plain { }`, `plainMessage(…)`,
`item { }`) and inputs with `input { }` → `DialogInputBuilder`. The control keys are what the
response is read by.

The difference is the default handling. Paper's builders hold **nullable** fields and only call the
Paper builder when a value was set; Minestom's hold concrete defaults and always pass them to the
record constructor. So `width`, `maxLength`, `labelVisible`, `showTooltip` and the rest fall back to
Paper's defaults on Paper and to surf's own constants on Minestom (`DEFAULT_MAX_LENGTH = 32`,
item `16 × 16`, `DialogBody.PlainMessage.DEFAULT_WIDTH`, `DialogInput.DEFAULT_WIDTH`).

Input kinds, both platforms: text, boolean, number range, single option. Paper additionally exposes
`simpleText`, `simpleBoolean`, `simpleNumberRange` and `addInput`; Minestom exposes `input(DialogInput)`
and a `numberRange(key, range) { }` overload taking a Kotlin `ClosedRange`. Paper's number range is
reached as `numberRange(key, range) { }` only — there is no overload without a range.
`references/paper-dialogs.md` and `references/minestom-dialogs.md` have the per-control field lists.

## How a response comes back

Paper: a button's action is a `ClickEvent`. `callback { }` / `playerCallback { }` give you the
audience only; `customClick(options) { response, audience }` and
`customPlayerClick(options) { response, player }` give you Paper's `DialogResponseView`, read as
`response.getText("key")`. The client only reports input values for a custom-click action.

Minestom: `DialogCallbacks` is a surf-owned registry. `callback { }`, `playerCallback { }`,
`customClick { }` and `customPlayerClick { }` register a lambda, get back a random
`surf_dialog_callback:<uuid>` key and emit `DialogAction.Custom` or `DialogAction.DynamicCustom`.
When the client presses the button, `PlayerCustomClickEvent` reaches
`minestom/impl/.../dialog/DialogCallbackListener.kt`, which calls `DialogCallbacks.dispatch`.
A registration **expires**: one use and 12 hours by default
(`DialogCallbackOptions.ONE_USE`, `DEFAULT_LIFETIME`), `UNLIMITED_USES = -1` for a reusable button.
A dialog kept open past its callback's lifetime silently stops working — pass explicit options for
anything long-lived. `DialogResponseView.of(payload)` turns the raw tag into typed reads
(`getText`, `getFloat`, `getBoolean`), returning `null` rather than failing on a missing or
mistyped key.

## Pagination and search

Pagination exists on **Paper only**: `composableDialog` + `DialogStore` re-render a dialog when the
state changes, `DialogQuery<PageState, T>` fetches a page, and `paginatedDialog(…)` assembles a
`multiAction` dialog with item buttons and Zurück/Weiter/Suchen buttons. `searchDialog` exists on
both, as a confirmation dialog whose two buttons mean "search" and "cancel" — note that the search
button is wired to `no` and the cancel button to `yes` on both platforms. Both carry hard-coded
German default labels. See `references/pagination-and-search.md`.

## Not in this skill

Read the sibling skill instead of inferring its rules from here: `surf-api-commands` for the command
a dialog is usually opened from, `surf-api-messages` for `SurfComponentBuilder`, `Colors` and the
`primary`/`info`/`success`/`error` helpers every `title { }` and `label { }` block uses,
`surf-api-inventory` for inventory menus, `surf-api-components` for the DI container,
`surf-api-nms` for the Paper NMS bridges and `surf-api-build` for the Gradle side.
`Player.clearDialogs` is a Paper NMS bridge call; its mechanics belong to `surf-api-nms`.

## Known rough edges

- Paper `search/SearchDialog.kt` nests `input { }` inside `body { }` and `type { }` inside
  `base { }`. It compiles only because Kotlin falls back to the outer receiver. Do not copy that
  nesting; Minestom's `search/SearchDialog.kt` has the same function written flat and correct.
- `DialogStore.mounted` is never set to `false`, and `remember`'s cache is never invalidated, so a
  `remember` key that stays equal across state changes will serve a stale value forever.
- `CursorDialogQuery` / `CursorResult` / `CursorState` are declared and unused — no cursor-paginated
  dialog helper exists.

## References

Read on demand, not up front:

- `references/paper-dialogs.md` — every type in the Paper package, the builder field lists, the
  action kinds, the composition/state model and the query types.
- `references/minestom-dialogs.md` — every type in the Minestom package, the builder field lists and
  defaults, and the `DialogCallbacks` lifecycle end to end.
- `references/pagination-and-search.md` — `paginatedDialog` and both `searchDialog`s in full, with
  their parameters, defaults and German labels.
