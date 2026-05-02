# surf-api

**Multi-platform plugin API framework** for Minecraft servers — providing modular service
discovery, lifecycle-managed components, NMS bridge abstractions, and rich DSLs across Paper,
Velocity, and Standalone platforms.

[![Latest Release](https://img.shields.io/github/v/release/SLNE-Development/surf-api?label=release)](https://github.com/SLNE-Development/surf-api/releases/latest)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-7F52FF?logo=kotlin)](https://kotlinlang.org/)
[![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)](https://openjdk.org/)
[![Docs](https://img.shields.io/badge/docs-slne.dev-informational)](https://docs.slne.dev/surf-api)

---

## Overview

surf-api is the shared foundation for SLNE's server plugin ecosystem. It provides:

- **Platform-abstracted APIs** for Paper (Bukkit/NMS), Velocity, and Standalone environments
- **Service discovery** via `ServiceLoader` (`requiredService<T>()`) for singleton bridges
- **Component system** with compile-time KSP discovery, topological lifecycle ordering, and
  conditional activation
- **NMS bridge pattern** to safely abstract version-specific internals behind stable interfaces
- **Inventory framework DSLs**, command helpers, scoreboard utilities, and extension-rich APIs
- **Gradle plugin** (`surf-api-gradle-plugin`) that configures downstream projects, handles
  dependency shading, and runs the KSP processor

---

## Module Structure

```
surf-api
├── surf-api-core                  # Platform-agnostic core API & implementation
│   ├── surf-api-core              # Core API interfaces
│   └── surf-api-core-server       # Core implementations
│
├── surf-api-paper                 # Paper/Bukkit platform
│   ├── surf-api-paper             # Public API (extensions, bridges, DSLs)
│   ├── surf-api-paper-server      # Implementations (shaded into final JAR)
│   ├── surf-api-paper-nms
│   │   ├── surf-api-paper-nms-common     # Shared NMS bridge interfaces
│   │   ├── surf-api-paper-nms-v1-21-11   # NMS impl for 1.21.11
│   │   └── surf-api-paper-nms-v26-1      # NMS impl for 26.1
│   └── surf-api-paper-plugin-test # Local test server (excluded from CI)
│
├── surf-api-velocity              # Velocity proxy platform
│   ├── surf-api-velocity          # Public API
│   └── surf-api-velocity-server   # Implementations
│
├── surf-api-standalone            # Standalone (non-server) platform
│
├── surf-api-shared                # Cross-platform annotations & component system
│   ├── surf-api-shared-public     # Public shared annotations (@ComponentMeta, etc.)
│   └── surf-api-shared-internal   # Internal shared utilities
│
├── surf-api-gradle-plugin         # Gradle convention plugins & KSP processor
│   └── surf-api-processor         # KSP processor — discovers @ComponentMeta at compile time
│
└── surf-api-generator             # NMS module scaffolding generator (local only)
```

> **API/Implementation split:** `-api` modules define public interfaces consumed by downstream
> projects. `-server` modules contain implementations that are shaded into the final JAR and never
> exposed as a public API dependency.

---

## Key Concepts

### Service Discovery

Two mechanisms are available depending on the use case:

#### 1. `requiredService<T>()` — Singleton bridges

For singleton platform bridges and NMS abstractions. Backed by Java `ServiceLoader`; implementations
register themselves with `@AutoService`.

```kotlin
// In -api: declare the bridge interface with companion delegation
@NmsUseWithCaution
interface SurfBukkitNmsCommonBridge {
    fun nextEntityId(): Int

    companion object : SurfBukkitNmsCommonBridge by requiredService()
}

// In -server: provide the implementation
@AutoService(SurfBukkitNmsCommonBridge::class)
class SurfBukkitNmsCommonBridgeImpl : SurfBukkitNmsCommonBridge {
    init { checkInstantiationByServiceLoader() }

    override fun nextEntityId(): Int = TODO()
}

// Usage — callers go through the companion, unaware of the implementation
val id = SurfBukkitNmsCommonBridge.nextEntityId()
```

#### 2. Component System — Lifecycle-managed components

Classes annotated with `@ComponentMeta` (or the meta-annotations `@Service` / `@Repository`) are
discovered at compile-time by the KSP processor, which writes JSON metadata to
`META-INF/surfapi/components/`. At runtime, `ComponentService`:

1. Loads all component metadata
2. Topologically sorts by `@Priority` and `@DependsOnComponent`
3. Checks conditions (`@ConditionalOnProperty`, `@DependsOnPlugin`, `@DependsOnClass`, …)
4. Calls `suspend fun load()` → `enable()` → `disable()` in order

```kotlin
@Service
class MyFeatureService : SurfComponent {
    override suspend fun enable() {
        // runs after all dependencies are enabled
    }

    override suspend fun disable() {
        // runs in reverse order on shutdown
    }
}
```

---

## Getting Started

### Prerequisites

- Java 25 (GraalVM recommended for CI)
- Gradle 8+

### Build

```bash
# Full build (produces shadow JARs with relocated dependencies)
./gradlew build shadowJar

# Build a single module
./gradlew :surf-api-paper:surf-api-paper-server:build

# Publish to local Maven repository
./gradlew publishToMavenLocal

# Run the Paper test server (local only, not available in CI)
./gradlew :surf-api-paper:surf-api-paper-plugin-test:runServer
```

---

## Conventions

### Kotlin

| Convention | Details |
|---|---|
| **Context parameters** | Enabled globally (`-Xcontext-parameters`). Used in inventory DSLs and other scoped builders. |
| **`@InternalSurfApi`** | `@RequiresOptIn` — mark new internal APIs with this annotation. All subprojects opt-in via compiler args. |
| **Coroutines** | All async work uses Kotlin coroutines. Component lifecycle methods are `suspend`. Bukkit uses MCCoroutine (Folia-aware). |
| **Extension functions** | Primary API enrichment pattern; organized in files named `*-extension.kt` or `*-extensions.kt`. |
| **DSL markers** | `@InventoryFrameworkDSL`, `@ItemDsl`, `@PaneMarker` restrict scope in builder DSLs. |

### Logging

Use Google Flogger via the `logger()` helper:

```kotlin
private val log = logger() // FluentLogger.forEnclosingClass()

log.atInfo().log("Server started")
log.atWarning().withCause(e).log("Failed to load component %s", name)
```

### Package Layout

```
dev.slne.surf.api                      # Root
dev.slne.surf.api.core.api             # Core API interfaces
dev.slne.surf.api.core.server          # Core implementations
dev.slne.surf.api.paper.api            # Paper API (extensions, bridges, DSLs)
dev.slne.surf.api.paper.server         # Paper implementations
dev.slne.surf.api.velocity.api         # Velocity API
dev.slne.surf.api.velocity.server      # Velocity implementations
dev.slne.surf.api.shared.api           # Shared annotations & component system
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
| [KSP](https://github.com/google/ksp) | Compile-time component discovery |
| [AutoService](https://github.com/google/auto/tree/main/service) | `ServiceLoader` registration |

---

## Contributing

When making changes:

- **API changes** go in `-api` modules. **Never** expose `-server` types in the public API.
- **Implementations** go in `-server` modules.
- **New components**: annotate with `@ComponentMeta` / `@Service` / `@Repository` and implement the
  appropriate lifecycle methods.
- **New NMS bridges**: declare the interface in `-api`, implement in `-server`, register via
  `@AutoService`, and call `checkInstantiationByServiceLoader()` in the `init` block.
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
