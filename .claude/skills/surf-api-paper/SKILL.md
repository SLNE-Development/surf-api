---
name: surf-api-paper
description: Use when writing or building something that runs on Paper against surf-api - setting up a plugin project with the dev.slne.surf.api.gradle.paper-plugin Gradle plugin, configuring surfPaperPluginApi { }, working out what ends up in the generated paper-plugin.yml, writing the plugin main class and getting its load/enable/disable order right, running on Folia or CanvasMC, or reaching for a part of the api that only exists on Paper - sidebars and the legacy scoreboard, block PDC, packet listeners and packet lore, the visualizer, glowing, the tick-thread guard, the PlaceholderAPI hook, permission registration, or smooth time skipping.
---

# surf-api on Paper

Everything below comes from `platform/paper/api/`, `platform/paper/impl/`,
`local/paper-plugin-test/` and
`tooling/gradle-plugin/src/main/kotlin/dev/slne/surf/api/gradle/platform/paper/`. Read those when
in doubt.

## The shape of the runtime

surf-api is itself a Paper plugin. `platform/paper/impl/build.gradle.kts` names it `surf-paper-api`
(`provides = ["SurfPaperAPI"]`), with `PaperMain` as `main` and `PaperBoostrapper` as
`bootstrapper` (the typo is the real class name). It requires `CommandAPI`, `packetevents` and
`LuckPerms` to load before it, and soft-depends on PlaceholderAPI, ProtocolLib, ProtocolSupport,
the Via stack and Geyser-Spigot.

Your plugin is a separate jar that compiles against `surf-api-paper` and declares `surf-paper-api`
as a required dependency loading `BEFORE` it. Nothing of the api works before that plugin is
enabled: every Paper-only entry point resolves its implementation through
`requiredService<T>()` (`common/core/api/.../util/service-util.kt`), a `ServiceLoader` lookup
satisfied by `surf-api-paper-impl`.

| Directory | Coordinate | What it is |
| --- | --- | --- |
| `platform/paper/api` | `surf-api-paper-api` | the contracts you compile against |
| `platform/paper/impl` | `surf-api-paper-impl` | the `surf-paper-api` plugin itself |
| `aggregate/paper` | `surf-api-paper` | the coordinate downstream depends on; no code |
| `platform/paper/nms/*` | `surf-api-paper-nms-*` | version bridges - see the `surf-api-nms` skill |
| `local/paper-plugin-test` | not published | the in-repo test plugin; best source of real usage |

Gradle paths are flat: `:surf-api-paper-impl`, never `:platform:paper:impl`. See the
`surf-api-build` skill.

## Setting a project up

```kotlin
plugins {
    id("dev.slne.surf.api.gradle.paper-plugin") version "3.0.0"
}

group = "dev.slne.example"
version = "1.0.0"

surfPaperPluginApi {
    mainClass("dev.slne.example.lobby.LobbyPlugin")
    authors.set(listOf("SLNE Development"))
}
```

The extension is named after the plugin: `CommonSurfPlugin.apply` creates
`surf${platformName.replaceFirstChar { it.uppercase() }}Api`, and the platform name of
`PaperPluginSurfPlugin` is `paperPlugin`, so the block is `surfPaperPluginApi { }` - there is no
`paperPlugin { }`. `mainClass(...)` is the only required setting - `validate()` demands it with
"Main class must be set to your plugin's main class". Note that `CommonSurfPlugin` **catches** a
failed validation, logs `Failed to validate extension` and then skips the rest of its
`afterEvaluate` work, so a missing main class shows up as a plugin that was never configured rather
than as a build failure.

For a Paper library or a module that is not a plugin, apply
`dev.slne.surf.api.gradle.paper-raw` instead and configure `surfRawPaperApi { }`; it has the same
settings minus the plugin-file ones.

What applying the plugin configures, what lands in `paper-plugin.yml`, the library loader, Folia and
CanvasMC, and the testing defaults: **`references/project-setup.md`**.

## Entry point and lifecycle

A surf Paper plugin extends mccoroutine's `SuspendingJavaPlugin`, so the three lifecycle hooks are
suspending. From `local/paper-plugin-test/src/main/kotlin/.../PaperPluginMain.kt`:

```kotlin
class PaperPluginMain : SuspendingJavaPlugin() {
    override suspend fun onLoadAsync() {
        MyPluginConfig.init()
        surfComponentApi.load(this)
        TestInventoryView.register()
    }

    override suspend fun onEnableAsync() {
        SurfApiTestCommand().register()
        surfComponentApi.enable(this)
    }

    override suspend fun onDisableAsync() {
        CommandAPI.unregister("surfapitest")
        surfComponentApi.disable(this)
    }

    companion object {
        val instance: PaperPluginMain get() = getPlugin(PaperPluginMain::class.java)
    }
}

val plugin get() = PaperPluginMain.instance
```

The order that matters:

- **`onLoadAsync`** - configuration, `surfComponentApi.load(this)`, inventory view registration,
  packet listeners. Runs while the server is still loading plugins.
