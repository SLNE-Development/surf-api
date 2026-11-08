# GitHub Copilot Instructions for SURF-API

## Build & Run

```bash
# Build everything (includes shadow JARs with relocated deps)
./gradlew build shadowJar

# Build a single module (Gradle paths are flat and equal the published artifact name)
./gradlew :surf-api-paper-impl:build

# Run Paper test server (not available in CI)
./gradlew :surf-api-paper-plugin-test:runServer

# Publish to local Maven
./gradlew publishToMavenLocal
```

CI uses **Java 25 GraalVM**. The Java toolchain version is set in `gradle.properties` (
`javaVersion=25`).

## Architecture

**Multi-platform Minecraft server API** — Kotlin on JVM, targeting Paper, Minestom, Velocity, and
Standalone.

### Module naming

Directories only group modules. The Gradle project path is **flat** and equals the published
artifact name, which `settings.gradle.kts` derives from the directory by dropping the first segment
and joining the rest with `-` behind the `surf-api-` prefix:

```
common/core/api           ->  :surf-api-core-api
platform/paper/nms/v26-3  ->  :surf-api-paper-nms-v26-3
aggregate/paper           ->  :surf-api-paper
```

Every platform follows an **API/implementation split**, carried by the module suffix:

- `-api` modules define public interfaces and extension functions (what consumers depend on)
- `-impl` modules contain the implementations; they are shaded into the final JAR and are not part
  of the published API surface. Never expose `-impl` types in public API.
- a module with neither suffix is an **aggregate**

### Folder groups

- `foundation/` — platform-agnostic basics: `surf-api-annotations` (cross-platform annotations and
  the component system contracts) and `surf-api-internal` (internal hooks and the
  `@InternalSurfApi` surface)
- `common/` — the platform-agnostic core: `surf-api-core-api`, `surf-api-core-impl`
- `platform/` — one domain per platform: `surf-api-paper-api` / `-impl` plus
  `surf-api-paper-nms-api` and the versioned `surf-api-paper-nms-v*` modules;
  `surf-api-minestom-api` / `-impl` plus the per-feature modules
  `surf-api-minestom-{bootstrap,console,luckperms,npc,player-visibility,plugins,signed-chat,spark}-impl`;
  `surf-api-velocity-api` / `-impl`; and `surf-api-standalone-impl`
- `aggregate/` — `surf-api-core`, `surf-api-paper`, `surf-api-minestom`,
  `surf-api-minestom-server`, `surf-api-velocity`, `surf-api-standalone`. These **carry no code**:
  each only re-exports the modules of its platform, and they are the coordinates downstream
  projects depend on.
- `tooling/` — `surf-api-bom`, `surf-api-gradle-plugin` (provides the Gradle plugins
  `dev.slne.surf.api.gradle.paper-plugin`, etc. that downstream projects apply) with the KSP
  symbol processor `surf-api-processor`, and `surf-api-generator`
- `local/` — never published: `surf-api-paper-plugin-test`, `surf-api-minestom-example`,
  `surf-api-minestom-example-plugin`

`local/`, `surf-api-generator` and the NMS module generator are skipped when `CI=true`;
`surf-api-minestom-luckperms-impl` is only included once `vendor/LuckPerms` has been generated.

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
in `-impl`:

```kotlin
// In -api: interface + companion delegation
@NmsUseWithCaution
interface SurfPaperNmsCommonBridge {
    fun nextEntityId(): Int
    companion object : SurfPaperNmsCommonBridge by requiredService()
}

// In -impl: implementation registered via ServiceLoader
@AutoService(SurfPaperNmsCommonBridge::class)
class SurfPaperNmsCommonBridgeProxy : SurfPaperNmsCommonBridge { ... }
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
- **Coroutines** for all async work. Component lifecycle methods are `suspend`. Paper uses
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
dev.slne.surf.api.core.*                 # core API (surf-api-core-api)
dev.slne.surf.api.core.server.*          # core implementations (surf-api-core-impl)
dev.slne.surf.api.paper.*                # Paper API (extensions, bridges, DSLs)
dev.slne.surf.api.paper.server.*         # Paper implementations
dev.slne.surf.api.paper.nms.common.*     # Paper NMS bridge contracts
dev.slne.surf.api.minestom.*             # Minestom API and implementations
dev.slne.surf.api.velocity.* / .server.* # Velocity
dev.slne.surf.api.standalone.impl.*      # Standalone runtime
dev.slne.surf.api.shared.api.*           # annotations, component system (surf-api-annotations)
dev.slne.surf.api.shared.internal.*      # internal hooks (surf-api-internal)
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

- **API changes** go in `-api` modules; implementations in `-impl` modules. Never expose `-impl`
  types in public API. Aggregates in `aggregate/` only re-export modules — never add code there.
- **New components**: annotate with `@ComponentMeta` (or `@Service`/`@Repository`), implement
  `Component`, and declare dependencies with `@DependsOnComponent`, `@DependsOnPlugin`, etc.
- **New bridges**: define interface in `-api` with companion delegation via `requiredService()`,
  implement in `-impl` with `@AutoService`.
- **New extension functions**: place in a file named `{topic}-extensions.kt` in the appropriate
  `-api` module.
- **KDoc**: required for all public API members.
- **Version catalog**: all dependency versions live in `gradle/libs.versions.toml`.
