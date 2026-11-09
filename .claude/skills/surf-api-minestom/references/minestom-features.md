# The Minestom-only parts of surf-api

Paths relative to `platform/minestom/api/src/main/kotlin/dev/slne/surf/api/minestom/` unless said
otherwise. `local/minestom-example/` and `local/minestom-example-plugin/` are the working samples.

Several of these need a server feature installed - see `references/server.md` for the two steps
(Gradle `withXxx()` plus runtime `withXxx()`).

## `chat/` - asynchronous and signed chat

Needs `withSignedChat()`.

`AsyncPlayerChatEvent` is the chat event Minestom does not have. Its listeners run **off the tick
thread** and may suspend or block without hurting the tick rate; messages from one player are
processed in order, so a slow listener only delays that player's next message.

```kotlin
AsyncPlayerChatEvent.addListener { event ->
    event.renderer = ChatRenderer.viewerUnaware { _, name, message ->
        Component.text()
            .append(Component.text("[Example] ", NamedTextColor.AQUA))
            .append(name)
            .append(Component.text(": ", NamedTextColor.GRAY))
            .append(message)
            .build()
    }
}
```

The event carries `player`, `viewers` (a `MutableSet<Audience>` - add or remove recipients),
`renderer`, `message` and `originalMessage` (unaffected by writes to `message`). `ChatRenderer` is a
`fun interface` with a **suspending** `render(source, sourceDisplayName, message, viewer)` called
once per audience; `ChatRenderer.viewerUnaware { source, name, message -> }` is the variant that
does not depend on the viewer.

Signature-preserving forwarding is `Player.sendSignedMessage(message, name, rendered)`:

```kotlin
for (player in ConnectionManager.onlinePlayers) {
    player.sendSignedMessage(message, name, rendered)
}
```

`ChatSessionHolder`, `RemoteChatSender` and `RemoteSignedMessage` are the pieces for messages that
arrive from elsewhere (a proxy or another server) with their signature intact.

## `player/` - the surf player and its events

A surf server creates every player as `SurfMinestomPlayer` (or a subclass set through
`surfMinestomServer { playerFactory { ... } }`). It adds to Minestom's `Player`:

- Adventure's `PermissionChecker` pointer, resolved through `MinestomPermissions`, which
  `Audience.hasPermission` of surf-api-core reads
- game-mode switches that complete the way vanilla does - a spectator is hidden from everyone but
  other spectators, leaves its vehicle and stops using its item
- player-list entries that keep the signed chat session, and signed-message deletion through the
  server's signed chat

A subclass **must not** override the viewer or viewable rule methods: the player visibility feature
calls them on the player.

Surf player events, in `player/event/`:

| Event | When |
| --- | --- |
| `AsyncPlayerCountEvent` | during configuration, to decide whether a full server admits the player. `result`, `Result.KICK_FULL`, `allow()` |
| `AsyncPlayerSpawnLocationEvent` | on the player's virtual configuration thread, so a stored location may be loaded there. Write `position` |
| `PlayerJoinEvent` | `joinMessage` |
| `PlayerQuitEvent` | `quitMessage` |
| `PlayerToggleFlightEvent` | cancellable |

`player/PlayerLimit` is the limit `SurfMinestomServerBuilder.maxPlayers` sets up.

## `visibility/` - how many players a player sees

Needs `withPlayerVisibility()`, configured with the settings the example server uses:

```kotlin
withPlayerVisibility {
    maxVisible = 10
    activateAt = 15
    deactivateBelow = 8
}
```

When the limit is active, the server **owns** each player's viewer rule. Restricting what a player
sees therefore goes through `PlayerVisibility`, not `Player#updateViewerRule`, which would drop the
limit for that player:

```kotlin
PlayerVisibility.updateViewerRule(player) { entity -> entity !is Player || canSee(player, entity) }
PlayerVisibility.updateViewerRule(player)                 // re-evaluate
PlayerVisibility.updateViewerRule(player, null)           // lift the restriction
```

`isLimited(player)` says whether the limit currently restricts that player, `isManaged(player)`
whether the limit owns its viewer and viewable rules. Without the feature these functions simply
forward to the player's own viewer rule, so code written against `PlayerVisibility` also works on a
server that does not install it.

## `npc/` - mannequin NPCs

Needs `withNpcLib()`, which installs StomNPCs on a process-wide event node and ships
`mixins.surf-api-minestom-server-npc.json`.

