# Building a runnable surf Minestom server

Sources:
`tooling/gradle-plugin/src/main/kotlin/dev/slne/surf/api/gradle/platform/minestom/server/`,
`platform/minestom/impl/src/main/kotlin/dev/slne/surf/api/minestom/server/`,
`platform/minestom/bootstrap-impl/src/main/java/dev/slne/surf/api/minestom/server/bootstrap/` and
`local/minestom-example/`.

## The Gradle plugin

```kotlin
plugins {
    id("dev.slne.surf.api.gradle.minestom-server") version "3.0.0"
}

surfMinestomServerApi {
    mainClass("dev.slne.example.lobby.MainKt")

    withSignedChat()
    withLuckPerms()
    withConsole()
    withPlugins()

    pluginDependencies {
        register("surf-core-minestom")
    }
}
```

`mainClass(...)` is not enforced: without it the plugin only logs
`"No main class set for the Minestom server <path>; call mainClass(...) in surfMinestomServerApi { }"`.
It is written both into the `application` plugin's `mainClass` and into the jar manifest's
`Main-Class`.

On top of the common `CommonSurfPlugin` setup (see
`surf-api-paper/references/project-setup.md`), this plugin applies
`org.gradle.application` and `xyz.jpenilla.gremlin-gradle`, adds the Minestom snapshot repository,
and adds `net.minestom:minestom` at `implementation` plus `log4j-core` and `log4j-slf4j2-impl` at
`runtimeOnly`. The surf dependency is `surf-api-minestom-server` at **`implementation`** (not
`compileOnly` as for a plugin), and `surf-api-minestom-bootstrap-impl` is added too.

The shadow jar gets `duplicatesStrategy = INCLUDE`, `mergeServiceFiles()`, Log4j's
`Log4j2PluginsCacheFileTransformer` and `Multi-Release: true` - log4j-core and its add-ons each
ship a plugin cache which has to reach the transformer to be merged instead of dropped.

## Features

`MinestomServerFeature` (`MinestomServerSurfExtension.kt`) is the complete list, with the module it
lives in, the delegate agent it needs and the mixin configuration it ships:

| Enum constant | `withXxx()` | Module | Agent / mixin config |
| --- | --- | --- | --- |
| `SIGNED_CHAT` | `withSignedChat()` | `surf-api-minestom-signed-chat-impl` | - |
| `LUCKPERMS` | `withLuckPerms()` | `surf-api-minestom-luckperms-impl` | agent `me.lucko.luckperms.minestom.dependencies.LuckPermsAgent` |
| `SPARK` | `withSpark()` | `surf-api-minestom-spark-impl` | - |
| `NPC` | `withNpcLib()` | `surf-api-minestom-npc-impl` | mixin `mixins.surf-api-minestom-server-npc.json` |
| `PLAYER_VISIBILITY` | `withPlayerVisibility()` | `surf-api-minestom-player-visibility-impl` | - |
| `CONSOLE` | `withConsole()` | `surf-api-minestom-console-impl` | - |
| `PLUGINS` | `withPlugins()` | `surf-api-minestom-plugins-impl` | - |

**Two steps, and they are easy to mix up.** `withXxx()` in `surfMinestomServerApi { }` (Gradle) only
puts the feature's module on the class path. Installing it at runtime is a separate
`withXxx(...)` call inside `surfMinestomServer { }` (Kotlin), which each feature module ships as an
extension on `SurfMinestomServerBuilder`. Both are needed. `withAllFeatures()` covers the Gradle
side for every feature at once.

The runtime installers, with their configuration blocks:

```kotlin
surfMinestomServer {
    withSignedChat()                 // SignedChatSettings
    withLuckPerms()                  // LuckPermsFeatureBuilder
    withSpark()                      // SparkFeatureBuilder
    withNpcLib()                     // no settings
    withPlayerVisibility { maxVisible = 10; activateAt = 15; deactivateBelow = 8 }
    withConsole()                    // ConsoleFeatureBuilder: threadName, onShutdown
    withPlugins()                    // PluginsFeatureBuilder, below
}
```

`PluginsFeatureBuilder` has `directory` (default `plugins`), `loadClassPathPlugins` (default `true`,
so plugin modules shaded into the same jar are loaded as well) and `requireAll` (default `false`).
When the server declares `pluginDependencies` and `directory` is changed, start it with
`-Dsurf.minestom.plugins=<dir>` too, so the bootstrap reads the same directory.

Writing a feature of your own: implement `SurfMinestomFeature` (`id`, `loadPriority`, `load`,
`enable`, `disable`) and `install(...)` it. Features load by ascending `loadPriority` (ties in
installation order), enable in that order, disable in reverse.

## `surfMinestomServer { }`

`SurfMinestomServerBuilder` also carries the server-wide settings:

| Member | What it does |
| --- | --- |
| `eventNodeName` | name of the api's event node, a child of the global handler. Default `"surf-api-minestom"` |
| `maxPlayers` | `Int?`; `null` means unlimited. Enforced through `AsyncPlayerCountEvent` |
| `recommendedServerProperties` | default `true`: sets `minestom.keep-alive-delay` to 2s (needs the surf bootstrap to make the property writable) and turns packet compression off, for a server behind a proxy |
| `listeners(vararg EventRegistrar)` | registers listener groups on the api's node |
| `permissions(provider)` | a `PermissionProvider`, e.g. for command requirements; takes precedence over one a feature installed |
| `disableEntityTicking(vararg EntityType)` | stops those entities ticking. Needs the surf Mixin agent; rejects `EntityType.PLAYER`; entity schedulers keep running |
| `playerFactory(factory)` | creates every player; default `::SurfMinestomPlayer`. While the api runs, Minestom's own `setPlayerProvider` is rejected |
| `withConfigurationPhase { }` | replaces Minestom's configuration phase with the ordered, editable one |
| `onShutdown { }` | runs after Minestom stopped and features and plugins are disabled |
| `install(feature)` / `isInstalled(id)` | the raw feature calls |

The returned `SurfMinestomServer` (also `SurfMinestomServer.instance`) has `eventNode`, `features`,
`feature(id)` and three ways to stop:

- `shutdown()` - disables features and the api only. Runs automatically when Minestom stops.
- `stop()` - stops Minestom, which shuts the api down, then runs the `onShutdown` actions. Blocks,
  so calling it from a tick thread (e.g. straight out of a command executor) throws
  `IllegalStateException`. The process keeps running afterwards.
- `exit(status = 0)` - does `stop()` plus log flush plus JVM exit, on a new non-daemon thread named
  `surf-minestom-exit`, so it is safe from any thread including a tick thread and the console.

`exitFlushingLogs(status)` (`platform/minestom/impl/.../server/Exit.kt`) is the blocking form the
console feature uses by default.

## The configuration phase

`withConfigurationPhase { }` replaces Minestom's configuration phase with an ordered list of steps
that can each be replaced, removed or surrounded. `ConfigurationTasks` holds the ids in the order
vanilla runs them: `PLAYER_COUNT`, `BRAND`, `CONFIGURATION_EVENT`, `SPAWN_LOCATION`,
`ENABLED_FEATURES`, `SYNCHRONIZE_REGISTRIES`, `AWAIT_SETTINGS`, `RESOURCE_PACK`, `JOIN_WORLD`.

```kotlin
withConfigurationPhase {
    codeOfConduct { player -> "Hello ${player.username}! Please be nice." }
    replace(ConfigurationTasks.RESOURCE_PACK) { context -> myPacks.await(context.player) }
    after(ConfigurationTasks.AWAIT_SETTINGS, ConfigurationTaskId("load_profile")) { context ->
        profiles.load(context.player.uuid)
    }
}
```

`replace(id, task)` and `remove(id)` require the step to exist; `before`/`after` take an anchor plus
a new `ConfigurationTaskId`. Steps run on the player's virtual configuration thread, so they may
suspend or block. `ids` is the current order.

## The bootstrap agent

`SurfMinestomBootstrap` (`platform/minestom/bootstrap-impl/.../SurfMinestomBootstrap.java`) is the
jar's `Launcher-Agent-Class`. In order it:

1. reads `dependencies.txt` from the jar root - the gremlin dependency set - downloads those
   libraries into `libraries/` on the first start and appends them to the system class path before
   `main` runs. `-Dsurf.minestom.libraries=<dir>` overrides the directory.
2. appends the plugins named in `Surf-Plugin-Dependencies` to the system class path
   (`ServerPlugins`).
3. makes `minestom.keep-alive-delay` writable at runtime, and turns Minestom's own
   `minestom.shutdown-on-signal` hook off unless it was set with `-D` - the surf api stops the
   server from its own hook, which also runs the shutdown actions.
4. sets `surf.minestom.server-jar` to the jar path, so an agent loaded from a downloaded library
   still finds the server jar's manifest.
5. calls the delegate agents listed in `Surf-Delegate-Agent-Classes`, in order.

Manifest attributes the Gradle plugin writes:

| Attribute | Contents |
| --- | --- |
| `Main-Class` | `mainClass(...)` |
| `Launcher-Agent-Class` | `dev.slne.surf.api.minestom.server.bootstrap.SurfMinestomBootstrap` |
| `Surf-Delegate-Agent-Classes` | `SurfMixinAgent` first, then each installed feature's `agentClass` |
| `Surf-Mixin-Configs` | `mixins.surf-api-minestom-server.json`, then each feature's config, then the project's |
| `Surf-Plugin-Dependencies` | the enabled `pluginDependencies`, optional ones suffixed `?` |
| `Multi-Release` | `true` |

