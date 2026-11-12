# The Paper-only parts of surf-api

Every path is relative to `platform/paper/api/src/main/kotlin/dev/slne/surf/api/paper/`, the
implementations to `platform/paper/impl/src/main/kotlin/dev/slne/surf/api/paper/server/impl/`.
Working examples live in `local/paper-plugin-test/src/main/kotlin/.../command/subcommands/`.

All of these resolve out of surf-api's root bean container through a `by surfBean<T>()` property,
so they only work inside a running server with the `surf-paper-api` plugin enabled.

## `sidebar/` - packet sidebars

Three entry points in `sidebar/sidebar-dsl.kt`, each taking the owning `Plugin`:

| Function | Returns | Viewers | Updates |
| --- | --- | --- | --- |
| `sidebar(plugin) { }` | `SurfViewerSidebar` | you add and remove them | you call `update()` |
| `autoUpdatingSidebar(plugin) { }` | `SurfAutoUpdatingSidebar` | you add and remove them | on `updateInterval` |
| `globalSidebar(plugin) { }` | `SurfGlobalSidebar` | every online player | on `updateInterval` |

The contract, from the doc comments on `SurfSidebar`:

- Titles and lines render **asynchronously per viewer** on a background dispatcher, and renderers
  may suspend. To touch state that needs the viewer's region thread, switch with
  `withContext(plugin.entityDispatcher(viewer))`.
- Rendering never overlaps for the same viewer; extra update requests are coalesced into one
  follow-up render, and only changed content is sent.
- A failing renderer is logged and that viewer keeps its previous content.
- The sidebar closes itself when the plugin is disabled, and drops viewers when they leave.
- One player should view one sidebar at a time, including a legacy `SurfScoreboard` - they share
  the single sidebar display slot.

`SidebarBuilder` (`sidebar/SidebarBuilder.kt`) distinguishes three kinds of content:

- static - `title(Component)`, `line(text, score = null)`, `emptyLine()`
- per viewer - `title { viewer -> }`, `line { viewer -> }`, `scoredLine { viewer -> SidebarLine }`,
  `lines { viewer -> List<SidebarLine> }`
- shared, rendered once per update for everyone - `sharedTitle { }`, `sharedLine { }`,
  `sharedLines { }`

plus `animatedLine(frames)` which advances one frame per update, and gradient lines. `maxLines` is
a `@Range(from = 1, to = 15) Int`; extra lines are discarded. `SidebarLine(text, score = null)` is
the line data class. The `build*` variants (`buildTitle`, `buildLine`, `buildSharedLine`,
`buildViewerLine`, `buildViewerLines`, `buildScoredLine`, `buildGradientLine`) take a
`SurfComponentBuilder` block instead of a `Component` - see `surf-api-messages`.

Adapted from `local/paper-plugin-test/.../subcommands/SidebarTest.kt`:

```kotlin
val bar = autoUpdatingSidebar(plugin) {
    updateInterval = Tick.of(20).toKotlinDuration()
    maxLines = 12

    buildViewerTitle { viewer -> primary("Lobby "); variableValue(viewer.name) }
    buildViewerLines { viewer ->
        val state = withContext(plugin.entityDispatcher(viewer)) { viewer.location.blockY }
        buildScoredLine({ info("Height") }, { variableValue(state) })
    }
    buildSharedLine { info("Online: "); variableValue(Bukkit.getOnlinePlayers().size) }
    separator()
}

bar.addViewer(player)
// ...
bar.close()
```

## `scoreboard/` - the previous api

`SurfScoreboardApi` is `@Deprecated`, with
`ReplaceWith("SurfSidebar", "dev.slne.surf.api.paper.sidebar.SurfSidebar")`. Its deprecation message
is explicit about the difference: the sidebar api sends its own packets and exposes no
`ScoreboardLibrary` instance, while `SurfScoreboardApi.scoreboardLibrary()` does. It is still
created and enabled by `PaperMain`, so existing callers keep working - but write new code against
`sidebar/`.

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

Register in `onLoadAsync` with `SurfPaperPacketListenerApi.registerListeners(ChatListener())`.
See the `surf-api-nms` skill before touching anything here.

**Packet lore** (`packet/lore/`) rewrites the lore a client is shown without changing the item.
`SurfPaperPacketLoreHandler` is a `fun interface` whose `handleLore(loreToDisplay, pdc, itemStack)`
receives a *mutable* list that already holds the item's real lore plus whatever earlier handlers
added; inserting at index 0 puts your lines on top. Register through `SurfPaperPacketApi`:

```kotlin
SurfPaperPacketApi.registerPacketLoreListener(plugin, key, handler, SurfPaperPacketLorePriority.LATE)
```

Handlers run in **ascending** priority order. `SurfPaperPacketLorePriority` provides `FIRST`
(`Short.MIN_VALUE`), `EARLY` (`-16384`), `NORMAL` (`0`), `LATE` (`16384`) and `LAST`
(`Short.MAX_VALUE`); a handler may carry its own default in `priority`, and the value passed at
registration wins. Always use the overloads that take an explicit `Plugin` - the doc comment says
the caller detection used by the deprecated ones is unreliable - because
`unregisterPacketLoreListener(plugin)` cleans up by that reference.

## `visualizer/` - block-display outlines

`SurfPaperVisualizerApi` is `@ExperimentalVisualizerApi`; a caller has to opt in
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
SurfGlowingApi.makeGlowing(entity, viewer, NamedTextColor.RED)
SurfGlowingApi.removeGlowing(entity, viewer)
```

Overloads take an `Entity` (colour optional), a raw entity id with a team id and extra metadata
flags, a `Block` or a `Location` (colour required for the last two). Nothing is persisted - whoever
makes something glow is responsible for removing it.

## `region/` - tick-thread assertions

`TickThreadGuard` fails fast when code runs on the wrong thread, which is what makes Folia bugs
findable:

```kotlin
TickThreadGuard.ensureTickThread(entity, "updating the shop display")
TickThreadGuard.ensureTickThread(world, position, blockRadius = 2, "placing blocks")
TickThreadGuard.ensureGlobalTickThread("changing world time")
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

SurfPaperPAPIHook.register(PapiExpansion("example", listOf(CoinsPlaceholder())))
```

`PapiExpansion(identifier, placeholder, author, version, name)` defaults `author` and `version`
from the calling plugin's `pluginMeta` and `name` to `identifier`. Both the expansion identifier and
every placeholder reject `%`, `{` and `_` in their constructor. `parseWithNoParams(player)` handles
the bare `%example%` form. Unregister with `SurfPaperPAPIHook.unregister(expansion)`.

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

`SurfApiPaper.skipTimeSmoothly(...)` has six suspending overloads: world + ticks, world + ticks +
duration, world + `SkipOperation`, and the same three for every world, which return
`Map<World, TimeSkipResult>`. `SkipOperations` holds the ready-made operations - `NEXT_SUNRISE`,
`NEXT_SUNSET`, `NEXT_DAY`, `NEXT_NOON`, `NEXT_NIGHT`, `NEXT_MIDNIGHT` - and
`SkipOperations.SkipOperation` is the functional interface for your own.

```kotlin
val result = SurfApiPaper.skipTimeSmoothly(world, SkipOperations.NEXT_SUNRISE)
```
