---
name: surf-api-standalone
description: Use when surf-api has to run outside a Minecraft server - a plain JVM service, a daemon, a worker, a CLI or a test runtime - booting it with SurfApiStandaloneBootstrap and its bootstrap/enable/shutdown lifecycle, wiring surf-api-standalone into a Gradle project with dev.slne.surf.api.gradle.standalone, understanding why core module tests get the standalone runtime automatically, or deciding whether a part of the api works at all without a server and what happens when it does not.
---

# surf-api standalone

Sources: `platform/standalone/impl/`, `aggregate/standalone/`,
`tooling/gradle-plugin/src/main/kotlin/dev/slne/surf/api/gradle/platform/standalone/`,
`.../platform/SurfApiPlatform.kt` and `.../platform/core/CoreSurfPlugin.kt`.

## What it is for

Most of surf-api lives in `surf-api-core-api` and needs no Minecraft server: configuration,
messages and Adventure components, the bean container, the event bus, serializers, reflection, the
math and random helpers. But every one of those entry points reads its implementation out of the
bean container - `SurfApiCore.instance`, `surfConfigApi`, `SurfEventBus.INSTANCE` are all
`by surfBean<T>()` properties - and on Paper, Minestom and Velocity it is the platform plugin that
starts the root container and supplies the beans. Without a platform there is no container, and the
first read fails with `No bean provides ...`.

`surf-api-standalone` *is* that missing platform: it starts surf-api's root container, publishes a
minimal `SurfApiCore` bean and runs the `CoreInstance` lifecycle, for a JVM process that is not a
server. Two uses:

1. **A plain JVM service** - a daemon, a worker, an HTTP backend, a CLI - that wants surf-api's
   config, messages, beans and event bus.
2. **Tests.** `CoreSurfPlugin` adds `dev.slne.surf.api:surf-api-standalone:+` to `testRuntimeOnly`
   of every project using `dev.slne.surf.api.gradle.core` as long as testing is enabled, so core
   tests get a resolvable api without a server. `CoreSurfPluginTest` asserts exactly that, and the
   opposite for `testing { enabled.set(false) }`.

| Directory | Coordinate | What it is |
| --- | --- | --- |
| `platform/standalone/impl` | `surf-api-standalone-impl` | the bootstrap and the implementation |
| `aggregate/standalone` | `surf-api-standalone` | the coordinate downstream depends on |

There is no `platform/standalone/api`: the standalone platform adds no contracts of its own. The
impl module re-exports the whole platform surface through `api` and keeps ABI validation on, which
its own `build.gradle.kts` comment flags as temporary - "Separating the bootstrap from the
re-export is still open".

## `SurfApiStandaloneBootstrap`

`platform/standalone/impl/src/main/kotlin/dev/slne/surf/api/standalone/SurfApiStandaloneBootstrap.kt`
is an `object` with three suspending functions. There is no plugin, no descriptor and no agent -
the process calls them itself.

```kotlin
suspend fun main() {
    SurfApiStandaloneBootstrap.bootstrap()
    SurfApiStandaloneBootstrap.enable()

    // the api is ready here: configs, messages, beans, the event bus

    // only when you did not want the shutdown hook:
    // SurfApiStandaloneBootstrap.shutdown()
}
```

| Function | What it does |
| --- | --- |
| `bootstrap()` | clears the shutdown latch, installs a `NoopPacketEvents` as the PacketEvents api and `load()`s and `init()`s it, starts the root container (`SurfContainerHost.startRoot(SurfStandaloneInstance)`) and installs it with `SurfContainerAccess.install`, then runs `SurfStandaloneInstance.bootstrap()` |
| `enable(registerShutdownHook: Boolean = true)` | runs the container's `enable` phase, then `SurfStandaloneInstance.onLoad()` and `onEnable()`, and - unless told otherwise - registers a JVM shutdown hook that calls `shutdown()` in `runBlocking` |
| `shutdown()` | runs `SurfStandaloneInstance.onDisable()`, then `stopAll()` and `SurfContainerAccess.uninstall`, then terminates PacketEvents. Guarded by an `AtomicBoolean`, so it runs once no matter how it is reached |

Call `bootstrap()` before `enable()`: it is what makes the api resolvable at all, since the
container has to exist before the first `surfBean` read; the `CoreInstance.bootstrap()` step
initialises `PlayerSkinFetcher` and `Colors`, and `isBootstrapping()` stays true until `onLoad()`
clears it.

**One root and no children.** A standalone process has a single classloader, so the embedding
application's beans and surf-api's sit on one classpath and in one container - the root. Do not call
`SurfContainer.startChild`; annotating your own classes `@Service` is enough, and the container
picks them up from the same `META-INF/surf-api/beans/` merge. Container shutdown comes *after*
`onDisable`, so a bean holding a connection is still answering while everything that might write to
it shuts down.

