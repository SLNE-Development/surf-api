# The Paper-only parts of surf-api

Every path is relative to `platform/paper/api/src/main/kotlin/dev/slne/surf/api/paper/`, the
implementations to `platform/paper/impl/src/main/kotlin/dev/slne/surf/api/paper/server/impl/`.
Working examples live in `local/paper-plugin-test/src/main/kotlin/.../command/subcommands/`.

Every service here is an interface whose implementation is a bean of surf-api's root Spring
context. A plugin bean gets it by constructor injection, as `TestPluginLifecycle` in the test plugin
does with `SurfPaperPacketListenerApi`. The extension functions (`Block.pdc()`, the sidebar DSL)
read the same beans. Either way they only work inside a running server with the `surf-paper-api`
plugin enabled.

## `sidebar/` - packet sidebars

Three entry points in `sidebar/sidebar-dsl.kt`, each taking the owning `Plugin`:

| Function | Returns | Viewers | Updates |
| --- | --- | --- | --- |
| `sidebar(plugin) { }` | `SurfViewerSidebar` | you add and remove them | you call `update()` |
| `autoUpdatingSidebar(plugin) { }` | `SurfAutoUpdatingSidebar` | you add and remove them | on `updateInterval` |
| `globalSidebar(plugin) { }` | `SurfGlobalSidebar` | every online player | on `updateInterval` |

The contract, from the doc comments on `SurfSidebar`:

- Titles and lines render **asynchronously per viewer** on a background dispatcher, not on the
  viewer's thread, and renderers may suspend. To touch state that needs the viewer's region thread,
  switch with `withContext(dispatchers.entity(viewer))`, using the plugin's injected
  `PaperDispatchers`.
- Rendering never overlaps for the same viewer; extra update requests are coalesced into one
  follow-up render, and only changed content is sent.
- A failing renderer is logged and that viewer keeps its previous content.
- The sidebar is active once created. It closes itself when the plugin is disabled, and drops
  viewers when they leave. `close()` is final: a closed sidebar (`isClosed`) cannot be shown again,
  so create a new one.
- One player should view one sidebar at a time - all sidebars share the single sidebar display
  slot.

`SidebarBuilder` (`sidebar/SidebarBuilder.kt`) distinguishes three kinds of content:

- static - `title(Component)`, `line(text, score = null)`, `emptyLine()`
- per viewer - `title { viewer -> }`, `line { viewer -> }`, `scoredLine { viewer -> SidebarLine }`,
  `lines { viewer -> List<SidebarLine> }`
- shared, rendered once per update for everyone - `sharedTitle { }`, `sharedLine { }`,
  `sharedLines { }`

plus `animatedLine(frames)` which advances one frame per update, and gradient lines. `maxLines` is
a `@Range(from = 1, to = 15) Int`; extra lines are discarded. `SidebarLine(text, score = null)` is
the line data class; a score is a fixed `Component`. The DSL is Kotlin-only: there are no Java
overloads. The `build*` variants `buildTitle`, `buildViewerTitle`, `buildSharedTitle`,
`buildLine`, `buildViewerLine`, `buildSharedLine` and `buildGradientLine` take a
`SurfComponentBuilder` block instead of a `Component` - see `surf-api-messages`.
`buildViewerLines` and `buildSharedLines` open a `SidebarLinesBuilder` instead, which offers
`line`, `buildLine`, `buildScoredLine` and `emptyLine`.

Adapted from `local/paper-plugin-test/.../subcommands/SidebarTest.kt`:

```kotlin
@Service
class LobbySidebar(private val plugin: Plugin, private val dispatchers: PaperDispatchers) {

    fun create(): SurfAutoUpdatingSidebar = autoUpdatingSidebar(plugin) {
        updateInterval = Tick.of(20).toKotlinDuration()
        maxLines = 12

        buildViewerTitle { viewer -> primary("Lobby "); variableValue(viewer.name) }
        buildViewerLines { viewer ->
            val height = withContext(dispatchers.entity(viewer)) { viewer.location.blockY }
            buildScoredLine({ info("Height") }, { variableValue(height) })
        }
        buildSharedLine { info("Online: "); variableValue(Bukkit.getOnlinePlayers().size) }
        separator()
    }
}

val bar = lobbySidebar.create()
bar.addViewer(player)
// ...
bar.close()
```

