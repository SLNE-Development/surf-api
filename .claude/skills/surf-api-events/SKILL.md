---
name: surf-api-events
description: Use when working with surf-api events - declaring a SurfSyncEvent or SurfAsyncEvent, making one cancellable with SurfCancellableEvent, firing it with call()/callSync/callAsync, registering a listener with @SurfEventHandler or SurfEventBus.on/onAsync, choosing a SurfEventPriority or using ignoreCancelled, wanting a suspending listener, deciding between the surf event bus and the platform's own (Bukkit HandlerList/Listener, Minestom EventNode), or diagnosing "must not be suspend", "No @SurfEventHandler methods found", "must take exactly one parameter", a listener that never fires, or an exception swallowed inside a listener.
---

# surf-api events

Every claim below comes from `common/core/api/src/main/kotlin/dev/slne/surf/api/core/event/` and
`common/core/impl/src/main/kotlin/dev/slne/surf/api/core/server/impl/event/`. Read those files when
in doubt; they are the only source of truth.

## One bus, two flavours

`SurfEvent` is a **sealed** marker interface. You never implement it directly - you extend one of
its two abstract subclasses, and the choice is permanent because it decides how the event is
dispatched:

| Base | Dispatched by | Listeners may suspend |
| --- | --- | --- |
| `SurfSyncEvent` | `callSync`, on the calling thread | no |
| `SurfAsyncEvent` | `callAsync`, a `suspend` function | yes |

```kotlin
data class PlayerAfkStateChangeEvent(
    val playerUuid: UUID,
    val fromState: Boolean,
    val toState: Boolean,
) : SurfSyncEvent()

data class ShopRestockEvent(
    val shopId: String,
) : SurfAsyncEvent()
```

Make it cancellable by also implementing `SurfCancellableEvent`, which needs one override:

```kotlin
data class PlayerJoinMessageEvent(
    val player: Player,
    var message: Component,
) : SurfSyncEvent(), SurfCancellableEvent {
    override var isCancelled = false
}
```

`cancel()` is a default method on the interface. A property a listener is meant to change has to be
a `var`.

## Publishing

The base classes carry the convenient form; `call()` returns **`false` when the event ended up
cancelled**, `true` otherwise (always `true` for a non-cancellable event):

```kotlin
val event = PlayerJoinMessageEvent(player, message)
if (event.call()) {              // SurfSyncEvent.call(), plain function
    broadcast(event.message)
}

val restock = ShopRestockEvent(shopId)
if (restock.call()) { … }        // SurfAsyncEvent.call(), suspend function
```

The bus functions underneath return the event itself, not a boolean:
`SurfEventBus.callSync(event)` and `suspend SurfEventBus.callAsync(event)`. Use them when you want
the fluent form; use `call()` when you want the cancellation answer.

Firing an async event needs a coroutine. Nothing in the event API creates one for you.

## Registering a listener

### Annotated methods

```kotlin
object ShopListener {
    fun register() = SurfEventBus.registerListeners(this)
    fun unregister() = SurfEventBus.unregisterListeners(this)

    @SurfEventHandler
    private fun onAfk(event: PlayerAfkStateChangeEvent) { … }

    @SurfEventHandler(priority = SurfEventPriority.HIGH, ignoreCancelled = false)
    private suspend fun onRestock(event: ShopRestockEvent) {
        delay(1.seconds)
    }
}
```

`registerListeners` walks the class and its superclasses, so inherited handlers are picked up, and
private methods are fine - the invoker uses a private lookup. Rules checked at registration, each
throwing immediately:

- exactly one parameter (a trailing `Continuation` on a suspend function does not count), and its
  type must be a `SurfEvent` subclass
- not `static`
- not `suspend` when the parameter is a `SurfSyncEvent` - "must not be suspend"
- the declaring class must be reachable from the bus's lookup, else `IllegalAccessException`
- at least one `@SurfEventHandler` method on the object, else "No @SurfEventHandler methods found"

`unregisterListeners(listener)` removes every handler registered from that exact instance.

### Lambdas

