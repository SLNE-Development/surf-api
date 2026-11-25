# surf-api

**Multi-platform plugin API framework** for Minecraft servers: Spring-managed plugin contexts, NMS
bridge abstractions and Kotlin DSLs for Paper, Minestom, Velocity and standalone JVM services.

[![Latest Release](https://img.shields.io/github/v/release/SLNE-Development/surf-api?label=release)](https://github.com/SLNE-Development/surf-api/releases/latest)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.21-7F52FF?logo=kotlin)](https://kotlinlang.org/)
[![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)](https://openjdk.org/)
[![Docs](https://img.shields.io/badge/docs-slne.dev-informational?logo=readthedocs)](https://docs.slne.dev/surf-api)

---

## Overview

surf-api is the shared foundation for SLNE's server plugin ecosystem. It provides:

- **Platform-abstracted APIs** for Paper (Bukkit/NMS), Minestom, Velocity and standalone
  environments
- **Spring application contexts**: one root context per JVM for surf-api's own beans, and one
  context per plugin below it, scanned from the plugin's own jar
- **Lifecycle hooks** through `SurfLifecycle` (`onLoad` → `onEnable` → `onDisable`), plus
  `onBootstrap` on Paper, run in dependency order
- **NMS bridge pattern** to abstract version-specific internals behind stable interfaces
- **Inventory framework DSLs**, command helpers, packet-based sidebars and extension-rich APIs
- **Infra libraries** for NATS, Redis, R2DBC databases and Spring Boot microservices, released with
  surf-api under one version
- **Gradle plugin** (`surf-api-gradle-plugin`) that configures downstream projects, generates the
  plugin main class and relocates shared dependencies

For the changes a downstream project makes to move to 4.0, see [MIGRATION-4.0.md](MIGRATION-4.0.md).
For the design of the Spring integration, see [docs/spring-migration.md](docs/spring-migration.md).

---

## Module structure

Directories group the modules; the published name is derived from the directory path by dropping
the first segment, joining the rest with `-` and prefixing `surf-api-`. So `common/core/api`
publishes as `surf-api-core-api` and `aggregate/paper` publishes as `surf-api-paper`. Gradle project
paths stay flat (`:surf-api-core-api`), so the typesafe accessor reads `projects.surfApiCoreApi`.

Every module directory has a `README.md` that states what the module does and what it contains.

`settings.gradle.kts` makes these exceptions:

- `infra/` modules keep their nested layout, with every `surf-` segment renamed to `surf-api-`:
  `infra/redis/surf-redis-api` is `:surf-api-redis:surf-api-redis-api`
  (`projects.surfApiRedis.surfApiRedisApi`) and publishes as `surf-api-redis-api`.
- `tooling/gradle-plugin` is an included build, not a module, and publishes as
  `surf-api-gradle-plugin`.
- `tooling/nms-module-generator` is an included build that scaffolds a new Paper NMS version
  module.

```
surf-api
├── foundation/                       # Bottom layer: depends on no other surf-api module
│   ├── annotations                   # surf-api-annotations: @InternalSurfApi, @PublishedSurfApi & co.
│   ├── internal                      # surf-api-internal: internal utilities, not meant for plugin code
│   ├── registry-keys                 # surf-api-registry-keys: generated vanilla registry key constants
│   └── spring
│       ├── api                       # surf-api-spring-api: SurfLifecycle, @PluginApi, @ConditionalOnPlugin, @BootstrapComponent
│       └── impl                      # surf-api-spring-impl: root and plugin contexts, exports, lifecycle hook runner
│
├── common/                           # Platform-agnostic, cut by domain
│   ├── common
│   │   └── api                       # surf-api-common-api: the shared base: util, logger, collections, math, random, font
│   ├── serialization
│   │   └── api                       # surf-api-serialization-api: kotlinx-serialization serializers
│   ├── messages
│   │   └── api                       # surf-api-messages-api: text: Colors, builders, MiniMessage, pagination, action bar
│   ├── config
│   │   ├── api                       # surf-api-config-api: config classes, constraints, migrations, environment variables
│   │   └── impl                      # surf-api-config-impl: the config service
│   ├── event
│   │   └── api                       # surf-api-event-api: SurfEventBus and the event contracts
│   ├── nbt
│   │   └── api                       # surf-api-nbt-api: Adventure binary tags
│   ├── reflection
│   │   └── api                       # surf-api-reflection-api: @ReflectionProxy interfaces and reflectionProxy<T>()
│   ├── command
│   │   └── api                       # surf-api-command-api: what the three platforms' command layers share
│   ├── luckperms
│   │   └── api                       # surf-api-luckperms-api: SurfLuckPerms and the LuckPerms extensions
│   ├── inventory
│   │   └── api                       # surf-api-inventory-api: the platform-neutral half of the inventory framework
│   └── core
│       ├── api                       # surf-api-core-api: SurfApiCore; re-exports the common, messages, event, nbt,
│       │                             #   reflection, command, luckperms, serialization and spring api modules,
│       │                             #   surf-api-registry-keys and surf-api-annotations
│       └── impl                      # surf-api-core-impl: core implementations
│
├── platform/                         # One group per runtime
│   ├── paper
│   │   ├── api                       # surf-api-paper-api: Paper/Bukkit contracts (extensions, NMS bridges, DSLs)
│   │   ├── impl                      # surf-api-paper-impl: Paper implementations and the surf-api plugin
│   │   └── nms
│   │       ├── api                   # surf-api-paper-nms-api: the contracts the version modules implement
│   │       ├── shared-26             # sources every 26.x module compiles (no module of its own)
│   │       ├── v1-21-11              # surf-api-paper-nms-v1-21-11: NMS beans for 1.21.11
│   │       ├── v26-1                 # surf-api-paper-nms-v26-1: NMS beans for 26.1
│   │       ├── v26-2                 # surf-api-paper-nms-v26-2: NMS beans for 26.2
│   │       └── v26-3                 # surf-api-paper-nms-v26-3: NMS beans for 26.3
│   ├── minestom
│   │   ├── api                       # surf-api-minestom-api: Minestom contracts
│   │   ├── impl                      # surf-api-minestom-impl: Minestom implementations
│   │   ├── bootstrap-impl            # Server bootstrap
│   │   ├── console-impl              # Console & command input
│   │   ├── npc-impl                  # NPCs
│   │   ├── player-visibility-impl    # Per-player entity visibility
│   │   ├── plugins-impl              # Plugin loading
│   │   ├── signed-chat-impl          # Signed chat handling
│   │   ├── spark-impl                # Spark profiler integration
│   │   └── luckperms-impl            # LuckPerms integration (needs vendor/LuckPerms)
│   ├── velocity
│   │   ├── api                       # surf-api-velocity-api: Velocity proxy contracts
│   │   └── impl                      # surf-api-velocity-impl: Velocity implementations and the surf-api plugin
│   └── standalone
│       ├── impl                      # surf-api-standalone-impl: standalone (non-server) runtime
│       └── testing                   # surf-api-standalone-testing: starts the standalone runtime once per test JVM
│
├── aggregate/                        # The coordinates downstream projects depend on; no code of their own
│   ├── core                          # surf-api-core
│   ├── paper                         # surf-api-paper
│   ├── minestom                      # surf-api-minestom
│   ├── minestom-server               # surf-api-minestom-server: for building a runnable Minestom server
│   ├── velocity                      # surf-api-velocity
│   └── standalone                    # surf-api-standalone
│
├── infra/                            # Broker, database and microservice libraries
│   ├── nats                          # surf-api-nats-*: api, core, types, standalone, spring, test fixtures
│   ├── redis                         # surf-api-redis-*: api, core, spring, standalone
│   ├── microservice                  # surf-api-microservice-*: api, runtime, database, reliability
│   └── database-r2dbc                # surf-api-database-r2dbc: Exposed on R2DBC, with Flyway migrations
│
├── tooling/
│   ├── bom                           # surf-api-bom: one version for every module
│   ├── registry-key-generator        # Regenerates foundation/registry-keys from vanilla data (local only)
│   ├── gradle-plugin                 # surf-api-gradle-plugin (included build): configures downstream projects
│   └── nms-module-generator          # Included build: scaffolds a new Paper NMS version module
│
├── vendor/                           # spark-minestom and the patched LuckPerms build
│
└── local/                            # Never published, excluded from CI
    ├── paper-plugin-test             # surf-api-paper-plugin-test: Paper test server
    ├── paper-shared-bean-*           # Provider, consumer and boot test for plugin exports on Paper
    ├── velocity-shared-bean-*        # The same boot test on Velocity
    ├── minestom-example              # surf-api-minestom-example: runnable Minestom example server
    └── minestom-example-plugin       # surf-api-minestom-example-plugin: example Minestom plugin
```

> **API/implementation split:** `-api` modules declare the contracts downstream projects compile
> against. Most of them carry ABI validation, and `checkApiBoundary` fails the build if one of them
> depends on an implementation module. `-impl` modules carry the implementations; they are wired in
> at runtime and ship inside surf-api's platform jars. Downstream projects do not name either
> directly. They depend on the `aggregate/` coordinates, which hold no code. `surf-api-core`,
> `surf-api-paper`, `surf-api-minestom` and `surf-api-velocity` pass on only the contracts.
> `surf-api-standalone` and `surf-api-minestom-server` export their platform's `-impl` module,
> because a standalone application or a Minestom server runs that runtime itself:
> `surf-api-standalone` carries `surf-api-standalone-impl` and, through it, `surf-api-core-impl`
> and `surf-api-internal`; `surf-api-minestom-server` carries `surf-api-minestom-impl`.
> Importing `surf-api-bom` pins one version for every module, so no transitive path can pull a
> second version of a sibling module into the graph.

---

## Key concepts

### Spring contexts

surf-api builds on Spring Framework. Each JVM has one **root context** holding surf-api's own beans,
such as the event bus, the config API and the platform bridges. Every plugin gets two more contexts
below it:

- **Import context**: holds only the beans that the plugin's direct dependencies export.
- **Plugin context**: holds every bean of the plugin. Its parent is the import context.

On Paper, a plugin also gets a bootstrap context, built in Paper's bootstrap phase. It holds the
plugin's beans that implement `SurfBootstrapLifecycle` or carry `@BootstrapComponent`.

A microservice is a Spring Boot application; its context is the root context.

A class becomes a bean through a Spring stereotype such as `@Service`, `@Component` or
`@Repository`, or through a `@Bean` method of a `@Configuration` class. Dependencies are
constructor parameters. Component scanning starts at the plugin's base package and reads only the
plugin's own jar. The following bean runs work in the plugin's enable phase:

```kotlin
package com.example.shop

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.spring.SurfLifecycle
import org.springframework.stereotype.Service

interface PriceHandler {
    fun adjust(price: Long): Long
}

@Service
class ShopService(private val handlers: List<PriceHandler>) : SurfLifecycle {
    private val log = logger()

    fun finalPrice(base: Long): Long = handlers.fold(base) { price, handler -> handler.adjust(price) }

    override suspend fun onEnable() {
        log.atInfo().log("Shop ready with %d price handlers", handlers.size)
    }
}
```

`SurfLifecycle` hooks run in the order in which the beans finished initializing; `onDisable` runs in
reverse. Constructors and `@PostConstruct` only wire objects; work that needs the server belongs in a
hook.

To see where startup time goes, start the server with `-Dsurf.startup.jfr=true` and a Java Flight
Recorder recording: each context build and each plugin phase appears as a `surf.*` startup step
next to Spring's own. Debug logging of `dev.slne.surf.api.spring.context.PluginContext` prints the
phase durations of every plugin without a recording. See
[Startup steps](docs/spring-migration.md#startup-steps).

### Plugin classes

The Gradle plugin generates the platform main class into the plugin's base package:
`GeneratedSurfPlugin`, and on Paper also `GeneratedSurfBootstrapper`. They extend `SurfPaperPlugin`,
`SurfPaperBootstrapper`, `SurfVelocityPlugin` or `SurfMinestomPlugin`, whose lifecycle methods are
final. A plugin that needs a class of its own names it in the build script
(`surfPaperPluginApi { mainClass(...) }`, `velocityPluginFile { main = ... }`,
`minestomPluginFile { main = ... }`) and extends the same base class.

The plugin main instance is a Spring-managed bean of the plugin context. Spring injects its
`@Autowired` members, runs `@PostConstruct` and every `BeanPostProcessor` on it, and calls
`@PreDestroy` and `DisposableBean.destroy()` when the context closes.

### Exports and soft dependencies

A plugin exports a bean by implementing an interface annotated with `@PluginApi`. Plugins that
declare the owning plugin as a direct dependency inject the bean by that interface; every other bean
stays private to its plugin. The following exported interface and its implementation live in the
providing plugin:

```kotlin
package com.example.economy

import dev.slne.surf.api.spring.PluginApi
import org.springframework.stereotype.Service
import java.util.UUID

@PluginApi
interface EconomyApi {
    fun balance(player: UUID): Long
}

@Service
class EconomyService : EconomyApi {
    override fun balance(player: UUID): Long = 0
}
```

A consuming plugin injects `EconomyApi` like any other constructor parameter.

Code that uses an optional plugin goes into an adapter bean annotated with
`@ConditionalOnPlugin("PluginName")`. Several names in one annotation are alternatives for a plugin
whose name differs between platforms, such as `@ConditionalOnPlugin("LuckPerms", "luckperms")`;
repeat the annotation to require several plugins. Spring evaluates the condition from bytecode metadata, so the
adapter's references to the optional plugin never load when it is absent. The rest of the plugin
injects the adapter's interface as a nullable type.

### Reaching surf-api's services

Plugins inject surf-api's services, such as `SurfEventBus`, a `ConfigHandle`, the plugin's
`CoroutineScope` and, on Paper, `PaperDispatchers`, or call the extension and top-level functions
that wrap them. On Paper and Minestom, inject the inventory framework's `ViewFrame` or
`ViewFrameAccessor`.

The companion accessors (`SurfEventBus.instance`, `ConfigSerializerCollections.instance`, the NMS
bridges' `instance`) and `surfBean<T>()` in `dev.slne.surf.api.spring` are `@InternalSurfApi`. They
serve surf-api's own static entry points, which have no constructor to inject into. `surfBean<T>()`
resolves a root-context bean on first access and again after the platform installs another root
context:

```kotlin
@NmsUseWithCaution
interface SurfPaperNmsCommonBridge {
    fun nextEntityId(): Int

    companion object {
        @InternalSurfApi
        val instance by surfBean<SurfPaperNmsCommonBridge>()
    }
}
```

A class that is itself a bean injects the dependency instead of reading the accessor.

---

## Environment variables

`surf-api-config-api` (package `dev.slne.surf.api.config.environment`) provides small, type-safe
environment-variable access without a separate schema or lookup phase. Direct reads resolve
immediately:

```kotlin
val DATABASE_HOST: String = env.required("DATABASE_HOST")
val DATABASE_PORT: Int = env.int("DATABASE_PORT", default = 5432) {
    requireIn(1..65535)
}
val INSTANCE_ID: UUID? = env.optionalUuid("INSTANCE_ID")
```

Property delegates resolve lazily on first access, infer the variable name from the property, and
cache the parsed value thread-safely:

```kotlin
object ServiceEnvironment {
    val DATABASE_HOST by env.requiredNonBlank()
    val DATABASE_PORT by env.int(default = 5432) {
        requireIn(1..65535)
    }
    val DATABASE_PASSWORD by env.requiredSecret()
    val DEBUG by env.boolean(default = false)
}
```

Use `env.required().named("DATABASE_HOST")` for an explicitly named delegate. Required values are
non-null, optional values are nullable, and defaulted values are non-null. Missing required values,
conversion failures and validation failures throw dedicated exceptions containing the variable
name. Mark values as sensitive, or use `requiredSecret`, to keep raw values and parser causes out of
error details. Required strings preserve present blank values; `requiredNonBlank` rejects them.
Built-in converters cover strings, numeric types, strict booleans, UUIDs, enums and Kotlin
durations. Custom converters can be passed directly, for example
`env.required("ID", UUID::fromString)`.

---

## Getting started

### Prerequisites

- Java 25 (CI uses GraalVM)
- The Gradle wrapper in this repository (Gradle 9.8.2)

### Build

```bash
# Full build, including the platform runtime jars
./gradlew build

# Build a single module (project paths are flat, whatever the directory)
./gradlew :surf-api-paper-impl:build

# Publish to the local Maven repository
./gradlew publishToMavenLocal
```

`build` writes the platform runtime jars to `build/libs/*-all.jar` of `surf-api-paper-impl`,
`surf-api-velocity-impl` and `surf-api-standalone-impl`. Releases attach them as GitHub release
assets. Modules built on `core-convention` publish no shaded (`-all` or `shadowRuntimeElements`)
variant to the Maven repository. The `infra/` modules with an explicit `shadow` publication, such as
`surf-api-redis-api`, still publish their shaded jar.

---

## Conventions

### Kotlin

| Convention | Details |
|---|---|
| **Context parameters** | Enabled for surf-api's modules and for projects built with the surf Gradle plugin (`-Xcontext-parameters`). Used in inventory DSLs and other scoped builders. |
| **`@InternalSurfApi`** | `@RequiresOptIn` at error level. Mark internal APIs with it. surf-api's own modules opt in through the root build; downstream projects do not. |
| **Coroutines** | All async work uses Kotlin coroutines, and lifecycle hooks are `suspend`. Every plugin context holds a `CoroutineScope` bean with a `SupervisorJob` and `Dispatchers.Default`. On Paper, the `PaperDispatchers` bean switches to the main, region or entity thread (Folia-aware). |
| **Extension functions** | Primary API enrichment pattern; organized in files named `*-extension.kt` or `*-extensions.kt`. |
| **DSL markers** | `@InventoryFrameworkDSL` and `@ItemDsl` restrict scope in builder DSLs. |

### Logging

Use Google Flogger through the `logger()` helper:

```kotlin
private val log = logger() // FluentLogger.forEnclosingClass()

log.atInfo().log("Server started")
log.atWarning().withCause(e).log("Failed to load %s", name)
```

### Package layout

```
dev.slne.surf.api                      # Root
dev.slne.surf.api.spring               # SurfLifecycle, @PluginApi, @ConditionalOnPlugin, @BootstrapComponent, surfBean
dev.slne.surf.api.spring.context       # Root and plugin contexts (surf-api-spring-impl)
dev.slne.surf.api.common               # The shared base: util, logger, collections, math, random, font, algorithms
dev.slne.surf.api.serialization        # kotlinx-serialization serializers
dev.slne.surf.api.messages             # Text: Colors, builders, MiniMessage, pagination, action bar, Rarity
dev.slne.surf.api.config               # Config classes, constraints, migrations, environment variables
dev.slne.surf.api.config.impl          # The config service
dev.slne.surf.api.event                # SurfEventBus and the event contracts
dev.slne.surf.api.nbt                  # Adventure binary tags
dev.slne.surf.api.reflection           # @ReflectionProxy interfaces and reflectionProxy<T>()
dev.slne.surf.api.command              # What the three platforms' command layers share
dev.slne.surf.api.luckperms            # SurfLuckPerms and the LuckPerms extensions
dev.slne.surf.api.inventory            # The platform-neutral half of the inventory framework
dev.slne.surf.api.core                 # SurfApiCore and PlayerLookupService
dev.slne.surf.api.core.generated       # Generated registry keys
dev.slne.surf.api.core.server          # Core implementations
dev.slne.surf.api.paper                # Paper API (extensions, DSLs, sidebars)
dev.slne.surf.api.paper.nms.bridges    # Paper NMS bridge interfaces
dev.slne.surf.api.paper.nms.common     # The contracts the NMS version modules implement
dev.slne.surf.api.paper.server         # Paper implementations
dev.slne.surf.api.minestom             # Minestom API
dev.slne.surf.api.minestom.server      # Minestom implementations
dev.slne.surf.api.velocity             # Velocity API
dev.slne.surf.api.velocity.server      # Velocity implementations
dev.slne.surf.api.standalone           # The standalone bootstrap
dev.slne.surf.api.standalone.testing   # Test support for the standalone runtime
dev.slne.surf.api.nats                 # NATS integration for surf-api's contexts
dev.slne.surf.api.redis                # Redis integration for surf-api's contexts
dev.slne.surf.api.shared.api           # Shared annotations (@InternalSurfApi & co.)
dev.slne.surf.api.shared.internal      # Internal utilities
dev.slne.surf.api.libs.*               # Relocation target for dependencies shared with plugins
dev.slne.surf.nats                     # surf-nats
dev.slne.surf.redis                    # surf-redis
dev.slne.surf.microservice             # surf-microservice
dev.slne.surf.database                 # surf-database-r2dbc
```

### Dependency shading

surf-api's platform jars bundle its dependencies, Spring included. Spring stays unrelocated: every
plugin compiles against it and uses the copy inside surf-api. The platform jars relocate these
packages themselves:

- Configurate and Adventure NBT, below `dev.slne.surf.api.libs` (`relocationPrefix` in
  `gradle.properties`)
- on Paper, the inventory framework and wyck, below the same prefix
- on Paper and Velocity, jnats and BouncyCastle, below `dev.slne.surf.nats.libs`

Redisson and its dependencies arrive already relocated in the shaded surf-redis jars: Netty below
`dev.slne.surf.redis.shaded`, everything else below `dev.slne.surf.redis.libs`. The Paper jar
leaves out Guava, Gson and SLF4J, which the server provides.

Plugins built with the surf Gradle plugin relocate the shared packages to the same targets, so they
bind to surf-api's copies: Configurate on every platform, Adventure NBT on every platform except
Velocity, the inventory framework on Paper and Minestom, and wyck on Paper. The relocations are
declared with an infix DSL in `CommonSurfPlugin` and its platform subclasses, for example in
`AbstractPaperSurfPlugin`:

```kotlin
"me.devnatan.inventoryframework" relocatesTo "devnatan.inventoryframework"
```

The task `checkNoBundledSpring` fails a Paper, Velocity or Minestom plugin whose jar would contain
Spring. `allowBundledSpring = true` turns it off.

The api modules declare their third-party libraries `compileOnlyApi`, a scope that only Gradle
module metadata carries; the Maven POMs leave these libraries out. surf-api therefore supports
Gradle consumers only.

---

## Notable dependencies

| Library | Purpose |
|---|---|
| [Spring Framework](https://spring.io/projects/spring-framework) | Dependency injection and the application contexts |
| [Spring Boot](https://spring.io/projects/spring-boot) | Microservice runtime (microservices only) |
| [PacketEvents](https://github.com/retrooper/packetevents) | Packet-level networking abstraction |
| [CommandAPI](https://commandapi.jorel.dev/) | Brigadier-based command framework |
| [Inventory Framework](https://github.com/devnatan/inventory-framework) (SLNE fork) | GUI/inventory DSL |
| [Adventure](https://docs.advntr.net/) | Text & component API |
| [LuckPerms](https://luckperms.net/) | Permissions API |
| [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) | Placeholder support |
| [Caffeine](https://github.com/ben-manes/caffeine) | High-performance caching |
| [Flogger](https://google.github.io/flogger/) | Fluent logging |
| [Configurate](https://github.com/SpongePowered/Configurate) (SLNE fork) | Configuration loading |
| [jnats](https://github.com/nats-io/nats.java) | NATS client behind surf-nats |
| [Redisson](https://redisson.org/) | Redis client behind surf-redis |
| [Exposed](https://github.com/JetBrains/Exposed) and [R2DBC](https://r2dbc.io/) | Reactive database access in surf-database-r2dbc |
| [Flyway](https://github.com/flyway/flyway) | Database migrations in surf-database-r2dbc |

---

## Contributing

When making changes:

- **API changes** go in `-api` modules. Never expose `-impl` types in the public API;
  `checkApiBoundary` fails the build if an `-api` module depends on an implementation module.
- **Implementations** go in `-impl` modules.
- **New beans**: annotate with a Spring stereotype such as `@Service`, take dependencies as
  constructor parameters, and implement `SurfLifecycle` only if the bean has work to do in a
  platform phase.
- **New NMS bridges**: declare the interface in `surf-api-paper-api`
  (`dev.slne.surf.api.paper.nms.bridges`) with an `@InternalSurfApi` companion
  `val instance by surfBean<T>()`. Implement it in every NMS version module as an `@NmsComponent`
  class; the module's `@ConditionalOnMinecraftVersion` configuration scans for it. Plugins inject
  the bridge interface.
- **NMS version modules** for 26.x apply `surf.nms-shared-26` next to `surf.module-impl`, compile
  the shared sources of `platform/paper/nms/shared-26` and keep only the files that differ.
- Publishing is automated: push to a `version/*` branch to trigger a release.

---

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).

---

## Links

- 📖 [Documentation](https://docs.slne.dev/surf-api)
- 📦 [Releases](https://github.com/SLNE-Development/surf-api/releases)
- 🐛 [Issues](https://github.com/SLNE-Development/surf-api/issues)
- 💬 [Discussions](https://github.com/SLNE-Development/surf-api/discussions)
