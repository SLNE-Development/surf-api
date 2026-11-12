# View state and stateful buttons

Sources: `view/state/` (`StateDsl.kt`, `StateHandle.kt`, `DeferredState.kt`, `StateRegistry.kt`),
`view/button/` (`ViewButton.kt`, `ViewButtonDsl.kt`, `ViewButtonHandle.kt`) and
`common/core/api/.../inventory/framework/internal/ViewButtonStates.kt`.

## Why state is deferred

IF's state factories (`View.state`, `mutableState`, `computedState`, `lazyState`, `initialState`)
are instance methods, but the DSL block runs *before* the view object exists. So `StateDsl`
registers a `DeferredState` in the context's `StateRegistry` and hands back a `StateHandle<S>`
holding the registry plus an index. `SurfViewDSLImpl` / `PaginatedSurfViewDSLImpl` call
`ctx.stateRegistry.resolveStates(this)` in their `init` block, which walks the deferred list in
order and calls the matching IF factory.

Consequences:

- Declare state in the DSL block only. `StateRegistry.register` has
  `require(resolvedStates.isEmpty()) { "Cannot register new states after resolution has begun" }`,
  and `resolveStates` itself has `check(!resolved) { "States have already been resolved" }`.
- Declaration order is the resolution order, and a handle is just an index - do not share a handle
  between views.

## The factories

All of them are `context(ctx: AbstractSurfViewContext<*>)` top-level functions in
`view.state`:

| Factory | Returns | `DeferredState` | IF call |
| --- | --- | --- | --- |
| `state(initialValue: T)` | `StateHandle<State<T>>` | `Immutable` | `view.state(v)` |
| `mutableState(initialValue: T)` | `StateHandle<MutableState<T>>` | `Mutable` | `view.mutableState(v)` |
| `mutableState(initialValue: Int)` | `StateHandle<MutableIntState>` | `MutableInt` | `view.mutableState(int)` |
| `computedState { context -> T }` | `StateHandle<State<T>>` | `Computed` | `view.computedState(Function)` |
| `computedState { T }` | `StateHandle<State<T>>` | `ComputedSupplier` | `view.computedState(Supplier)` |
| `lazyState { context -> T }` | `StateHandle<State<T>>` | `Lazy` | `view.lazyState(Function)` |
| `lazyState { T }` | `StateHandle<State<T>>` | `LazySupplier` | `view.lazyState(Supplier)` |
| `initialState<T>()` | `StateHandle<MutableState<T>>` | `Initial(null)` | `view.initialState<Any>()` |
| `initialState<T>(key: String)` | `StateHandle<MutableState<T>>` | `Initial(key)` | `view.initialState<Any>(key)` |

An `Int` literal resolves to the `MutableIntState` overload - `mutableState(0)` is int state,
`mutableState(0 as Any?)` is not. Computed state re-evaluates on every read; lazy state computes
once per host (view session) and caches. Initial state receives what the view was opened with:
`view.open(player, data)` for the key-less form, or the value under `key` when the data is a
`Map<String, Any>`.

## Reading and writing

Operators in `view.state` (import them explicitly - `get`, `set`, `increment`, `decrement`):

```kotlin
val counter = mutableState(0)
val profile = initialState<Profile>()

onFirstRender {
    val p = profile[this]                 // read, any Context
    slot(1, 1) {
        withItem(ItemType.PAPER) { displayName { text(p.name) } }
        onItemClick {
            counter.increment(this)       // returns the new value
            update()                      // IF: re-render this context
        }
    }
}
```

`StateHandle<State<T>>.get`, `StateHandle<MutableState<T>>.get` (JVM name `getMutableState`) and
`StateHandle<MutableIntState>.get` all take a `Context`; `set` is available for the two mutable
forms, `increment`/`decrement` for int state. Writing a `MutableState` makes IF update every
component that watches it (`updateOnStateChange(state)` / `watchState(id, …)`); reading does not.
There is no handle-level `watch` - use IF's own `Context.watchState` with the resolved state when
needed.

## Stateful buttons

A stateful button is a slot that cycles through values, with its value stored **per view session**,
so every viewer has their own. Declare it in the DSL block, place the handle in a slot.

