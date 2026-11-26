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
`PacketListenerResult`. From `local/paper-plugin-test/.../listener/ChatListener.kt`:

```kotlin
class ChatListener : PacketListener {
    @ServerboundListener
    fun onChat(packet: ServerboundChatPacket, player: ServerPlayer): PacketListenerResult {
        return PacketListenerResult.CANCEL
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

## `inventory/insertion/` - item insertion

An insertion plan answers how much of an item fits into an inventory before anything changes, for
example to tell a player whether a reward or a purchase fits:

```kotlin
val plan = player.planInsertion(ItemStack.of(Material.DIAMOND), 200)

if (!plan.fits) {
    player.sendText {
        appendErrorPrefix()
        error("Free up space for ")
        variableValue(plan.leftover)
        error(" more items.")
    }
    return
}

val result = plan.commit()
```

- `Inventory.planInsertion(item, amount)` works on any Bukkit inventory; `Player.planInsertion` is
  the shortcut for the player's inventory. The amount of `item` itself is ignored, and `amount`
  may exceed one stack.
- Several items are planned as one set: `planInsertion(vararg items)` and
  `planInsertion(items: Collection<ItemStack>)` count each stack's own amount,
  `planInsertion(items: Map<ItemStack, Int>)` the amount each key maps to. The set fits only when
  everything fits together. Similar items merge into one entry; `entries` and `leftoverOf(item)`
  give the leftover per item, `leftover` the total.

  ```kotlin
  val plan = player.planInsertion(mapOf(sword to 1, arrows to 128))
  val missingArrowSpace = plan.leftoverOf(arrows)
  ```

- On a player inventory, only the storage slots count: armor, offhand and crafting grid stay
  untouched. `includeOffhand = true` on the `Player` overloads lets the offhand take what the
  storage slots leave over. Every other inventory uses all its slots.
- `fits` and `leftover` describe the inventory when the plan was made. The plan runs Bukkit's own
  `addItem` on a copy of the slots in scope, so similarity follows `ItemStack.isSimilar` and each
  item's own max stack size, including the `MAX_STACK_SIZE` data component.
- `commit(policy)` inserts into the current inventory under an `ItemInsertionPolicy`:
  - `REJECT` (default) plans again and inserts nothing when the items no longer fit together.
  - `PARTIAL` inserts what fits.
  - `DROP` inserts what fits and drops the rest so that it never despawns: at the player, owned by
    the player, or at the center of the inventory's location without an owner. An inventory without
    a location, such as one from `Bukkit.createInventory`, throws `IllegalStateException` before
    anything changes, even when everything fits.

  The `ItemInsertionResult` carries `inserted` and `leftover` in total, per item in `entries`, and
  through `insertedOf(item)` and `leftoverOf(item)`. Under `DROP`, the leftover is what was dropped.
- Planning and committing run on the thread that owns the inventory: the holding entity's thread,
  the region thread of the inventory's location, or the global tick thread for an inventory with
  neither. Anywhere else they throw `IllegalStateException` through `TickThreadGuard`.
- The suspending variants switch to that thread themselves: `planInsertionAsync` with the same
  eight overloads as `planInsertion`, and `commitAsync(policy)` on the plan. A whole flow then reads
  as one coroutine of the plugin, started from any thread:

  ```kotlin
  scope.launch {
      val plan = player.planInsertionAsync(reward, 64)
      if (!plan.fits) return@launch

      plan.commitAsync(ItemInsertionPolicy.DROP)
  }
  ```

  They switch through the calling plugin's `PaperDispatchers`, so they need a coroutine of a surf
  plugin, such as one of its injected `CoroutineScope` or a suspending command executor, and throw
  `IllegalStateException` in any other coroutine. A coroutine cancelled before the commit starts
  leaves the inventory unchanged; a commit that has started completes. When the entity that holds
  the inventory is removed first, the call fails with a `CancellationException`, as `withEntity`
  does.

## `inventory/removal/` - item removal

A removal plan answers whether an inventory holds enough of one or more items before anything
changes, for example to check whether a player can pay a price or a crafting cost:

```kotlin
val plan = player.planRemoval(ItemStack.of(Material.EMERALD), 32)

if (!plan.isAvailable) {
    player.sendText {
        appendErrorPrefix()
        error("You lack ")
        variableValue(plan.missing)
        error(" emeralds.")
    }
    return
}

