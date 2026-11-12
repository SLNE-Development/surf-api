---
name: surf-api-minestom
description: Use when writing or building something that runs on Minestom against surf-api - deciding whether the thing being built is a Minestom plugin (SurfMinestomPlugin, minestom-plugin.json, dev.slne.surf.api.gradle.minestom) or a runnable server (surfMinestomServer { }, dev.slne.surf.api.gradle.minestom-server), switching server features on with withSignedChat, withLuckPerms, withSpark, withNpcLib, withPlayerVisibility, withConsole or withPlugins, working on the bootstrap agent, a mixin configuration, the gremlin dependency set and libraries/, the shaded configuration or how a built server is started, shaping the configuration phase, or reaching for a Minestom-only part of the api - mannequin NPCs, player visibility, async chat, block highlights, SurfMinestomPlayer, the plugin manager or the coroutine dispatchers.
---

# surf-api on Minestom

Everything below comes from `platform/minestom/`, `local/minestom-example/`,
`local/minestom-example-plugin/` and
`tooling/gradle-plugin/src/main/kotlin/dev/slne/surf/api/gradle/platform/minestom/`.

## Two different things

Minestom is a library, not a server you drop plugins into, so surf-api supports **two** shapes.
Get this straight before anything else, because almost every mistake here is a confusion between
them.

| | A plugin | A server |
| --- | --- | --- |
| What it is | a jar in a surf server's `plugins/`, or shaded into one | an executable jar that *is* the server |
| Gradle plugin | `dev.slne.surf.api.gradle.minestom` | `dev.slne.surf.api.gradle.minestom-server` |
| Extension | `surfMinestomApi { }` plus `minestomPluginFile { }` | `surfMinestomServerApi { }` |
| Coordinate pulled in | `surf-api-minestom` (`compileOnly`) | `surf-api-minestom-server` (`implementation`) |
| Entry point | a `SurfMinestomPlugin` subclass named in `minestom-plugin.json` | a `main` function calling `surfMinestomServer { }` |
| Owns the server config | no | yes - features, configuration phase, max players, shutdown |
| Reference | **`references/plugin.md`** | **`references/server.md`** |

A plugin is only loaded by a server that installed the plugins feature (`withPlugins()`). A server
without it reads nothing from `plugins/`, and `PluginManager.instance` throws
"This server does not load plugins".

| Directory | Coordinate | What it is |
| --- | --- | --- |
| `platform/minestom/api` | `surf-api-minestom-api` | the plugin-facing contracts |
| `platform/minestom/impl` | `surf-api-minestom-impl` | the runtime: `surfMinestomServer`, mixins, configuration phase |
| `platform/minestom/bootstrap-impl` | `surf-api-minestom-bootstrap-impl` | the `Launcher-Agent-Class` of a server jar |
| `platform/minestom/<feature>-impl` | `surf-api-minestom-<feature>-impl` | one optional server feature each |
| `aggregate/minestom` | `surf-api-minestom` | what a plugin compiles against; no code |
| `aggregate/minestom-server` | `surf-api-minestom-server` | what a server builds on; exposes `surf-api-minestom-impl` |
| `local/minestom-example` | not published | a server using every feature |
| `local/minestom-example-plugin` | not published | a plugin using only the plugin api |

The LuckPerms feature (`platform/minestom/luckperms-impl`) is only in the build once
`vendor/LuckPerms` has been generated - `git submodule update --init --recursive` and then
`cd vendor && ./gradlew applyPatches`. Otherwise `settings.gradle.kts` logs a warning and skips it
(and fails in CI). See the `surf-api-build` skill.

## Writing a plugin

```kotlin
class ExamplePlugin : SurfMinestomPlugin() {
    override suspend fun onLoad() {
        val file = dataDirectory.createDirectories().resolve("greeting.txt")
        logger.info("Loaded the greeting from {}", file)
    }

    override suspend fun onEnable() {
        eventNode.addListener<PlayerJoinEvent> { event ->
            event.player.sendMessage(Component.text("Hello!", NamedTextColor.GREEN))
        }
        scope.launch { /* ... */ }
    }

    override suspend fun onDisable() = Unit
}
```

The class needs a no-argument constructor or has to be an `object`, and is named as `main` in
`minestom-plugin.json`. `meta`, `eventNode`, `dataDirectory` (`plugins/<id>`), `logger` and `scope`
come from the base class. Listeners on `eventNode`, coroutines in `scope` and commands registered
during a lifecycle function or from `scope` are removed, cancelled or unregistered when the plugin
is disabled.

