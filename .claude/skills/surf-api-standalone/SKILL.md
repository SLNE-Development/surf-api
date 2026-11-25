---
name: surf-api-standalone
description: Use when surf-api has to run outside a Minecraft server - a plain JVM service, a daemon, a worker, a CLI or a test runtime - booting it with SurfApiStandaloneBootstrap and its bootstrap/enable/shutdown lifecycle, hosting an application's beans in surf-api's root Spring context or handing a Spring Boot context over with adopt, wiring surf-api-standalone into a Gradle project with dev.slne.surf.api.gradle.standalone, starting the runtime for tests with surf-api-standalone-testing, or deciding whether a part of the api works at all without a server and what happens when it does not.
---

# surf-api standalone

Sources: `platform/standalone/impl/`, `platform/standalone/testing/`, `aggregate/standalone/`,
`foundation/spring/impl/.../spring/context/SurfRootContext.kt`,
`tooling/gradle-plugin/src/main/kotlin/dev/slne/surf/api/gradle/platform/standalone/`,
`.../platform/SurfApiPlatform.kt` and `.../platform/core/CoreSurfPlugin.kt`.

## What it is for

Most of surf-api lives in `surf-api-core-api` and needs no Minecraft server: configuration,
messages and Adventure components, the event bus, serializers, reflection, the math and random
helpers. Their implementations are beans of surf-api's root Spring context, and surf-api's static
entry points read them from there through the `@InternalSurfApi` `surfBean`. On Paper, Minestom and
Velocity the platform plugin builds that root context. Without a platform there is none, and the
first read fails with `IllegalStateException: surf-api's root context has not been started yet`.

`surf-api-standalone` *is* that missing platform: it builds surf-api's root context, publishes a
minimal `SurfApiCore` bean and runs the `CoreInstance` lifecycle, for a JVM process that is not a
server. Three uses:

1. **A plain JVM service** - a daemon, a worker, an HTTP backend, a CLI - that wants surf-api's
   config, messages, beans and event bus.
2. **A Spring Boot program**, which hands its own context to surf-api with `adopt`. The microservice
   runtime does this for every microservice.
3. **Tests**, through `surf-api-standalone-testing`.

| Directory | Coordinate | What it is |
| --- | --- | --- |
| `platform/standalone/impl` | `surf-api-standalone-impl` | the bootstrap and the implementation |
| `platform/standalone/testing` | `surf-api-standalone-testing` | a JUnit Platform session listener that starts and stops the runtime |
| `aggregate/standalone` | `surf-api-standalone` | the coordinate downstream depends on |

None of them publishes an `-all` (shadowed) variant; depend on the plain coordinate and let the
application's own build assemble its jar.

There is no `platform/standalone/api`: the standalone platform adds no contracts of its own. The
impl module re-exports the whole platform surface through `api` and keeps ABI validation on,
because that surface is what downstream compiles against. `SurfApiStandaloneBootstrap` creates the
root context, so it cannot be a bean of it, and the aggregate's build file explains why it is not
split into an api module.

## `SurfApiStandaloneBootstrap`

`platform/standalone/impl/src/main/kotlin/dev/slne/surf/api/standalone/SurfApiStandaloneBootstrap.kt`
is an `object`. The process calls it itself; there is no plugin and no agent.

```kotlin
suspend fun main() {
    SurfApiStandaloneBootstrap.bootstrap(
        PluginDescriptor(
            name = "worker",
            classLoader = WorkerService::class.java.classLoader,
            source = null,
            basePackages = listOf("dev.slne.example.worker"),
            dataDirectory = Path.of("config"),
        )
    )
    SurfApiStandaloneBootstrap.enable()

    // the api and the application's beans are ready here

    // only when you did not want the shutdown hook:
    // SurfApiStandaloneBootstrap.shutdown()
}
```