- **`onEnableAsync`** - command registration, `surfComponentApi.enable(this)`, listeners.
- **`onDisableAsync`** - `surfComponentApi.disable(this)`, unregister commands, close sidebars.

A top-level `val plugin get() = ...` is the repository's convention for reaching the plugin instance
from anywhere; the api itself does the same in `platform/paper/impl/.../PaperMain.kt`.

The api's own lifecycle, for reference: `PaperBoostrapper.bootstrap` runs
`PaperInstance.bootstrap()`, then `PaperMain.onLoadAsync` starts bStats and runs
`PaperInstance.onLoad()` (packet api, inventory framework), and `onEnableAsync` runs
`PaperInstance.onEnable()` (listeners, the `SurfApiPaper` implementation) plus the scoreboard
registry. `PaperInstance` extends `CoreInstance`
(`common/core/impl/.../core/server/CoreInstance.kt`), whose `bootstrap`/`onLoad`/`onEnable`/
`onDisable` are `@MustBeInvokedByOverriders`.

## `SurfApiPaper`

`platform/paper/api/.../paper/SurfApiPaper.kt` is the platform object, a `SurfApiCore` with Paper
additions. It is a companion-delegating interface, so the members are reachable statically:

```kotlin
if (SurfApiPaper.isFolia) { /* ... */ }
SurfApiPaper.sendPlayerToServer(player, "lobby")
val result = SurfApiPaper.skipTimeSmoothly(world, SkipOperations.NEXT_SUNRISE)
```

`isFolia` and `isCanvasMc` are the platform probes; `skipTimeSmoothly` in its six overloads is the
`time/` feature.

## What only exists on Paper

Every path is relative to `platform/paper/api/src/main/kotlin/dev/slne/surf/api/paper/`.
Details and examples: **`references/paper-features.md`**.

| Package | What it is for |
| --- | --- |
| `sidebar/` | packet-based sidebars built with a DSL - `sidebar(plugin) { }`, `autoUpdatingSidebar(plugin) { }`, `globalSidebar(plugin) { }`. Titles and lines render asynchronously per viewer, only changes are sent, and the sidebar closes itself when the plugin is disabled. |
| `scoreboard/` | the previous scoreboard api on top of scoreboard-library. `SurfScoreboardApi` is `@Deprecated` and points at `sidebar/`; it is still wired up in `PaperMain`, so existing code keeps working. Use `sidebar/` for anything new. |
| `pdc/` | persistent data containers on *blocks*, which Bukkit has no api for. `Block.pdc()` returns a `CustomBlockPersistentDataContainer`. |
| `packet/` | two things: `SurfPaperPacketListenerApi` registers raw clientbound/serverbound packet listeners (`@NmsUseWithCaution`), and `SurfPaperPacketApi` registers lore handlers that rewrite an item's lore on the way to the client without touching the real item. |
| `visualizer/` | block-display outlines for single locations, many locations or a whole area, shown to chosen viewers. `@ExperimentalVisualizerApi` - callers must opt in. |
| `glow/` | makes an entity, a block or a location glow for one viewer only, in a chosen `NamedTextColor`. |
| `region/` | `TickThreadGuard.ensureTickThread(...)` - asserts the current thread owns a position, chunk, entity, bounding box or the global tick, for code that has to be correct on Folia. |
| `hook/` | the PlaceholderAPI bridge: build a `PapiExpansion` out of `PapiPlaceholder`s and register it with `SurfPaperPAPIHook`. PlaceholderAPI is a soft dependency. |
| `permission/` | `PermissionRegistry` - an abstract class whose `create(permission)` registers a Bukkit `Permission` with the plugin manager and returns the node string, so permission constants are declared and registered in one place. |
| `time/` | smooth time skipping: `SkipOperations` (NEXT_SUNRISE, NEXT_SUNSET, NEXT_DAY, NEXT_NOON, NEXT_NIGHT, NEXT_MIDNIGHT) and `TimeSkipResult`, driven through `SurfApiPaper.skipTimeSmoothly`. |

Smaller Paper-side helpers worth knowing: `extensions/` (`server`, `pluginManager`), `util/`
(`bukkit-util.kt` with `doInChunkAsync`, `chunkX`/`chunkZ`, `position-util.kt`,
`reflection-remapper-util.kt`), `builder/item-stack.kt` and `serializer/bukkit/` (Configurate
serializers for `ItemStack`, `Location`, `BlockData`, players and more).

## Where not to look here

These are platform-independent or have a skill of their own - do not reimplement them from the
Paper packages:

- `inventory/framework/` → **`surf-api-inventory`**
- `dialog/` → **`surf-api-dialogs`**
- `command/` → **`surf-api-commands`**
- messages, components and `Colors` → **`surf-api-messages`**
- `serializer/`, Configurate config classes → **`surf-api-config`**
- `event/`, `listen<T>` → **`surf-api-events`**
- `surfComponentApi`, beans, `@Service` → **`surf-api-components`**
- `nms/` and the version bridges → **`surf-api-nms`**
- module layout, Gradle paths, `surf.module-*` plugins → **`surf-api-build`**
