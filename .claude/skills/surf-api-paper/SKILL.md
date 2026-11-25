---
name: surf-api-paper
description: Use when writing or building something that runs on Paper against surf-api - setting up a plugin project with the dev.slne.surf.api.gradle.paper-plugin Gradle plugin, configuring surfPaperPluginApi { }, working out what ends up in the generated paper-plugin.yml, the generated main class and bootstrapper and the bootstrap/load/enable/disable order of a plugin's beans, running on Folia or CanvasMC, or reaching for a part of the api that only exists on Paper - sidebars, block PDC, packet listeners and packet lore, the visualizer, glowing, the tick-thread guard, the PlaceholderAPI hook, permission registration, or smooth time skipping.
---

# surf-api on Paper

Everything below comes from `platform/paper/api/`, `platform/paper/impl/`,
`local/paper-plugin-test/` and
`tooling/gradle-plugin/src/main/kotlin/dev/slne/surf/api/gradle/platform/paper/`. Read those when
in doubt. `docs/spring-migration.md` records why the Spring integration works the way it does.

## The shape of the runtime

surf-api is itself a Paper plugin. `platform/paper/impl/build.gradle.kts` names it `surf-paper-api`
(`provides = ["SurfPaperAPI"]`), with `PaperMain` as `main` and `PaperBoostrapper` as
`bootstrapper` (the typo is the real class name). It requires `CommandAPI`, `packetevents` and
`LuckPerms` to load before it, and soft-depends on PlaceholderAPI, ProtocolLib, ProtocolSupport,
the Via stack and Geyser-Spigot. LuckPerms is a lazy root bean (`luckPerms`), because it enables
after surf-api's root context is built; inject `SurfLuckPerms`, `ObjectProvider<LuckPerms>` or a
`@Lazy` `LuckPerms`, never a plain `LuckPerms` constructor parameter. `Player.getLuckPermsUser()`,
`getLuckPermsUserOrNull()`, `getPrefixedName()` and `OfflinePlayer.loadLuckPermsUser()` wrap it;
Velocity has the same `Player` extensions.

Your plugin is a separate jar that compiles against `surf-api-paper` and declares `surf-paper-api`
as a required bootstrap and server dependency loading `BEFORE` it. surf-api builds a Spring context
for your plugin below its own root context, and your code lives in the beans of that context. The
Paper-only features are interfaces whose implementations are beans of surf-api's root context, so a
bean gets them by constructor injection. Where an interface has a `companion object` accessor
(`instance`), it is `@InternalSurfApi`, an error-level opt-in that plugins do not compile against.

surf-api's jar carries Spring, `kotlin-reflect` and `kotlinx-coroutines-reactor` under their own
package names, and Redis and NATS with Redisson, `io.nats` and BouncyCastle relocated. It does not
carry Guava, Gson or SLF4J: those are the server's.

| Directory | Coordinate | What it is |
| --- | --- | --- |
| `platform/paper/api` | `surf-api-paper-api` | the contracts you compile against |
| `platform/paper/impl` | `surf-api-paper-impl` | the `surf-paper-api` plugin itself; the server jar is `build/libs/*-all.jar` and a GitHub release asset, never a Maven artifact |
| `aggregate/paper` | `surf-api-paper` | the coordinate downstream depends on; no code |
| `platform/paper/nms/*` | `surf-api-paper-nms-*` | version bridges - see the `surf-api-nms` skill |
| `local/paper-plugin-test` | not published | the in-repo test plugin; best source of real usage |

Gradle paths are flat: `:surf-api-paper-impl`, never `:platform:paper:impl`. See the
`surf-api-build` skill.

## Setting a project up

```kotlin
plugins {
    id("dev.slne.surf.api.gradle.paper-plugin") version "4.0.0"
}

group = "dev.slne.example"
version = "1.0.0"

surfPaperPluginApi {
    authors.set(listOf("SLNE Development"))
}
```

The extension is named after the plugin: `CommonSurfPlugin.apply` creates
`surf${platformName.replaceFirstChar { it.uppercase() }}Api`, and the platform name of
`PaperPluginSurfPlugin` is `paperPlugin`, so the block is `surfPaperPluginApi { }` - there is no
`paperPlugin { }`. No setting is required. The build generates the main class and the
bootstrapper into the plugin's base package, the longest package all of its sources share.

For a Paper library or a module that is not a plugin, apply
`dev.slne.surf.api.gradle.paper-raw` instead and configure `surfRawPaperApi { }`; it has the same
settings minus the plugin-file ones and generates no classes.

What applying the plugin configures, the generated classes and their checks, what lands in
`paper-plugin.yml`, the library loader, Folia and CanvasMC, and the testing defaults:
**`references/project-setup.md`**.

## Entry point and lifecycle

The build generates two classes into the base package, and both carry no logic:

- `GeneratedSurfBootstrapper` extends `SurfPaperBootstrapper` (Paper's bootstrapper)
- `GeneratedSurfPlugin` extends `SurfPaperPlugin` (Paper's `main`)

Both base classes live in `platform/paper/api/.../paper/plugin/`, and their lifecycle methods are
`final`. They hand each phase to surf-api, which drives the plugin's Spring contexts and calls the
hooks of the plugin's beans. Plugin logic goes into beans that implement `SurfLifecycle` (package
`dev.slne.surf.api.spring`), as `local/paper-plugin-test/.../TestPluginLifecycle.kt` does:

```kotlin
import dev.slne.surf.api.spring.SurfLifecycle
import org.bukkit.plugin.Plugin
import org.springframework.stereotype.Service

@Service
class LobbyLifecycle(private val plugin: Plugin) : SurfLifecycle {

    override suspend fun onLoad() {
        plugin.logger.info("Lobby loaded")
    }

    override suspend fun onEnable() {
        plugin.logger.info("Lobby enabled")
    }

    override suspend fun onDisable() {
        plugin.logger.info("Lobby disabled")
    }
}
```

The phases, in order:

| Paper phase | What surf-api does for the plugin |
| --- | --- |
| bootstrap (`SurfPaperBootstrapper.bootstrap`) | builds the **bootstrap context**: only the components that implement `SurfBootstrapLifecycle` or carry `@BootstrapComponent`, then runs `onBootstrap` |
| load (`SurfPaperPlugin.onLoad`) | builds the **plugin context** with every other component, below the import context that holds its dependencies' exports and below the bootstrap context, then runs `onLoad` |
| enable (`SurfPaperPlugin.onEnable`) | registers the context's Bukkit `Listener` beans, its commands (`CommandAPICommand` and `CommandTree` beans, `CommandRegistrar` beans), its `@SurfEventHandler` beans and its `@PapiExpansion` beans, then runs `onEnable` |
| disable (`SurfPaperPlugin.onDisable`) | runs `onDisable` in reverse order, unregisters the `@SurfEventHandler` and `@PapiExpansion` beans and closes the contexts; Paper itself unregisters the Bukkit listeners |

Rules that follow from it:

- **Scanning.** The component scan starts at the bootstrapper's package and reads only the
  plugin's own jar. Annotate beans with Spring's stereotypes (`@Service`, `@Component`,
  `@Configuration`, from `org.springframework.stereotype` and
  `org.springframework.context.annotation`).
- **The bootstrap phase is restricted.** A plugin sees only the classes of its bootstrap
  dependencies and no server exists yet, so a bootstrap bean must not use the Bukkit server API,
  another plugin's API or the main and region threads.
- **Hooks block the server thread.** `onLoad`, `onEnable` and `onDisable` run on the thread Paper
  calls the phase on, which is the main thread. `PluginDispatchers.lifecycle` runs
  `PaperDispatchers.main` work inside the blocked phase, so a hook may wait for it; on Folia it must
  not wait for region or entity work, and it must never wait for a task handed to the Bukkit
  scheduler directly. A plugin whose load phase failed is never enabled.
- **The plugin instance is a bean.** The plugin context injects it under every type it has
  (`Plugin`, `JavaPlugin`, the concrete class). The bootstrap context injects it only as
  `org.bukkit.plugin.Plugin`, a proxy (`PluginInstanceHolder`) that forwards every call
  reflectively and throws before the load phase. Use it for registration, not in code that runs per
  tick; inject the instance in the plugin context there. Spring manages the instance
  like any other singleton: `@Autowired` members, `@PostConstruct`, every `BeanPostProcessor`, and
  `@PreDestroy` and `DisposableBean.destroy` when the context closes.
- **Every plugin context holds a `CoroutineScope`** (a `SupervisorJob` on `Dispatchers.Default`,
  cancelled after the disable hooks) and a `PaperDispatchers` bean with `main`, `region(...)`,
  `entity(entity)` and `async`. Inject them; they are the plugin's own.
- **`@Scheduled` works without `@EnableScheduling`.** Tasks start after the enable hooks and stop
  before the disable hooks. `scheduler = PaperSchedulers.GLOBAL_REGION` runs a method on
  `PaperDispatchers.main`; see Folia in `references/project-setup.md` and `surf-api-components`.

`@PluginApi` exports, `@ConditionalOnPlugin` adapters, qualifiers and the rest of the bean model are
in **`surf-api-components`**. Another surf plugin's exported beans are only visible when the build
script declares it with `surfPluginDependency(name)`.

A plugin that needs its own main class or bootstrapper names it with `mainClass(...)` or
`bootstrapper(...)`; it must extend the matching base class, which `checkSurfPluginClasses`
verifies. With a named bootstrapper, the scan starts at that class's package.

The api's own lifecycle, for reference: `PaperBoostrapper.bootstrap` installs NATS and Redis, builds
surf-api's root bootstrap context through `PaperSpringRuntime` and runs `PaperInstance.bootstrap()`.
`PaperMain.onLoad` starts bStats, builds the root context (`loadRoot`, which also installs it for
surf-api's static entry points) and runs `PaperInstance.onLoad()`. `onEnable` runs the root's enable
hooks, then `PaperInstance.onEnable()` (packet api, listeners).
`onDisable` closes the plugin contexts that are still open, tears surf-api down and closes the root
last. `PaperInstance` extends `CoreInstance` (`common/core/impl/.../core/server/CoreInstance.kt`),
whose `bootstrap`/`onLoad`/`onEnable`/`onDisable` are `@MustBeInvokedByOverriders`.

## `SurfApiPaper`

`platform/paper/api/.../paper/SurfApiPaper.kt` is the platform interface, a `SurfApiCore` with Paper
additions. Inject it:

```kotlin
@Service
class LobbyTransfer(private val api: SurfApiPaper) {

    suspend fun sendToLobby(player: Player) {
        if (api.isFolia) { /* ... */ }
        api.sendPlayerToServer(player, "lobby")
        api.skipTimeSmoothly(player.world, SkipOperations.NEXT_SUNRISE)
    }
}
```

`isFolia` and `isCanvasMc` are the platform probes; `skipTimeSmoothly` in its six overloads is the
`time/` feature.

## What only exists on Paper

Every path is relative to `platform/paper/api/src/main/kotlin/dev/slne/surf/api/paper/`.
Details and examples: **`references/paper-features.md`**.

| Package | What it is for |
| --- | --- |
| `sidebar/` | packet-based sidebars built with a Kotlin DSL - `sidebar(plugin) { }`, `autoUpdatingSidebar(plugin) { }`, `globalSidebar(plugin) { }`. Titles and lines render asynchronously per viewer, only changes are sent, and the sidebar closes itself when the plugin is disabled. |
| `pdc/` | persistent data containers on *blocks*, which Bukkit has no api for. `Block.pdc()` returns a `CustomBlockPersistentDataContainer`. |
| `packet/` | two things: `SurfPaperPacketListenerApi` registers raw clientbound/serverbound packet listeners (`@NmsUseWithCaution`), and `SurfPaperPacketApi` registers lore handlers that rewrite an item's lore on the way to the client without touching the real item. |
| `visualizer/` | block-display outlines for single locations, many locations or a whole area, shown to chosen viewers. `@ExperimentalVisualizerApi` - a warning-level opt-in. |
| `glow/` | `SurfGlowingApi` makes an entity, a block or a location glow for one viewer only, in a chosen `NamedTextColor`. |
| `region/` | the injected `TickThreadGuard`'s `ensureTickThread(...)` - asserts the current thread owns a position, chunk, entity, bounding box or the global tick, for code that has to be correct on Folia. |
| `hook/` | the PlaceholderAPI bridge: a `@PapiExpansion` bean with `@Placeholder` methods, registered with its plugin and present only while PlaceholderAPI is installed; hand-built expansions still register through `SurfPaperPAPIHook`. |
| `permission/` | `PermissionRegistry` - an abstract class whose `create(permission)` registers a Bukkit `Permission` with the plugin manager and returns the node string, so permission constants are declared and registered in one place. |
| `time/` | smooth time skipping: `SkipOperations` (NEXT_SUNRISE, NEXT_SUNSET, NEXT_DAY, NEXT_NOON, NEXT_NIGHT, NEXT_MIDNIGHT) and `TimeSkipResult`, driven through `skipTimeSmoothly` on the injected `SurfApiPaper`. |
| `coroutine/` | `PaperDispatchers`, `entity.withEntity { }` and `Int.ticks` - see Folia in `references/project-setup.md`. |

Smaller Paper-side helpers worth knowing: `extensions/` (`server`, `pluginManager`), `event/`
(`listen<T> { }`, `Listener.register()`), `util/` (`bukkit-util.kt` with `doInChunkAsync`,
`chunkX`/`chunkZ`, `position-util.kt`), `builder/item-stack.kt` and
`serializer/bukkit/` (Configurate serializers for `ItemStack`, `Location`, `BlockData`, players and
more).

## Where not to look here

These are platform-independent or have a skill of their own - do not reimplement them from the
Paper packages:

- `inventory/framework/` → **`surf-api-inventory`**
- `dialog/` → **`surf-api-dialogs`**
- `command/` → **`surf-api-commands`**
- messages, components and `Colors` → **`surf-api-messages`**
- `serializer/`, `@SurfConfig` classes and `ConfigHandle` → **`surf-api-config`**
- the surf event bus and `@SurfEventHandler` → **`surf-api-events`**
- beans, `SurfLifecycle`, `@PluginApi`, `@ConditionalOnPlugin` → **`surf-api-components`**
- `nms/` and the version bridges → **`surf-api-nms`**
- module layout, Gradle paths, `surf.module-*` plugins → **`surf-api-build`**