```kotlin
val visibility = tripleButton(TestVisibility.ALL) {
    state(TestVisibility.ALL) {
        item(ItemType.LIME_DYE) { displayName { text("Sichtbar für alle") } }
    }
    state(TestVisibility.FRIENDS) {
        item(ItemType.YELLOW_DYE) { displayName { text("Sichtbar für Freunde") } }
    }
    state(TestVisibility.NONE) {
        item(ItemType.RED_DYE) { displayName { text("Für niemanden sichtbar") } }
    }

    onChange { from, to -> player.sendMessage(text("$from -> $to")) }
    onCloseChanged { initial, current -> repository.save(player, current) }
}

onFirstRender {
    slot(1, 5) { button(visibility) }
}
```

| Factory (all `context(AbstractSurfViewContext<*>)`) | States | Initial value |
| --- | --- | --- |
| `statefulButton(initial: T) { }` | any number, declaration order | fixed |
| `computedStatefulButton({ context -> T }) { }` | any number | resolved once per session |
| `toggleButton(initial: Boolean = false) { }` | `false`, `true` (pinned order) | fixed |
| `computedToggleButton({ context -> Boolean }) { }` | `false`, `true` | resolved once per session |
| `tripleButton(initial: T) { }` | exactly three, else `IllegalArgumentException` | fixed |
| `computedTripleButton({ context -> T }) { }` | exactly three | resolved once per session |

Use the `computed*` variants whenever the button reflects something already stored for the player -
that is what makes "changed on close" mean "changed relative to what the player had".

### Inside the builder

`ViewButtonBuilder<T>`:

- `state(value, block)` - declares a state; the same `value` twice reconfigures it instead of adding
  a second. Declaration order is the cycle order.
- `onChange { from, to -> }` - every transition, after the per-state `onEnter`.
- `onCloseChanged { initial, current -> }` - on close, only if the final value differs from the
  initial one. Skipped when the view's own `onClose` cancelled the close.
- `reverseOnRightClick(enabled = true)` - a right click steps backwards; disable to make every click
  step forward.
- `check(scopes.isNotEmpty())`: at least one state is required.

`ViewToggleButtonBuilder` wraps it for `Boolean` with `whenOff { }` / `whenOn { }`, `onToggle
{ enabled -> }` and `onCloseChanged`; it pre-declares `false` then `true` so the cycle order does
not depend on which block you write first.

`ViewButtonStateScope<T>` (one per state):

- `item(material: Material, amount: Int = 1) { }` / `item(type: ItemType, amount: Int = 1) { }` -
  rebuilt on every render.
- `renderItem { … }` - `SlotRenderContext.() -> ItemStack`, for an appearance that depends on the
  viewer.
- `onEnter { from -> }` - when the button switches *into* this state; not called for the starting
  state.
- Every state must declare an appearance, or the build fails with "Button state '<value>' has no
  appearance."

### The handle

`ViewButtonHandle<T>` wraps a `lazyState` holding a `ViewButtonSession<T>` (core internal: the
ordered values, the initial one, the current index, `hasChanged`, wrapping `advance(step)`), plus
the immutable `ViewButtonSpec`.

| Member | Effect |
| --- | --- |
| `handle[context]` | the state currently shown for that viewer |
| `handle[context] = value` | jump to `value`; **no** `onEnter`/`onChange`, **no** re-render - call `update()` yourself. Throws if the value is not declared |
| `handle.initial(context)` | the value the viewer started on |
| `handle.hasChanged(context)` | whether it currently differs from the initial |

`BukkitItemComponentBuilder.button(handle)` (in `view.button`) wires the slot: it calls
`cancelOnClick()`, renders the current state's item, and on click advances the session, repaints via
`component?.update()` *before* running `onEnter`/`onChange` (so a callback may navigate away), then
runs them.

Close hooks are not per slot: each button factory registers one on the view context
(`registerButtonCloseHandler`), and `SurfViewDSLImpl.onViewClose` / `PaginatedSurfViewDSLImpl`
fire them all through `ctx.fireButtonCloseHandlers(close)` after the user's `onClose`, returning
early when `close.isCancelled`.