From `local/minestom-example/.../ExampleNpcs.kt`:

```kotlin
mannequinNpc("guide", instance, Pos(3.5, 41.0, 3.5, 135f, 0f)) {
    displayName = Component.text("Guide", NamedTextColor.GREEN)
    setEquipment(EquipmentSlot.MAIN_HAND, ItemStack.of(Material.COMPASS))
    onInteract { interaction ->
        interaction.player.sendMessage(Component.text("Try /example highlight!"))
    }
}
```

`MannequinNpcBuilder` has `displayName` (the hologram above the NPC), `profile`
(`ResolvableProfile?`, `null` for the default), `scale` (`1.0` is player-sized),
`displayNameOffset` (defaults to just above the head, following `scale`), `description` (what the
client renders *below* the mannequin; empty hides the client's default label), `setEquipment(slot,
item)` and `onInteract { }`, which may be called more than once. `mannequinNpc(...)` builds **and
spawns**.

## `highlight/` - outlining a single block for a single player

No feature needed; `BlockHighlights` is a plain object using block-display entities.

```kotlin
BlockHighlights.show(viewer, instance, position, NamedTextColor.AQUA)
BlockHighlights.hideAll(viewer)
```

`show(viewer, instance, position, color = null, block = instance.blockAtOrAir(position))` - the
colour is the glow colour (`null` uses the client default) and `block` is what the outline is drawn
as. Showing one where the same player already has one at that position replaces it, which is how a
highlight is refreshed after the block below changed. Nothing happens when the chunk is neither
loaded nor loadable.

A highlight **lives until it is taken back**, so whoever shows one owns it.
`hide(viewer, instance, position)` returns whether there was one, `hideAll(viewer)` returns how many
it removed, and the latter is what a player leaving calls for.

## `plugin/` - the plugin model

`SurfMinestomPlugin`, `PluginMeta` and `PluginManager` - see `references/plugin.md`.
`PluginManager.instance` throws when the plugins feature is not installed.

## `coroutine/` - dispatchers and scopes

`MinestomDispatchers`:

| Dispatcher | What it does |
| --- | --- |
| `Main` | dispatches to the next tick through the scheduler manager, and implements `Delay`, so `delay()` and `withTimeout` work in ticks rather than wall-clock threads |
| `Blocking` | a virtual-thread-per-task executor, threads named `surf-blocking-<n>` |

Ready-made scopes, each a `SupervisorJob` under a shared `CoroutineExceptionHandler` that logs
through `MinecraftServer.LOGGER`: `minestomScope` (on `Main`), `minestomAsyncScope` (on
`Dispatchers.Default`) and `minestomBlockingScope` (on `Blocking`). A plugin should prefer its own
`scope`, which is cancelled on disable.

`entity-scheduler.kt` bridges to an entity's own scheduler:

```kotlin
val y = entity.withEntity { it.position.blockY() }   // runs next tick on the entity's scheduler
```

`withEntity` throws `IllegalStateException` when the entity is removed or has no instance.
`Collection<T>.withEntities(block)` runs the block for every entity on its own scheduler and
collects the results in the order of the entities.

**The tick trap**: nothing dispatched to `Main` or an entity scheduler runs while the server starts
or stops. A plugin's `onLoad`/`onEnable`/`onDisable`, a feature's `load`/`enable`/`disable` and a
shutdown action must never wait for such work - launch it in a scope instead.

## `extension/` and `event/`

`extension/events.kt` has the reified `EventNode<Event>.addListener<T> { }` used everywhere.
`extension/instance.kt` has `buildInstance { }`. `extension/server.kt` exposes every
`MinecraftServer` manager and registry as a property (`ConnectionManager`, `SchedulerManager`,
`CommandManager`, `GlobalEventHandler`, the registries, ...), which is why example code reads
`ConnectionManager.onlinePlayers` rather than `MinecraftServer.getConnectionManager()`.
`extension/entity.kt` and `extension/registry.kt` cover entity metadata editing and registry
lookups.

`event/EventRegistrar` is a `fun interface` for grouping listeners so they can be handed to the
server:

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

(The doc comment on `EventRegistrar` writes `surfMinestomApi { listeners(...) }`; that name belongs
to the *Gradle* extension of a plugin project. The runtime function is `surfMinestomServer { }`.)
