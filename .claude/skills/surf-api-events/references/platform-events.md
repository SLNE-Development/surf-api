# Platform event systems and how they relate to the surf bus

Sources: `platform/paper/api/src/main/kotlin/dev/slne/surf/api/paper/event/`,
`platform/minestom/api/src/main/kotlin/dev/slne/surf/api/minestom/event/`,
`platform/minestom/api/src/main/kotlin/dev/slne/surf/api/minestom/chat/AsyncPlayerChatEvent.kt`,
`platform/minestom/impl/src/main/kotlin/dev/slne/surf/api/minestom/server/SurfMinestomServer.kt`.

The surf bus does not replace or wrap the platform's event system. They are independent: a Bukkit
`PlayerJoinEvent` never reaches a `@SurfEventHandler`, and a `SurfSyncEvent` never reaches a Bukkit
`Listener`. The surf bus exists for signals that surf-api and surf plugins define themselves, and
that must work the same on Paper, Minestom, Velocity and standalone.

## Paper

### Registering Bukkit listeners

`event/listener-extension.kt` is sugar over `PluginManager`, nothing more - cancellation, priority
and threading stay Bukkit's.

```kotlin
class JoinListener : Listener {
    @EventHandler
    fun onJoin(event: PlayerJoinEvent) { … }
}

JoinListener().register()            // JavaPlugin.getProvidingPlugin(this::class.java)
JoinListener().register(plugin)      // explicit plugin
listener.unregister()                // HandlerList.unregisterAll(this)
```

| Declaration | Notes |
| --- | --- |
| `Listener.register(plugin)` / `Listener.register()` | `pluginManager.registerEvents`; the no-arg form infers the plugin from the listener's class |
| `Listener.unregister()` | unregisters all of the listener's handlers |
| `Listener.register(plugin, eventClass, priority = EventPriority.NORMAL, ignoreCancelled = false, executor)` | one event with a custom `(Listener, Event) -> Unit` executor |
| `Listener.register<T>(priority, ignoreCancelled, executor)` | reified form of the above |
| `SingleListener<T>(priority, ignoreCancelled)` | abstract `Listener` with a single `abstract fun T.onEvent()` |
| `SingleListener<T>.register(plugin, eventClass)` / `register<T>()` | registers that one event |
| `listen<T>(priority, ignoreCancelled, register = true) { … }` | builds and (by default) registers an anonymous `SingleListener<T>`; extension on `Any`, returns the listener so you can `unregister()` later |
| `Cancellable.cancel()` | `isCancelled = true` |

Note that Bukkit's `ignoreCancelled` default here is `false`, unlike `@SurfEventHandler`'s `true`.

### Surf-bus events Paper declares

All in `event/common/`, all `SurfSyncEvent`:

| Event | Payload | Cancellable |
| --- | --- | --- |
| `PlayerAfkStateChangeEvent` | `playerUuid: UUID`, `fromState: Boolean`, `toState: Boolean` | no |
| `PlayerJoinMessageEvent` | `player: Player`, `var message: Component` | yes |
| `PlayerQuitMessageEvent` | `player: Player`, `var message: Component` | yes |
| `PlayerDeathMessageEvent` | `player: Player`, `var message: Component`, `lastDamageCause: EntityDamageEvent.DamageCause` | yes |

The three message events follow one pattern: mutate `message` to rewrite the broadcast, cancel to
suppress it. Listen with `@SurfEventHandler` or `SurfEventBus.on<…>`, not with a Bukkit `Listener`.

These are declarations only - **nothing in this repository fires them**. The publishers live in the
downstream plugins that own join/quit/death messaging and AFK tracking, so a change to their shape
is a breaking change outside this repo.

## Minestom

### EventRegistrar

Minestom has its own `EventNode` tree. surf-api creates one node, named by
`SurfMinestomServerBuilder.eventNodeName` (default `"surf-api-minestom"`), as a child of the global
handler, and hands it to every registered `EventRegistrar`:

```kotlin
class WelcomeListener : EventRegistrar {
    override fun register(node: EventNode<Event>) {
        node.addListener<PlayerSpawnEvent> { it.player.sendMessage("Welcome!") }
    }
}

surfMinestomServer {
    listeners(WelcomeListener())
}
```

The reified `addListener<T> { }` used above is surf's own extension on `EventNode<Event>`, in
`platform/minestom/api/.../minestom/extension/events.kt`; it wraps `EventListener.of`.

`EventRegistrar` is a `fun interface`, so a lambda works too. The api's own features use it -
`MinestomInventoryLoader`, `DialogCallbackListener`, `MinestomSuggestionListener` are all
`EventRegistrar`s.

The KDoc on `EventRegistrar` shows `surfMinestomApi { … }`; the actual entry point is
`surfMinestomServer { … }` in `platform/minestom/impl/.../server/SurfMinestomServer.kt`.

### SuspendingEventNode

Minestom listeners run on the tick thread and cannot suspend. `SuspendingEventNode<E>` is the
api's answer for events whose listeners should be able to suspend or block:

```kotlin
val registration = AsyncPlayerChatEvent.addListener(priority = 0) { event ->
    val prefix = database.loadPrefix(event.player.uuid)   // suspending or blocking, both fine
    event.renderer = ChatRenderer.viewerUnaware { _, name, message -> … }
}

registration.close()   // Registration is AutoCloseable
```

- `addListener(priority: Int = 0, listener: suspend (E) -> Unit): Registration<E>` - **lower
  priority runs first**, and the list is re-sorted on every registration.
- `Registration.close()` removes that one listener.
- `call(event)` is `@InternalSurfApi`; only the api fires these.
- Listeners run sequentially. A failure is logged ("Listener for '…' failed, continuing with the
  next one") and the next listener still runs - except a `CancellationException` that is not the
  listener's own timeout, which is rethrown and aborts the dispatch. `isOwnTimeout` treats a
  `TimeoutCancellationException` raised while the calling coroutine is still active as the
  listener's own problem.

`AsyncPlayerChatEvent` is the worked example: `viewers` is a mutable set of recipients, `renderer`
and `message` are mutable, `originalMessage` and `signedMessage` are not affected by changes, and
`isCancelled = true` drops the message. Messages from one player are processed in order, so a slow
listener delays that player's next message but not the tick loop.

## Choosing

| You want to… | Use |
| --- | --- |
| react to vanilla/server behaviour | the platform's events (Bukkit `Listener`, Minestom `EventNode`) |
| expose a signal other surf plugins can observe or veto, on any platform | `SurfSyncEvent` / `SurfAsyncEvent` |
| let listeners suspend | `SurfAsyncEvent`, or `SuspendingEventNode` on Minestom |
| rewrite a surf-managed chat/join/quit/death message | the surf-bus event for it |