`SurfStandaloneInstance` is nothing but `object SurfStandaloneInstance : CoreInstance()` - the
whole lifecycle is the one in `common/core/impl/.../core/server/CoreInstance.kt`, whose
`bootstrap`/`onLoad`/`onEnable`/`onDisable` are `@MustBeInvokedByOverriders`. `onEnable` registers
the core listeners (`CoreListenerManager` → `CorePacketListenerManager`) and `onDisable`
unregisters them.

Pass `registerShutdownHook = false` when the process already owns its shutdown sequence - a Spring
context, a test harness, an application framework - and call `shutdown()` from there. The parameter
is annotated `@IntroducedAt("3.52.0")`, so there is also a no-argument `enable()` overload for
callers compiled against an older api.

## What works and what does not

`SurfStandaloneApiImpl` (`.../standalone/impl/SurfStandaloneApiImpl.kt`) is a `@Service` bean
extending `SurfApiCoreImpl`, and is deliberately thin:

| Member | Standalone behaviour |
| --- | --- |
| `sendPlayerToServer(uuid, server)` | throws `UnsupportedOperationException("sendPlayerToServer is not supported in standalone mode")` |
| `getPlayer(uuid)` | throws `UnsupportedOperationException("getPlayer is not supported in standalone mode")` |
| `isPlayer(audience)` | always `false` |

It also declares `dataFolder = Path.of("api-data")` (relative to the working directory), but that
property is on the implementation class only, not on `SurfApiCore`, so reaching it means casting
`SurfApiCore.instance`.

**Available**, because it is all platform-independent and the implementations ship in
`surf-api-core-impl`, which `surf-api-standalone-impl` depends on at `api` scope:

- configuration - Configurate with the standalone `SpongeConfigSerializers`
  (`StandaloneSpongeConfigSerializers`, itself a `@Service` bean) → **`surf-api-config`**
- messages, component building, MiniMessage, `Colors`, pagination → **`surf-api-messages`**
- the bean container → **`surf-api-components`**
- the surf event bus → **`surf-api-events`**
- `SurfReflection`, the algorithms, collections, math and random helpers
- the Adventure stack (api, MiniMessage, the gson/legacy/plain/ansi serializers, the slf4j
  component logger), Guava, dazzleconf, spongepowered-math, commons-lang3/text, OkHttp, flogger and
  commons-math - all re-exported at `api` scope, so they are on your compile classpath

**Not available**, because they are a server's or a proxy's api:

- anything under `dev.slne.surf.api.paper.*` or `dev.slne.surf.api.minestom.*` - sidebars, PDC,
  packet listeners, NPCs, visibility, highlights, inventories, dialogs
- Bukkit, Minestom and Velocity types in general, and therefore everything taking a `Player`,
  `World`, `ItemStack` or `Instance`
- commands: `surf-api-standalone-impl` excludes `commandapi-core` and `brigadier` from its
  `surf-api-core-impl` dependency, so there is no command framework here
- PacketEvents is only a `NoopPacketEvents` - the surface exists, nothing is sent or received
- `sendPlayerToServer`, `getPlayer`, `isPlayer` as listed above
- `@ConditionalOnPlugin` / `@ConditionalOnAnyPlugin`: `StandaloneContainerPlatform.isPluginLoaded`
  always returns `false`, so a bean guarded that way is never created here. Use
  `@ConditionalOnProperty` or `@ConditionalOnEnvironment` for a switch a standalone process can
  actually flip - `environments()` goes through `EnvironmentConfig`, which also reads a `.env` file

A bean written against the core api works unchanged in standalone; one that touches a platform
type does not. That is the line to check when deciding whether code belongs in
`common/core` or in a platform module - see **`surf-api-build`**.

## Setting a project up

```kotlin
plugins {
    id("dev.slne.surf.api.gradle.standalone") version "3.0.0"
}

surfStandaloneApi {
    withCoreCommon()
}
```

`StandaloneSurfPlugin` is `AbstractCoreSurfPlugin<StandaloneSurfExtension>("standalone",
SurfApiPlatform.STANDALONE)` and `StandaloneSurfExtension` adds nothing to `CoreSurfExtension`, so
the whole configuration is the shared one: the plugins, repositories, Kotlin and Java setup, the
KSP processor, relocations and the `testing { }` block described in
`surf-api-paper/references/project-setup.md`.

The one standalone-specific detail is the scope. `SurfApiPlatform.STANDALONE` declares
`dev.slne.surf.api:surf-api-standalone:+` with scope **`api`**, not `compileOnly` as the server
platforms use - correctly so, since there is no plugin to supply the implementation at runtime; the
process has to carry it. `addSurfApiToClasspath(false)` or `surfApiScope("implementation")` change
that if the project assembles the dependency itself.

Because the extension name follows `surf<PlatformName>Api`, the block is `surfStandaloneApi { }`.

## Related skills

`surf-api-build` for module layout and coordinates; `surf-api-config`, `surf-api-messages`,
`surf-api-components` and `surf-api-events` for the feature areas that work here;
`surf-api-paper` and `surf-api-minestom` for the platforms that do have a server.
