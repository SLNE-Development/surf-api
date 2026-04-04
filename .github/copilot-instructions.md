# GitHub Copilot Instructions for SURF-API

## Build & Run

```bash
# Build everything (includes shadow JARs with relocated deps)
./gradlew build shadowJar

# Build a single module
./gradlew :surf-api-bukkit:surf-api-bukkit-server:build

# Run Paper test server (not available in CI)
./gradlew :surf-api-bukkit:surf-api-bukkit-plugin-test:runServer

# Publish to local Maven
./gradlew publishToMavenLocal
```

CI uses **Java 25 GraalVM**. The Java toolchain version is set in `gradle.properties` (
`javaVersion=25`).

## Architecture

**Multi-platform Minecraft server API** — Kotlin on JVM, targeting Paper/Bukkit, Velocity, and
Standalone (Hytale is future/placeholder). Every platform follows an **API/Implementation split**:

- `-api` modules define public interfaces and extension functions (what consumers depend on)
- `-server` modules contain implementations (shaded into the final JAR, not published as API)
- `surf-api-shared` contains cross-platform annotations, the component system, and internal hooks
- `surf-api-gradle-plugin` provides Gradle plugins (`dev.slne.surf.api.gradle.paper-plugin`, etc.)
  that downstream projects apply, and contains the KSP symbol processor (`surf-api-processor`)

### Service Discovery (two mechanisms)

1. **`requiredService<T>()`** — For singleton bridges/APIs. Uses Adventure's
   `Services.serviceWithFallback()` backed by Java `ServiceLoader`. Implementations register with
   `@AutoService(InterfaceName::class)`. Used for NMS bridges, platform API singletons, and core
   services.

2. **Component System** — For lifecycle-managed components. Classes annotated with
   `@ComponentMeta` (or meta-annotations `@Service`, `@Repository`) are discovered at compile-time
   by KSP, which writes JSON metadata to `META-INF/surfapi/components/`. At runtime,
   `ComponentService` loads, topologically sorts by `@Priority` and `@DependsOnComponent`, checks
   conditions (`@ConditionalOnProperty`, `@DependsOnPlugin`, `@DependsOnClass`, etc.), then calls
   `suspend fun load()` → `enable()` → `disable()`.

### NMS Bridge Pattern

Platform-specific (NMS) code is abstracted behind bridge interfaces in `-api` with implementations
in `-server`:

```kotlin
// In -api: interface + companion delegation
@NmsUseWithCaution
interface SurfBukkitNmsCommonBridge {
    fun nextEntityId(): Int
    companion object : SurfBukkitNmsCommonBridge by requiredService()
}

// In -server: implementation registered via ServiceLoader
@AutoService(SurfBukkitNmsCommonBridge::class)
class SurfBukkitNmsCommonBridgeImpl : SurfBukkitNmsCommonBridge { ... }
```

Bridge implementations call `checkInstantiationByServiceLoader()` in their `init` block.

## Key Conventions

### Kotlin Features

- **Context parameters** enabled globally (`-Xcontext-parameters`). Used extensively in inventory
  framework DSLs:
  ```kotlin
  context(ctx: AbstractSurfViewContext<ViewRef>)
  fun <ViewRef : AbstractSurfViewRef> onOpen(block: context(ViewRef) OpenContext.() -> Unit)
  ```
- **`@InternalSurfApi`** — `@RequiresOptIn` annotation for internal APIs. All subprojects opt in via
  compiler args. Mark new internal APIs with this.
- **Coroutines** for all async work. Component lifecycle methods are `suspend`. Bukkit uses
  MCCoroutine (Folia-aware) for dispatching.
- **Extension functions** are the primary API enrichment pattern, organized in files named
  `*-extension.kt` or `*-extensions.kt`.
- **DSL markers** (`@InventoryFrameworkDSL`, `@ItemDsl`, `@PaneMarker`) restrict scope in builder
  DSLs.

### Logging

Use Google Flogger via the `logger()` helper:

```kotlin
private val log = logger()  // FluentLogger.forEnclosingClass()
log.atWarning().withCause(e).log("Failed to do X for %s", name)
```

### Package Structure

```
dev.slne.surf.api                        # root
dev.slne.surf.api.core.api               # core API interfaces
dev.slne.surf.api.core.server            # core implementations
dev.slne.surf.api.paper.api             # bukkit API (extensions, bridges, DSLs)
dev.slne.surf.api.paper.server          # bukkit implementations
dev.slne.surf.api.velocity.api / .server # velocity
dev.slne.surf.api.shared.api             # shared annotations and component system
dev.slne.surf.api.libs.*                 # relocation target for shaded deps
```

### Dependency Shading

All non-platform dependencies are shaded via Shadow and relocated to `dev.slne.surf.api.libs.*` (
configured in `gradle.properties` as `relocationPrefix`). Platform APIs (Paper, Velocity) are
`compileOnly`. Relocations are configured in the Gradle plugin's `CommonSurfPlugin` using an infix
DSL:

```kotlin
"me.devnatan.inventoryframework" relocatesTo "devnatan.inventoryframework"
```

### Versioning

Format: `{MC_VERSION}-{LIBRARY_VERSION}` (e.g., `1.21.11-2.62.0`), set in `gradle.properties`.
Publishing triggers on pushes to `version/*` branches.

## When Making Changes

- **API changes** go in `-api` modules; implementations in `-server` modules. Never expose `-server`
  types in public API.
- **New components**: annotate with `@ComponentMeta` (or `@Service`/`@Repository`), implement
  `Component`, and declare dependencies with `@DependsOnComponent`, `@DependsOnPlugin`, etc.
- **New bridges**: define interface in `-api` with companion delegation via `requiredService()`,
  implement in `-server` with `@AutoService`.
- **New extension functions**: place in a file named `{topic}-extensions.kt` in the appropriate
  `-api` module.
- **KDoc**: required for all public API members.
- **Version catalog**: all dependency versions live in `gradle/libs.versions.toml`.