`SurfMixinAgent` must be first: its transformer has to exist before any other agent loads a Minestom
class.

## Mixins

The api's own configuration is `mixins.surf-api-minestom-server.json`
(`platform/minestom/impl/src/main/resources/`): package
`dev.slne.surf.api.minestom.server.mixin`, `compatibilityLevel` `JAVA_25`, `required: true`,
`defaultRequire: 1`, with `CommandManagerMixin`, `ConnectionManagerMixin`, `EntityTickMixin`,
`EntityTrackerEntryMixin`, `EntityTrackerMixin`, `LightingChunkSkylightMixin` and `ServerMixin`.
The NPC feature ships `mixins.surf-api-minestom-server-npc.json`.

`SurfMixinAgent` (`platform/minestom/impl/src/main/java/.../impl/mixin/SurfMixinAgent.java`) reads
the configurations out of `Surf-Mixin-Configs` and additionally applies surf's *compatibility*
configurations - today `mixins.surf-api-minestom-server-miniplaceholders.json` - on its own, for
every library whose marker class is on the system class path. Those need no manifest entry, and
their injections are not required, so a library version that no longer needs the fix is simply left
untransformed. A manifest listing no configuration at all fails the installation, as does a
required mixin whose target class was already loaded; a transformation that fails later leaves the
class untransformed and is reported by `failedTransformations()`. Mixin is installed once per JVM.

To add mixins of your own:

```kotlin
surfMinestomServerApi {
    mixinConfig("mixins.surf-lobby.json")
}
```

Every mixin configuration needs its own mixin package. **Mixin is installed by the bootstrap**, so
the server must not bootstrap it itself. A server that runs without the Mixin agent fails
`surfMinestomServer` (tests excepted), and `disableEntityTicking` silently does nothing.

## What is shaded and what is downloaded

`downloadLibraries` defaults to `true`. With it, the server jar keeps only:

- the project's own modules and any project dependency
- `surf-api-minestom-bootstrap-impl`, `xyz.jpenilla:gremlin-runtime` and `org.jspecify:jspecify`
  (`BOOTSTRAP_MODULES` - what the bootstrap needs before anything is downloaded)
- the whole resolved closure of the `shaded` configuration
- every `dev.slne.surf*` module, unless `downloadSurfModules` is set

Everything else goes into `dependencies.txt` with the jar's relocations applied, and is downloaded
at first start. Each component has exactly one destination, jar or download file -
`MinestomServerSurfPlugin.ShadingRules` decides.

`shaded` is for dependencies that **cannot** be downloaded: anything from a repository that needs
credentials, or that only exists in `mavenLocal()`. `runtimeOnly` extends it.

```kotlin
dependencies {
    shaded(libs.private.plugin)
}
```

The whole resolved closure of a `shaded` declaration is embedded, which pulls in public modules it
depends on; `isTransitive = false` narrows a declaration to the module itself.

`downloadSurfModules = true` downloads the surf modules as well, leaving a small jar. It requires
`downloadLibraries`; otherwise the plugin warns and shades everything. With `downloadLibraries =
false`, `dependencies.txt` is excluded from the resources and the ASM version is written into the
`org/objectweb/asm/` manifest section, because Mixin reads it there and a shaded ASM has no such
section.

gremlin is configured with `defaultGremlinRuntimeDependency = false` (the bootstrap module brings
the runtime) and `defaultJarRelocatorDependencies = false` (gremlin's default ASM cannot read
current class files), with `me.lucko:jar-relocator:1.7`, `asm` and `asm-commons` added to
`jarRelocatorRuntime` instead.

## `runServer`

The plugin registers `runServer` in the `surf-api` group. It depends on `shadowJar` and runs the jar
with `java -jar` from the toolchain launcher, so the manifest's agents actually apply - which is why
it is an `Exec` task rather than a `JavaExec`. `standardInput` is wired to `System.in` for the
console feature. The working directory is `runDirectory`, default `run`, created if missing.

```
./gradlew runServer
```

## Logging

Unless the project ships `src/main/resources/log4j2.xml`, the plugin generates one (console plus a
rolling `logs/latest.log`, 30 files). When the console feature is installed, the console appender is
`TerminalConsole` with `%highlightError{...}`, otherwise a plain `Console`. Shipping your own file
replaces it entirely.

## Inside this repository

`local/minestom-example` does **not** apply `dev.slne.surf.api.gradle.minestom-server` - the
published plugin is not available inside this build - so its `build.gradle.kts` reproduces the
manifest attributes, the gremlin wiring, the shadow filter and `runServer` by hand, with a comment
saying so. It is the best place to see what the plugin generates, and `./gradlew
:surf-api-minestom-example:runServer` starts it on `localhost:25565`. It also shades
`surf-api-minestom-example-plugin` in, so the plugins feature finds it on the class path.