Pass your own plugin: the sidebar belongs to it and closes with it.

There is no `scoreboard/` package and no scoreboard-library (`net.megavex`) on the classpath; use
`sidebar/`. The sidebar sends its own packets and exposes no `ScoreboardLibrary`, so custom teams
and objectives are out of its scope. To port 3.x scoreboard code, follow section 18 of
`MIGRATION-4.0.md` in the repository root, which maps each scoreboard type to its sidebar
counterpart.

## `pdc/` - block persistent data

Bukkit has no persistent data container for a block; this adds one, stored per chunk.

```kotlin
import dev.slne.surf.api.paper.pdc.block.pdc

block.pdc().set(NamespacedKey(plugin, "owner"), PersistentDataType.STRING, player.name)
```

`CustomBlockPersistentDataContainer` is a `PersistentDataContainer` plus `chunk`,
`copyTo(block)` and `clear()`. `BlockPdcProvider.getPdc(block)` is the non-extension form.

The chunk has to be loaded and you have to be on its thread. `local/paper-plugin-test/.../
BlockPdcContainerTest.kt` goes through `world.doInChunkAsync(location.chunkX, location.chunkZ) { }`
from `dev.slne.surf.api.paper.util`, which is the pattern to copy on Folia.

## `packet/` - packet listeners and packet lore

Two independent things in one package.

**Raw packet listeners** (`packet/listener/`) are `@NmsUseWithCaution`: the handler signatures name
NMS packet and `ServerPlayer` types directly. A `PacketListener` is a marker interface; the methods
are found by the `@ClientboundListener` / `@ServerboundListener` annotations and return a
`PacketListenerResult`. From `local/paper-plugin-test/.../listener/ChatListener.java`:

```java
public class ChatListener implements PacketListener {
    @ServerboundListener
    public PacketListenerResult onChat(ServerboundChatPacket packet, ServerPlayer player) {
        return PacketListenerResult.CANCEL;
    }
}
```

Register it in a `SurfLifecycle.onLoad` hook through the injected `SurfPaperPacketListenerApi`, as
`TestPluginLifecycle` does; `unregisterListeners(listener)` takes it back:

```kotlin
@Service
@OptIn(NmsUseWithCaution::class)
class ChatBlocker(private val packetListeners: SurfPaperPacketListenerApi) : SurfLifecycle {

    override suspend fun onLoad() {
        packetListeners.registerListeners(ChatListener())
    }
}
```

See the `surf-api-nms` skill before touching anything here.

**Packet lore** (`packet/lore/`) rewrites the lore a client is shown without changing the item.
`SurfPaperPacketLoreHandler` is a `fun interface` whose `handleLore(loreToDisplay, pdc, itemStack)`
receives a *mutable* list that already holds the item's real lore plus whatever earlier handlers
added; inserting at index 0 puts your lines on top. Register through the injected
`SurfPaperPacketApi`:

```kotlin
packetApi.registerPacketLoreListener(plugin, key, handler, SurfPaperPacketLorePriority.LATE)
```

Handlers run in **ascending** priority order. `SurfPaperPacketLorePriority` provides `FIRST`
(`Short.MIN_VALUE`), `EARLY` (`-16384`), `NORMAL` (`0`), `LATE` (`16384`) and `LAST`
(`Short.MAX_VALUE`); a handler may carry its own default in `priority`, and the value passed at
registration wins. Every registration takes the owning `Plugin`, and
`unregisterPacketLoreListener(plugin)` removes all of that plugin's handlers;
`registerPacketLoreListenerGlobal(plugin, handler)` applies a handler to every item.

## `visualizer/` - block-display outlines

`SurfPaperVisualizerApi`, injected like the other services, is `@ExperimentalVisualizerApi`, a
warning-level `@RequiresOptIn`. A caller that does not opt in still compiles, with a warning; opt
in to silence it
(`@file:OptIn(ExperimentalVisualizerApi::class)`, as the tests do, or
`optIn.add("dev.slne.surf.api.paper.visualizer.visualizer.ExperimentalVisualizerApi")` in the
Kotlin compiler options, as `platform/paper/impl/build.gradle.kts` does).

