# GitHub Copilot Instructions for SURF-API

## Build & Run

```bash
# Build everything (includes shadow JARs with relocated deps)
./gradlew build shadowJar

# Build a single module (Gradle paths are flat and equal the published artifact name)
./gradlew :surf-api-paper-impl:build

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
  the `@InternalSurfApi` surface), `surf-api-di-api` / `surf-api-di-impl` (the bean container),
  `surf-api-registry-keys` (generated vanilla registry keys) and `surf-api-internal` (internal
  hooks)
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

### The bean container

A class becomes a bean by carrying a stereotype — `@Service`, `@Repository`, or an own annotation
meta-annotated with `@BeanMeta`, all in `dev.slne.surf.api.di`. KSP writes the metadata to
`META-INF/surf-api/beans/` at compile time and the container builds the graph from it. Dependencies
are constructor parameters: `T`, `T?`, `List<T>` / `Set<T>` for every candidate, `Lazy<T>` to resolve
on first access.

```kotlin
@Service
class ShopService(
    private val transactions: TransactionApi,
    private val handlers: List<PriceHandler>,
)
```

surf-api owns the root container; every application gets a child of it holding its own beans.
Resolution walks upwards only, and `Scope.SHARED` is the one way a bean reaches the root and so
every other application.

A bean with something to open and close implements `dev.slne.surf.api.di.SurfComponent` and gets
`load`, `enable` and `disable`, each run as its own pass over all beans in dependency order
(`disable` in reverse). The annotations in `dev.slne.surf.api.di.condition` —
`@ConditionalOnProperty`, `@ConditionalOnPlugin`, `@ConditionalOnClass`, `@ConditionalOnMissingBean`,
`@ConditionalOn`, `@DependsOnBean` and the rest — decide whether a bean is created at all.

A plugin starts its own container from the published api, one call per platform phase:

```kotlin
class MyPlugin : SuspendingJavaPlugin() {
    override suspend fun onLoadAsync() { SurfContainer.startChild(this) }
    override suspend fun onEnableAsync() { SurfContainer.enableFor(this) }
    override suspend fun onDisableAsync() { SurfContainer.stopFor(this) }
}
```

Where there is no constructor to inject into — a platform entry point, a `companion object` that
publishes a service to the rest of an api module — use `by surfBean<T>()`
(`dev.slne.surf.api.di`, `@InternalSurfApi`). It resolves on first access, which is what keeps class
load order out of the behaviour.

### NMS Bridge Pattern

Platform-specific (NMS) code is abstracted behind bridge interfaces in `-api` with implementations
in `-impl`:

```kotlin
// In -api: the contract, plus the accessor the api module reads through
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
```

Callers go through `SurfPaperNmsCommonBridge.INSTANCE.nextEntityId()`.

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
- **Coroutines** for all async work. Bean lifecycle methods are `suspend`. Paper uses
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
dev.slne.surf.api.di.*                   # bean annotations, BeanContainer, SurfContainer (surf-api-di-api)
dev.slne.surf.api.shared.api.*           # annotations (surf-api-annotations)
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
- **New beans**: annotate with `@Service` / `@Repository` / an own `@BeanMeta` stereotype, take
  dependencies as constructor parameters, implement `SurfComponent` only when the bean has
  something to open and close, and guard it with a `@ConditionalOn*` annotation if it is optional.
- **New bridges**: define the interface in `-api` with a `companion object` whose `INSTANCE` reads a
  private `by surfBean<T>()`, and annotate the `-impl` proxy with `@Service`.
- **New extension functions**: place in a file named `{topic}-extensions.kt` in the appropriate
  `-api` module.
- **KDoc**: required for all public API members.
- **Version catalog**: all dependency versions live in `gradle/libs.versions.toml`.