| Function | What it does |
| --- | --- |
| `bootstrap(application: PluginDescriptor? = null)` | re-arms the shutdown latch, installs a `NoopPacketEvents` as the PacketEvents api and `load()`s and `init()`s it, builds the root context with `SurfRootContext.create`, installs it for the static entry points (`SurfContexts.install`) before the refresh, runs the root's `onLoad` hooks, then `SurfStandaloneInstance.bootstrap()` |
| `enable(registerShutdownHook: Boolean = true)` | registers the root context's beans with `@SurfEventHandler` methods on the event bus (`SurfEventHandlerBeans`), runs the root's `onEnable` hooks and the `afterEnable` phase listeners, then `SurfStandaloneInstance.onLoad()` and `onEnable()`, and - unless told otherwise - registers a JVM shutdown hook that calls `shutdown()` in `runBlocking` |
| `adopt(context, application)` | makes a caller-owned `AnnotationConfigApplicationContext` the root; see below |
| `shutdown()` | runs `SurfStandaloneInstance.onDisable()`, then the `beforeDisable` phase listeners and the `onDisable` hooks, unregisters the event bus listeners that `enable` registered, closes the root context and uninstalls it, then terminates PacketEvents. Runs once per `bootstrap`/`adopt`, whichever way it is reached |

`bootstrap` throws `IllegalStateException("surf-api is already bootstrapped")` while a root context
exists; after `shutdown()` the process can bootstrap again. The event bus then is the new root's,
but the static entry points that read through `surfBean` keep the beans of the root they first
read. `enable` throws when nothing was bootstrapped. Pass
`registerShutdownHook = false` when the process already owns its shutdown sequence - a test harness
or an application framework - and call `shutdown()` from there.

**The application lives in the root context.** A standalone process has a single class loader, so
there are no plugin contexts. The root context always scans `dev.slne.surf.api`. With an
`application` descriptor it also scans the descriptor's `basePackages`, lets every
`ContextContributor` found through `ServiceLoader` contribute (config handles for `@SurfConfig`
classes, NATS), and runs the application's `SurfLifecycle` hooks and `ContextPhaseListener` beans
with surf-api's. The packages must lie outside `dev.slne.surf.api`; `bootstrap` rejects an overlap
with `IllegalArgumentException`. Without a descriptor, beans outside `dev.slne.surf.api` are not
scanned. `PluginDescriptor` lives in `dev.slne.surf.api.spring.context`; `source = null` scans every
class the class loader sees under the packages, and `dataDirectory` is where config handles read and
write their files.

The root context closes *after* `onDisable`, so a bean holding a connection still answers while
everything that might write to it shuts down.

`SurfStandaloneInstance` is nothing but `object SurfStandaloneInstance : CoreInstance()` - the whole
lifecycle is the one in `common/core/impl/.../core/server/CoreInstance.kt`, whose
`bootstrap`/`onLoad`/`onEnable`/`onDisable` are `@MustBeInvokedByOverriders`. `bootstrap()`
initialises `Colors`, `isBootstrapping()` stays true until `onLoad()`
clears it, `onEnable` registers the core listeners (`CoreListenerManager` →
`CorePacketListenerManager`) and `onDisable` unregisters them.

### Handing over a Spring Boot context

`adopt(context, application)` prepares a context the caller creates, refreshes and closes as
surf-api's root. Call it from an `ApplicationContextInitializer`, before the refresh. surf-api scans
its own package and runs the contributors; the application's components come from the caller's own
scan, so `application.basePackages` only feeds the overlap check. The context gets a
`SmartLifecycle` bean in phase `SurfApiStandaloneBootstrap.LIFECYCLE_PHASE` (`0`) that runs the load
and enable hooks when the context starts and `shutdown()` when it stops; beans that need surf-api
enabled start in a later phase. A failing start shuts surf-api down before the failure propagates,
and a refresh that fails earlier undoes the adoption. The lifecycle bean cannot be restarted.

A Spring Boot program other than a microservice supplies what the microservice runtime otherwise
does: the property `surf.config-directory` (`SurfProperties.CONFIG_DIRECTORY`), a `shutdown()` on
`ApplicationFailedEvent`, and shutting log4j down after the context closed. The `adopt` KDoc lists
the details.

## What works and what does not

`SurfStandaloneApiImpl` (`.../standalone/impl/SurfStandaloneApiImpl.kt`) is a `@Service` bean
extending `SurfApiCoreImpl`, and is deliberately thin:

| Member | Standalone behaviour |
| --- | --- |
| `sendPlayerToServer(uuid, server)` | throws `UnsupportedOperationException("sendPlayerToServer is not supported in standalone mode")` |
| `getPlayer(uuid)` | throws `UnsupportedOperationException("getPlayer is not supported in standalone mode")` |
| `isPlayer(audience)` | always `false` |

It also declares `dataFolder = Path.of("api-data")` (relative to the working directory). That
property is on the implementation class only, not on `SurfApiCore`, so a bean that needs it injects
`SurfStandaloneApiImpl`.

