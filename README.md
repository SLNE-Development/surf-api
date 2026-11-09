# surf-api

**Multi-platform plugin API framework** for Minecraft servers — providing a dependency-injection
container, lifecycle-managed beans, NMS bridge abstractions, and rich DSLs across Paper, Minestom,
Velocity, and Standalone platforms.

[![Latest Release](https://img.shields.io/github/v/release/SLNE-Development/surf-api?label=release)](https://github.com/SLNE-Development/surf-api/releases/latest)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-7F52FF?logo=kotlin)](https://kotlinlang.org/)
[![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)](https://openjdk.org/)
[![Docs](https://img.shields.io/badge/docs-slne.dev-informational?logo=readthedocs)](https://docs.slne.dev/surf-api)

---

## Overview

surf-api is the shared foundation for SLNE's server plugin ecosystem. It provides:

- **Platform-abstracted APIs** for Paper (Bukkit/NMS), Minestom, Velocity, and Standalone
  environments
- **Bean container** with compile-time KSP discovery, constructor injection, a container per
  application below a process-wide root, and conditional activation
- **Opt-in bean lifecycle** (`load` → `enable` → `disable`) driven in dependency order
- **NMS bridge pattern** to safely abstract version-specific internals behind stable interfaces
- **Inventory framework DSLs**, command helpers, scoreboard utilities, and extension-rich APIs
- **Gradle plugin** (`surf-api-gradle-plugin`) that configures downstream projects, handles
  dependency shading, and runs the KSP processor

---

## Module Structure

Directories group the modules; the published name is derived from the directory path by dropping the
first segment, joining the rest with `-`, and prefixing `surf-api-`. So `common/core/api` publishes
as `surf-api-core-api` and `aggregate/paper` publishes as `surf-api-paper`. Gradle project paths
stay flat (`:surf-api-core-api`), which is why the typesafe accessor reads `projects.surfApiCoreApi`.

```
surf-api
├── foundation/                       # Bottom layer — depends on no other surf-api module
│   ├── annotations                   # surf-api-annotations: @InternalSurfApi, @GenerateReflection & co.
│   ├── di
│   │   ├── api                       # surf-api-di-api: bean annotations, BeanContainer, SurfContainer
│   │   └── impl                      # surf-api-di-impl: the container itself — graph, conditions, lifecycle
│   ├── registry-keys                 # surf-api-registry-keys: generated vanilla registry key constants
│   └── internal                      # surf-api-internal: internal utilities, never a public dependency
│
├── common/                           # Platform-agnostic, cut by domain
│   ├── common
│   │   └── api                       # surf-api-common-api: the shared base — util, logger, collections, math, random, font
│   ├── serialization
│   │   └── api                       # surf-api-serialization-api: kotlinx-serialization serializers and codecs
│   ├── config
│   │   ├── api                       # surf-api-config-api: config classes, constraints, managers, migrations
│   │   └── impl                      # surf-api-config-impl: the config service
│   ├── inventory
│   │   └── api                       # surf-api-inventory-api: the platform-neutral half of the inventory framework
│   └── core
│       ├── api                       # surf-api-core-api: what is not yet its own domain — messages, events, NBT, commands
│       └── impl                      # surf-api-core-impl: core implementations
│
├── platform/                         # One group per runtime
│   ├── paper
│   │   ├── api                       # surf-api-paper-api: Paper/Bukkit contracts (extensions, bridges, DSLs)
│   │   ├── impl                      # surf-api-paper-impl: Paper implementations
│   │   └── nms
│   │       ├── api                   # surf-api-paper-nms-api: version-independent NMS bridge interfaces
│   │       ├── v1-21-11              # surf-api-paper-nms-v1-21-11: NMS bridge for 1.21.11
│   │       ├── v26-1                 # surf-api-paper-nms-v26-1: NMS bridge for 26.1
│   │       ├── v26-2                 # surf-api-paper-nms-v26-2: NMS bridge for 26.2
│   │       └── v26-3                 # surf-api-paper-nms-v26-3: NMS bridge for 26.3
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
│   │   └── impl                      # surf-api-velocity-impl: Velocity implementations
│   └── standalone
│       └── impl                      # surf-api-standalone-impl: standalone (non-server) runtime
│
├── aggregate/                        # The coordinates downstream projects depend on — no code of their own
│   ├── core                          # surf-api-core
│   ├── paper                         # surf-api-paper
│   ├── minestom                      # surf-api-minestom
│   ├── minestom-server               # surf-api-minestom-server: for building a runnable Minestom server
│   ├── velocity                      # surf-api-velocity
│   └── standalone                    # surf-api-standalone
│
├── tooling/
│   ├── bom                           # surf-api-bom: one version for every module
│   ├── gradle-plugin                 # surf-api-gradle-plugin: configures downstream projects, shading, KSP
│   │   └── processor                 # surf-api-processor: KSP processor — discovers beans and @GenerateReflection
│   └── generator                     # surf-api-generator: NMS module scaffolding (local only)
│
└── local/                            # Never published, excluded from CI
    ├── paper-plugin-test             # surf-api-paper-plugin-test: Paper test server
    ├── minestom-example              # surf-api-minestom-example: runnable Minestom example server
    └── minestom-example-plugin       # surf-api-minestom-example-plugin: example Minestom plugin
```

> **API/implementation split:** `-api` modules declare the contracts downstream projects compile
> against. They carry ABI validation, and the build fails if one of them depends on an
> implementation module. `-impl` modules carry the implementations; they are wired in at runtime and
> shaded into the final JAR, and never reach a consumer's compile classpath. Downstream projects do
> not name either directly — they depend on the `aggregate/` coordinates (`surf-api-paper` and
> friends), which hold no code and let the contracts reach the consumer transitively. Importing
> `surf-api-bom` pins one version for every module, so no transitive path can pull a second version
> of a sibling module into the graph.

## Environment Variables

`surf-api-core-api` provides small, type-safe environment-variable access without a separate schema or
lookup phase. Direct reads resolve immediately:

```kotlin
val DATABASE_HOST: String = env.required("DATABASE_HOST")
val DATABASE_PORT: Int = env.int("DATABASE_PORT", default = 5432) {
    requireIn(1..65535)
}
val INSTANCE_ID: UUID? = env.optionalUuid("INSTANCE_ID")
```

Property delegates resolve lazily on first access, infer the variable name exactly from the property,
and cache the parsed value thread-safely:

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
conversion failures, and validation failures throw dedicated exceptions containing the variable
name. Mark values as sensitive, or use `requiredSecret`, to keep raw values and parser causes out of
error details. Required strings preserve present blank values; `requiredNonBlank` rejects them.
Built-in converters cover strings, numeric types, strict booleans, UUIDs, enums, and Kotlin
durations. Custom converters can be passed directly, for example
`env.required("ID", UUID::fromString)`.

---

## Key Concepts

### The bean container

A class becomes a bean by carrying a stereotype — `@Service`, `@Repository`, or an annotation of
your own meta-annotated with `@BeanMeta`, all from `dev.slne.surf.api.di`. The KSP processor writes
the metadata to `META-INF/surf-api/beans/` at compile time, and the container builds the graph from
it without loading a class first. A bean needs no supertype; dependencies are constructor
parameters.

```kotlin
@Service
class ShopService(
    private val transactions: TransactionApi,
    private val handlers: List<PriceHandler>,
)
```

Containers form a hierarchy: surf-api owns the root, and every application — a plugin, a
microservice — gets a child of it holding its own beans. Resolution walks upwards only, so an
application sees surf-api's beans and never another application's. `Scope.SHARED` is the one way a
bean crosses that boundary.

A bean that owns something to open and close implements `SurfComponent` and gets `load`, `enable`
and `disable`, each run as its own pass over all beans in dependency order (`disable` in reverse).
`@ConditionalOnProperty`, `@ConditionalOnPlugin`, `@ConditionalOnClass` and the rest of
`dev.slne.surf.api.di.condition` decide whether a bean is created at all.

A plugin starts its own container from the published api, one call per platform phase:

```kotlin
class MyPlugin : SuspendingJavaPlugin() {
    override suspend fun onLoadAsync() { SurfContainer.startChild(this) }
    override suspend fun onEnableAsync() { SurfContainer.enableFor(this) }
    override suspend fun onDisableAsync() { SurfContainer.stopFor(this) }
}
```

### Reaching a bean where there is no constructor

An api module publishes its own service through `surfBean<T>()`, a property that resolves on first
access. This is the pattern behind every `Companion.INSTANCE` in the api modules: the contract lives
in `-api`, the `@Service` implementation in `-impl`, and the caller never names either.

```kotlin
// In -api: the contract, plus the accessor the rest of the api module uses
@NmsUseWithCaution
interface SurfPaperNmsCommonBridge {
    fun nextEntityId(): Int

    companion object {
        val INSTANCE get() = bridge
    }
}

private val bridge by surfBean<SurfPaperNmsCommonBridge>()

// In -impl: an ordinary bean
@NmsUseWithCaution
@Service
class SurfPaperNmsCommonBridgeProxy :
    SurfPaperNmsCommonBridge by NmsProvider.current.createCommonBridge()

// Usage — callers go through INSTANCE, unaware of the implementation
val id = SurfPaperNmsCommonBridge.INSTANCE.nextEntityId()
```

`surfBean` and `SurfContainer` are `@InternalSurfApi`: they exist for the edges where there is no
constructor to inject into. Inside a bean, write what you need into the constructor instead.

---

## Getting Started

### Prerequisites

- Java 25 (GraalVM recommended for CI)
- Gradle 8+

### Build

```bash
# Full build (produces shadow JARs with relocated dependencies)
./gradlew build shadowJar

# Build a single module (project paths are flat, whatever the directory)
./gradlew :surf-api-paper-impl:build

# Publish to local Maven repository
./gradlew publishToMavenLocal
```

---

## Conventions

### Kotlin

| Convention | Details |
|---|---|
| **Context parameters** | Enabled globally (`-Xcontext-parameters`). Used in inventory DSLs and other scoped builders. |
| **`@InternalSurfApi`** | `@RequiresOptIn` — mark new internal APIs with this annotation. All subprojects opt-in via compiler args. |
| **Coroutines** | All async work uses Kotlin coroutines. Bean lifecycle methods are `suspend`. Bukkit uses MCCoroutine (Folia-aware). |
| **Extension functions** | Primary API enrichment pattern; organized in files named `*-extension.kt` or `*-extensions.kt`. |
| **DSL markers** | `@InventoryFrameworkDSL`, `@ItemDsl`, `@PaneMarker` restrict scope in builder DSLs. |

### Logging

Use Google Flogger via the `logger()` helper:

```kotlin
private val log = logger() // FluentLogger.forEnclosingClass()

log.atInfo().log("Server started")
log.atWarning().withCause(e).log("Failed to load bean %s", name)
```

### Package Layout

```
dev.slne.surf.api                      # Root
dev.slne.surf.api.common               # The shared base: util, logger, collections, math, random, font
dev.slne.surf.api.serialization        # kotlinx-serialization serializers and codecs
dev.slne.surf.api.config               # Config classes, constraints, managers, migrations
dev.slne.surf.api.config.impl          # The config service
dev.slne.surf.api.inventory            # The platform-neutral half of the inventory framework
dev.slne.surf.api.core                 # What is not yet its own domain: messages, events, NBT, commands
dev.slne.surf.api.core.server          # Core implementations
dev.slne.surf.api.paper                # Paper API (extensions, bridges, DSLs)
dev.slne.surf.api.paper.server         # Paper implementations
dev.slne.surf.api.minestom             # Minestom API
dev.slne.surf.api.minestom.server      # Minestom implementations
dev.slne.surf.api.velocity             # Velocity API
dev.slne.surf.api.velocity.server      # Velocity implementations
dev.slne.surf.api.standalone           # The standalone bootstrap
dev.slne.surf.api.di                   # Bean annotations, BeanContainer, SurfContainer
dev.slne.surf.api.shared.api           # Shared annotations (@InternalSurfApi & co.)
dev.slne.surf.api.libs.*               # Relocation target for all shaded dependencies
```

### Dependency Shading

All non-platform dependencies are shaded via Shadow and relocated to `dev.slne.surf.api.libs.*`
(configured via `relocationPrefix` in `gradle.properties`). Platform APIs (Paper, Velocity) remain
`compileOnly`. Relocations are declared in `CommonSurfPlugin` using an infix DSL:

```kotlin
"me.devnatan.inventoryframework" relocatesTo "devnatan.inventoryframework"
```

---

## Notable Dependencies

| Library | Purpose |
|---|---|
| [PacketEvents](https://github.com/retrooper/packetevents) | Packet-level networking abstraction |
| [CommandAPI](https://commandapi.jorel.dev/) | Brigadier-based command framework |
| [MCCoroutine](https://github.com/Shynixn/MCCoroutine) (SLNE fork) | Folia-aware coroutine dispatching |
| [Inventory Framework](https://github.com/devnatan/inventory-framework) | GUI/inventory DSL |
| [Adventure](https://docs.advntr.net/) | Text & component API |
| [LuckPerms](https://luckperms.net/) | Permissions API |
| [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) | Placeholder support |
| [Caffeine](https://github.com/ben-manes/caffeine) | High-performance caching |
| [Flogger](https://google.github.io/flogger/) | Fluent logging |
| [Ktor](https://ktor.io/) | HTTP client |
| [Configurate](https://github.com/SpongePowered/Configurate) | Configuration loading |
| [KSP](https://github.com/google/ksp) | Compile-time bean discovery |

---

## Contributing

When making changes:

- **API changes** go in `-api` modules. **Never** expose `-impl` types in the public API —
  `checkApiBoundary` fails the build if an `-api` module depends on an implementation module.
- **Implementations** go in `-impl` modules.
- **New beans**: annotate with `@Service` / `@Repository` / an own `@BeanMeta` stereotype, take
  dependencies as constructor parameters, and implement `SurfComponent` only if the bean has
  something to open and close.
- **New NMS bridges**: declare the interface in `-api` with a `companion object` whose `INSTANCE`
  reads a private `by surfBean<T>()`, and annotate the `-impl` proxy with `@Service`.
- Publishing is automated — push to a `version/*` branch to trigger a release.

---

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).

---

## Links

- 📖 [Documentation](https://docs.slne.dev/surf-api)
- 📦 [Releases](https://github.com/SLNE-Development/surf-api/releases)
- 🐛 [Issues](https://github.com/SLNE-Development/surf-api/issues)
- 💬 [Discussions](https://github.com/SLNE-Development/surf-api/discussions)