val result = plan.commit()
```

- `planRemoval` has the same four overloads as `planInsertion`: one item and an amount, `vararg`
  items, a `Collection<ItemStack>` and a `Map<ItemStack, Int>`. The amount may be spread over any
  number of stacks. The set is available only when the inventory holds every item in full;
  `entries` and `missingOf(item)` give the missing amount per item, `missing` the total.
- Similarity follows `ItemStack.isSimilar`: an item with a different name, enchantment or other
  data does not count.
- On a player inventory, only the storage slots count. The `Player` overloads take
  `includeOffhand = true` and `includeArmor = true` to count the offhand and the worn armor as
  well; a commit takes from the storage slots first, then the offhand, then the armor. Every other
  inventory uses all its slots.
- `isAvailable` and `missing` describe the inventory when the plan was made. Planning runs Bukkit's
  own `removeItem` on a copy of the slots in scope; committing runs it on the inventory itself.
- `commit(policy)` removes from the current inventory under an `ItemRemovalPolicy`:
  - `REJECT` (default) checks again with `containsAtLeast` and removes nothing when the inventory
    no longer holds every item in full.
  - `PARTIAL` removes what is there, up to each item's amount.

  The `ItemRemovalResult` carries `removed` and `missing` in total, per item in `entries`, and
  through `removedOf(item)` and `missingOf(item)`.
- The thread rule of item insertion applies: plan and commit on the thread that owns the
  inventory, or they throw `IllegalStateException`. `planRemovalAsync` and `commitAsync(policy)`
  switch to that thread themselves, with the same rules as for item insertion:

  ```kotlin
  scope.launch {
      val plan = player.planRemovalAsync(price, 16)
      if (!plan.isAvailable) return@launch

      plan.commitAsync()
  }
  ```

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

PlaceholderAPI is a soft dependency of the api plugin. Declare an expansion by annotating a class
with `@PapiExpansion` and its placeholder methods with `@Placeholder`:

```kotlin
@PapiExpansion("example", placeholders = [RankPlaceholders::class])
class CoinsExpansion(private val coins: CoinService) : PapiExpansionFallback {

    @Placeholder("coins")
    fun coins(player: OfflinePlayer?): String? = player?.let { coins.balance(it).toString() }

    @Placeholder("top")
    fun top(player: Player, args: String): Component = coins.topLine(player, args)

    override fun onRequest(player: OfflinePlayer?, params: String): String? = null
}
```

- `@PapiExpansion` is meta-annotated with `@Component` and `@ConditionalOnPlugin("PlaceholderAPI")`:
  the class is scanned like any bean and does not exist without PlaceholderAPI.
- `PaperSpringRuntime` registers the expansion beans of the plugin's full context in the enable
  phase, before the `onEnable` hooks, and unregisters them on disable
  (`server/hook/papi/PapiExpansionBeans.kt`).
- `%example_<name>_<args>%` calls the method named `<name>`. The first parameter is `OfflinePlayer?`
  (every request, `null` without a player), a non-null Kotlin `OfflinePlayer` (requests with a
  player only) or `Player` (online players only). A skipped request resolves to `null` without a
  call. The optional second `String` parameter gets everything after
  `<name>_`, unsplit. The return type is `String?` or `Component?`; a component is serialized to
  legacy section-sign text.
- `%example_%` or an unknown name goes to `PapiExpansionFallback.onRequest` when the annotated
  class implements it, and resolves to `null` otherwise.
- `placeholders` lists further classes with `@Placeholder` methods. A Kotlin `object` contributes
  its instance, and a class with a bean in the plugin's context (ancestors included) contributes
  that bean; several matching beans fail the enable with `NoUniqueBeanDefinitionException`. Any
  other class is created through the context with its constructor injected, and destroyed on
  disable (`PapiExpansionRegistration.createdInstances`).
- A blank `author` or `version` uses the plugin's `pluginMeta`. Identifiers and placeholder names,
  annotated or hand-built, must not be empty and reject `%`, `{`, `}` and `_`
  (`requirePlaceholderName`); duplicate names, invalid signatures and `suspend` methods fail the
  plugin's enable.
- Methods are called through hidden-class invokers (`PlaceholderInvokers`), on whatever thread
  PlaceholderAPI resolves the placeholder, so they must be thread-safe.

Expansions built by hand still go through `SurfPaperPAPIHook`:

```kotlin
class CoinsPlaceholder : PapiPlaceholder("coins") {
    override fun parse(player: OfflinePlayer, args: List<String>) = coins(player).toString()
}

papiHook.register(PapiExpansion("example", listOf(CoinsPlaceholder())))
```

`expansion.PapiExpansion(identifier, placeholder, author, version, name)` defaults `author` and
`version` from the calling plugin's `pluginMeta`. Its placeholders get the request's arguments split
at `_`, `parseWithNoParams(player)` handles `%example_%`, and requests without a player resolve to
`null`. PlaceholderAPI never resolves `%example%` without an underscore.

`SurfPaperPAPIHookImpl` is a root `SurfLifecycle` bean: it attaches registrations to PlaceholderAPI
in surf-api's `onEnable` (or right away once enabled) and detaches all of them in `onDisable`. One
lock guards the registry; identifiers are unique, ignoring case, across annotated and hand-built
expansions. PlaceholderAPI fires a synchronous Bukkit event on registration, so registration
runs on the main thread.

The hook injects an `ExpansionHolderFactory`, which connects each registration to PlaceholderAPI.
Two root beans provide it (`server/hook/papi/ExpansionHolderFactories.kt`):
`PapiExpansionHolderFactory` carries `@ConditionalOnPlugin("PlaceholderAPI")` and creates
`PlaceholderExpansion`s; `NoopExpansionHolderFactory` is the `@Fallback` whose holders do nothing.
No PlaceholderAPI type loads without PlaceholderAPI.

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