**Available**, because it is all platform-independent and the implementations ship in
`surf-api-core-impl`, which `surf-api-standalone-impl` depends on at `api` scope:

- configuration - Configurate with the standalone `SpongeConfigSerializers`
  (`StandaloneSpongeConfigSerializers`, itself a `@Service` bean) → **`surf-api-config`**
- messages, component building, MiniMessage, `Colors`, pagination → **`surf-api-messages`**
- Spring beans, `SurfLifecycle` and injection → **`surf-api-components`**
- the surf event bus → **`surf-api-events`**
- reflection proxies (`reflectionProxy<T>()`), the algorithms, collections, math and random helpers
- the Adventure stack (api, MiniMessage, the gson/legacy/plain/ansi serializers, the slf4j
  component logger), Guava, dazzleconf, spongepowered-math, commons-lang3/text, flogger and
  commons-math - all re-exported at `api` scope, so they are on your compile classpath

**Not available**, because they are a server's or a proxy's api:

- anything under `dev.slne.surf.api.paper.*` or `dev.slne.surf.api.minestom.*` - sidebars, PDC,
  packet listeners, NPCs, visibility, highlights, inventories, dialogs
- Bukkit, Minestom and Velocity types in general, and therefore everything taking a `Player`,
  `World`, `ItemStack` or `Instance`
- commands: `surf-api-standalone-impl` excludes `commandapi-core` and `brigadier` from its
  `surf-api-core-impl` dependency, so there is no command framework here
- PacketEvents is only a `NoopPacketEvents` - the surface exists, nothing is sent or received
- Netty beyond `netty-buffer` and `netty-common`, which ship at runtime for `NoopPacketEvents`
  (versioned by `netty-bom`). Code that needs transport, handler or codec declares them itself.
- `sendPlayerToServer`, `getPlayer`, `isPlayer` as listed above
- `@ConditionalOnPlugin`: the condition reads a `PluginPresence` bean that the platforms register
  for plugin contexts, and a root context built by `bootstrap` has none, so a bean carrying it makes
  `bootstrap()` fail with an `IllegalStateException`. Use Spring's own conditions (`@Profile`, a
  `@Conditional` with your own `Condition`) for a switch a standalone process can flip.

A bean written against the core api works unchanged in standalone; one that touches a platform
type does not. That is the line to check when deciding whether code belongs in
`common/core` or in a platform module - see **`surf-api-build`**.

## Tests

`CoreSurfPlugin` adds `dev.slne.surf.api:surf-api-standalone:+` to `testRuntimeOnly` of every
project using `dev.slne.surf.api.gradle.core` as long as testing is enabled. `CoreSurfPluginTest`
asserts exactly that, and the opposite for `testing { enabled.set(false) }`. That puts the runtime
on the classpath; it does not start it.

To start it, put `surf-api-standalone-testing` on the test runtime classpath:

```kotlin
dependencies {
    testRuntimeOnly("dev.slne.surf.api:surf-api-standalone-testing:<version>")
}
```

Its `SurfApiStandaloneTestSession` is a JUnit Platform `LauncherSessionListener`, registered through
`META-INF/services`. It calls `bootstrap()` and `enable(registerShutdownHook = false)` once when the
test JVM's launcher session opens and `shutdown()` when it closes, so every test can use surf-api's
beans and the API that reads them, such as config classes. Inside this repository the infra modules
use it as `projects.surfApiStandaloneTesting`. A test that calls `bootstrap()` itself must not run
with the session listener, because a second `bootstrap()` fails.

## Setting a project up

```kotlin
plugins {
    id("dev.slne.surf.api.gradle.standalone") version "4.0.0"
}

surfStandaloneApi {
    withCoreCommon()
}
```

`StandaloneSurfPlugin` is `AbstractCoreSurfPlugin<StandaloneSurfExtension>("standalone",
SurfApiPlatform.STANDALONE)` and `StandaloneSurfExtension` adds nothing to `CoreSurfExtension`, so
the whole configuration is the shared one: the plugins, repositories, Kotlin and Java setup,
relocations and the `testing { }` block described in
`surf-api-paper/references/project-setup.md`. It generates no plugin class and runs neither
`checkSurfPluginClasses` nor `checkNoBundledSpring`: a standalone process carries Spring itself.

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