The lifecycle functions run **blocking** while the server starts and stops, so they must not wait
for work dispatched to `MinestomDispatchers.Main` or an entity scheduler - the server does not tick
yet while starting and no longer ticks while stopping. Launch that work in `scope` instead.

Details, including the Gradle plugin, the generated `minestom-plugin.json` and the class-path index:
**`references/plugin.md`**.

## Building a server

```kotlin
fun main() {
    val server = MinecraftServer.init(Auth.Online())
    val world = createWorld()

    surfMinestomServer {
        withConfigurationPhase()
        withSignedChat()
        withConsole()
        withPlugins()

        maxPlayers = 20
        listeners(ExampleListeners(world))
        onShutdown { LOGGER.info("stopped") }
    }

    ExampleCommands.register()
    server.start("0.0.0.0", 25565)
}
```

`surfMinestomServer { }` (`platform/minestom/impl/.../server/SurfMinestomServer.kt`) is called
**once**, after `MinecraftServer.init()` and before `MinecraftServer#start`. It loads and enables
every feature and every plugin before it returns, so the server is ready for players - and the
CommandAPI is ready, so commands can be registered right after it. It throws when the server is not
initialised, when the api is already running, or when Mixin is not installed (tests excepted).

Features are loaded in `loadPriority` order (ties in installation order), enabled in the same order
and disabled in reverse. A feature throwing in `load` or `enable` fails the whole call and disables
what was loaded; one throwing in `disable` is only logged.

The bootstrap agent, the `MinestomServerFeature` enum and how `withXxx()` relates to it, the mixin
configurations, the gremlin dependency set, the `shaded` configuration and how to start a built
server:
**`references/server.md`**.

## What only exists on Minestom

Paths relative to `platform/minestom/api/src/main/kotlin/dev/slne/surf/api/minestom/`.
Details and examples: **`references/minestom-features.md`**.

| Package | What it is for |
| --- | --- |
| `plugin/` | the plugin model: `SurfMinestomPlugin`, its `PluginMeta` (what `minestom-plugin.json` says), and `PluginManager.instance` to look other plugins up. Loaded by the plugins feature. |
| `player/` | `SurfMinestomPlayer`, the player class a surf server creates: it resolves Adventure's `PermissionChecker` pointer, completes game-mode switches the way vanilla does, and keeps the signed chat session in the player-list packet. Plus `PlayerLimit` and the surf player events (`AsyncPlayerCountEvent`, `AsyncPlayerSpawnLocationEvent`, `PlayerJoinEvent`, `PlayerQuitEvent`, `PlayerToggleFlightEvent`). |
| `chat/` | chat that Minestom has no api for: `AsyncPlayerChatEvent` (listeners run off the tick thread and may suspend or block), `ChatRenderer` / `ChatRenderer.viewerUnaware`, and signed-message handling (`sendSignedMessage`, `ChatSessionHolder`, `RemoteSignedMessage`). Needs `withSignedChat()`. |
| `visibility/` | `PlayerVisibility` - restrict what a player sees while the server limits how many players each player sees. Go through it rather than `Player#updateViewerRule`, which would drop the limit. Needs `withPlayerVisibility()`. |
| `npc/` | `mannequinNpc(name, instance, pos) { }` builds and spawns a `MannequinNpc` - a mannequin entity with a hologram, equipment, scale and click handlers, on top of StomNPCs. Needs `withNpcLib()`. |
| `highlight/` | `BlockHighlights.show(viewer, instance, position, color)` outlines one block for one player with a block display; `hide`/`hideAll` take it back. No feature needed. |
| `coroutine/` | `MinestomDispatchers.Main` (the next tick, with working `delay`) and `.Blocking` (virtual threads), the ready-made scopes `minestomScope` / `minestomAsyncScope` / `minestomBlockingScope`, and `entity.withEntity { }` to run on an entity's own scheduler and get its result back. |

Also Minestom-side: `extension/` (`addListener<T>`, `buildInstance`, and properties for every
`MinecraftServer` manager and registry), `permission/MinestomPermissions` with
`CommandSender.hasPermission(...)`, and `event/EventRegistrar` for grouping listeners.

## Where not to look here

Platform-independent, or with a skill of their own:

- `inventory/framework/` → **`surf-api-inventory`**
- `dialog/` → **`surf-api-dialogs`**
- `command/` (the Minestom CommandAPI port) → **`surf-api-commands`**
- messages, `Colors`, MiniMessage → **`surf-api-messages`**
- Configurate config classes and serializers → **`surf-api-config`**
- the surf event bus → **`surf-api-events`**
- beans and the bean container → **`surf-api-components`**
- module layout, Gradle paths and coordinates → **`surf-api-build`**

Minestom has no NMS layer - `surf-api-nms` is Paper only.