| Factory | Visualizer |
| --- | --- |
| `createSingleLocationVisualizer(location)` | `SurfVisualizerSingleLocation` |
| `createMultiLocationVisualizer(world)` | `SurfVisualizerMultipleLocations` |
| `createAreaVisualizer(world, initialSettings = null, initialEdges = emptyList(), useHighestYBlock = false, placeDelay = Duration.ZERO)` | `SurfVisualizerArea` |

`getByUid(uid)` finds a live one. Every `SurfVisualizer` is `AutoCloseable` and has `uid`,
`startVisualizing()`, `stopVisualizing()`, `isVisualizing()`, `viewerUuids`, `addViewer(player)`,
`removeViewer(...)`. The block shown comes from `BlockDisplaySettings` (an NMS bridge type) via
`settings { blockData = ... }`. Example:
`local/paper-plugin-test/.../visualizer/SingleLocationVisualizerTest.kt`.

## `glow/` - per-viewer glowing

`SurfGlowingApi` makes a target glow for one viewer only:

```kotlin
@Service
class Highlighter(private val glowing: SurfGlowingApi) {

    fun mark(entity: Entity, viewer: Player) = glowing.makeGlowing(entity, viewer, NamedTextColor.RED)

    fun unmark(entity: Entity, viewer: Player) = glowing.removeGlowing(entity, viewer)
}
```

Overloads take an `Entity` (colour optional), a raw entity id with a team id and extra metadata
flags, a `Block` or a `Location` (colour required for the last two). Nothing is persisted - whoever
makes something glow is responsible for removing it.

## `region/` - tick-thread assertions

`TickThreadGuard` fails fast when code runs on the wrong thread, which is what makes Folia bugs
findable. With an injected `guard: TickThreadGuard`:

```kotlin
guard.ensureTickThread(entity, "updating the shop display")
guard.ensureTickThread(world, position, 2, "placing blocks")
guard.ensureGlobalTickThread("changing world time")
```

Overloads cover a position, a position with a block radius, a chunk by coordinates, an entity, a
`BoundingBox` and an x/z pair; `reason` is part of the failure message.

## `hook/` - PlaceholderAPI

PlaceholderAPI is a soft dependency of the api plugin. Build an expansion out of placeholders and
register it:

```kotlin
class CoinsPlaceholder : PapiPlaceholder("coins") {
    override fun parse(player: OfflinePlayer, args: List<String>) = coins(player).toString()
}

@Service
class CoinsExpansion(private val papiHook: SurfPaperPAPIHook) : SurfLifecycle {

    private val expansion = PapiExpansion("example", listOf(CoinsPlaceholder()))

    override suspend fun onEnable() = papiHook.register(expansion)

    override suspend fun onDisable() = papiHook.unregister(expansion)
}
```

`PapiExpansion(identifier, placeholder, author, version, name)` defaults `author` and `version`
from the calling plugin's `pluginMeta` and `name` to `identifier`. Both the expansion identifier and
every placeholder reject `%`, `{` and `_` in their constructor. `parseWithNoParams(player)` handles
the bare `%example%` form.

## `permission/` - permission nodes

`PermissionRegistry` is an abstract class; subclass it as an `object` so nodes are declared and
registered in one place. `create(permission)` registers a Bukkit `Permission` with the plugin
manager and returns the node string:

```kotlin
object LobbyPermissions : PermissionRegistry() {
    val BUILD = create("lobby.build")
    val FLY = create("lobby.fly")
}
```

The registration happens when the object is initialised, so touch it during enable.

## `time/` - smooth time skipping

`SurfApiPaper.skipTimeSmoothly(...)`, on the injected `SurfApiPaper`, has six suspending overloads: world + ticks, world + ticks +
duration, world + `SkipOperation`, and the same three for every world, which return
`Map<World, TimeSkipResult>`. `SkipOperations` holds the ready-made operations - `NEXT_SUNRISE`,
`NEXT_SUNSET`, `NEXT_DAY`, `NEXT_NOON`, `NEXT_NIGHT`, `NEXT_MIDNIGHT` - and
`SkipOperations.SkipOperation` is the functional interface for your own.

```kotlin
val result = api.skipTimeSmoothly(world, SkipOperations.NEXT_SUNRISE)
```