```kotlin
val token = SurfEventBus.on<PlayerAfkStateChangeEvent> { event -> … }
val asyncToken = SurfEventBus.onAsync<ShopRestockEvent>(priority = SurfEventPriority.MONITOR) {
    event -> audit(event)
}

SurfEventBus.unregisterListeners(token)
```

`on` / `onAsync` are reified extensions over `registerHandler(eventClass, priority,
ignoreCancelled, handler)` and `registerAsyncHandler(…)`. Both default to
`priority = SurfEventPriority.NORMAL, ignoreCancelled = true`, and both return an opaque token -
**keep it**, it is the only way to unregister.

## Priority, ordering and cancellation

`SurfEventPriority` is `LOWEST, LOW, NORMAL, HIGH, HIGHEST, MONITOR`, invoked in that order.
Handlers of equal priority run in registration order (a global counter), so ordering is
deterministic but depends on which plugin registered first - do not rely on it across plugins.

`ignoreCancelled` defaults to **`true`**, the opposite of Bukkit's default: by default a handler is
**skipped** once an earlier handler cancelled the event. Pass `ignoreCancelled = false` to keep
seeing cancelled events. `MONITOR` handlers always run regardless, and are expected to observe
only - that is convention, not enforced.

Handlers always run **sequentially**, one after another, never in parallel, for both flavours.

## Which event system to use

| Situation | Use |
| --- | --- |
| Something happening in Minecraft itself (join, block break, damage) | the platform's own events |
| A surf-api or cross-plugin signal that other surf plugins should be able to observe or veto | `SurfSyncEvent` / `SurfAsyncEvent` on the surf bus |
| You need listeners that suspend or do I/O | `SurfAsyncEvent`, or Minestom's `SuspendingEventNode` |

On Paper, `platform/paper/api/.../paper/event/listener-extension.kt` gives you Bukkit registration
helpers (`Listener.register()`, `Listener.unregister()`, `listen<T> { }`, `SingleListener<T>`), and
`platform/paper/api/.../paper/event/common/` holds the surf-bus events Paper declares. On Minestom,
`EventRegistrar` contributes listeners to the api's `EventNode`, and `SuspendingEventNode` backs the
suspending events such as `AsyncPlayerChatEvent`. Details: `references/platform-events.md`.

## Traps

- **Pick the right base class.** A handler for a `SurfSyncEvent` can never suspend, and a
  `SurfAsyncEvent` can only be fired from a coroutine. Changing the base later is a breaking change
  for every listener.
- **Blocking work in a sync listener blocks the publisher.** `callSync` runs handlers on the calling
  thread, which on Paper is usually the main thread. Database or HTTP work belongs in a
  `SurfAsyncEvent` handler - and even there handlers are sequential, so a slow one delays every
  later handler and the `call()` that is awaiting them.
- **Exceptions are swallowed.** A throwing handler is logged at SEVERE and dispatch continues. Your
  listener's failure will not surface to the publisher and will not cancel the event - check the
  log.
- **`ignoreCancelled = true` is the default.** A listener that must see cancelled events needs
  `ignoreCancelled = false`, or `MONITOR`.
- **Polymorphic dispatch is real.** A handler for a supertype fires for every subtype
  (`isAssignableFrom`). Narrow the parameter type if you only want one event.
- **Cancellation is re-read before every handler.** A handler that sets `isCancelled = false` again
  makes all later handlers run, and the event's `call()` then returns `true`.
- **Keep the lambda token.** `unregisterListeners` matches by identity. Passing the event class, or
  the enclosing object, removes nothing.
- **Unregister on disable.** Handlers are held in static bus state. A listener from an unloaded
  plugin keeps firing, and keeps its classes alive, until it is unregistered.
- **Async handlers inherit the publisher's coroutine context.** `callAsync` calls `ensureActive()`
  before each handler, so cancelling the publishing coroutine stops the remaining handlers
  mid-dispatch. A `CancellationException` thrown by a handler itself is only logged.
- **No impl, no bus.** `SurfEventBus` resolves through `requiredService` at class-init time, so a
  missing `-impl` module fails with `ServiceConfigurationError` on first touch.

Dispatch internals, the hidden-class invokers and the exact exception and cancellation semantics:
`references/dispatch-and-invokers.md`. Registering a listener as a bean: the `surf-api-components`
skill.
