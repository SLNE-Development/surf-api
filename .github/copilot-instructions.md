# GitHub Copilot Instructions for SURF-API

## Build & Run

```bash
# Build everything, including the platform runtime jars (build/libs/*-all.jar)
./gradlew build

# Build a single module (Gradle paths are flat and equal the published artifact name)
./gradlew :surf-api-paper-impl:build

# Publish to Maven Local
./gradlew publishToMavenLocal
```

CI uses **Java 25 GraalVM**. The Java toolchain version is set in `gradle.properties`
(`javaVersion=25`). Use the Gradle wrapper (Gradle 9.8.2).

## Architecture

**Multi-platform Minecraft server API**: Kotlin on the JVM, targeting Paper, Minestom, Velocity and
standalone JVM services, with Spring Framework for dependency injection.

For the Spring integration's design, read `docs/spring-migration.md`. For what changed for
downstream projects in 4.0, read `MIGRATION-4.0.md`. Topic guides live in `.claude/skills/`.

### Module naming

Directories only group modules. The Gradle project path is **flat** and equals the published
artifact name, which `settings.gradle.kts` derives from the directory by dropping the first segment
and joining the rest with `-` behind the `surf-api-` prefix:

```
common/core/api           ->  :surf-api-core-api
platform/paper/nms/v26-3  ->  :surf-api-paper-nms-v26-3
aggregate/paper           ->  :surf-api-paper
```

Exceptions:

- `infra/` modules keep their nested paths and rename every `surf-` segment to `surf-api-`:
  `infra/redis/surf-redis-api` -> `:surf-api-redis:surf-api-redis-api`, published as
  `surf-api-redis-api`.
- `tooling/gradle-plugin` is an included build (`surf-api-gradle-plugin`), not a module.
- `tooling/registry-key-generator` (`surf-api-registry-key-generator`, local only) regenerates the
  keys in `foundation/registry-keys`.
- `tooling/nms-module-generator` is an included build that scaffolds a new Paper NMS version
  module.

Every platform follows an **API/implementation split**, carried by the module suffix:

- `-api` modules define public interfaces and extension functions (what consumers compile against)
- `-impl` modules contain the implementations; they ship inside surf-api's platform jars. The
  aggregates `surf-api-core`, `-paper`, `-minestom` and `-velocity` export only `-api` modules.
  `surf-api-standalone` and `surf-api-minestom-server` export their platform's `-impl` module
  (`surf-api-standalone-impl`, which carries `surf-api-core-impl`, and `surf-api-minestom-impl`),
  because those consumers run the runtime themselves. Never expose `-impl` types in `-api` modules.
- a module with neither suffix under `aggregate/` is an **aggregate**

### Folder groups

- `foundation/`: platform-agnostic basics. `surf-api-annotations` (cross-platform annotations and
  `@InternalSurfApi`), `surf-api-internal` (internal utilities), `surf-api-registry-keys` (generated
  vanilla registry keys), and `surf-api-spring-api` / `surf-api-spring-impl` (the Spring contracts
  and the root and plugin contexts)
- `common/`: platform-agnostic domains, one module per domain: `surf-api-common-api` (the shared
  base), `-serialization-api`, `-messages-api`, `-config-api` / `-config-impl`, `-event-api`,
  `-nbt-api`, `-reflection-api`, `-command-api`, `-luckperms-api`, `-inventory-api`, and
  `surf-api-core-api` / `-core-impl`. `surf-api-core-api` re-exports the common, messages, event,
  nbt, reflection, command, luckperms, serialization and spring api modules,
  `surf-api-registry-keys` and `surf-api-annotations`.
- `platform/`: one group per platform. `surf-api-paper-api` / `-impl` plus `surf-api-paper-nms-api`
  and the versioned `surf-api-paper-nms-v*` modules; `surf-api-minestom-api` / `-impl` plus the
  per-feature modules
  `surf-api-minestom-{bootstrap,console,luckperms,npc,player-visibility,plugins,signed-chat,spark}-impl`;
  `surf-api-velocity-api` / `-impl`; `surf-api-standalone-impl` and `surf-api-standalone-testing`
- `aggregate/`: `surf-api-core`, `surf-api-paper`, `surf-api-minestom`,
  `surf-api-minestom-server`, `surf-api-velocity`, `surf-api-standalone`. These **carry no code**:
  each only re-exports the modules of its platform, and they are the coordinates downstream
  projects depend on.
- `infra/`: `nats`, `redis`, `microservice` and `database-r2dbc`, released with surf-api under one
  version (`surf-api-nats-*`, `surf-api-redis-*`, `surf-api-microservice-*`,
  `surf-api-database-r2dbc`)
- `tooling/`: `surf-api-bom` and the `surf-api-gradle-plugin` included build, which provides the
  Gradle plugins downstream projects apply, including `dev.slne.surf.api.gradle.paper-plugin`,
  `.velocity`, `.minestom`, `.minestom-server`, `.standalone`, `.core`, `.microservice` and `.nats`
- `local/`: never published. `surf-api-paper-plugin-test`, the `paper-shared-bean-*` and
  `velocity-shared-bean-*` boot tests, `surf-api-minestom-example`,
  `surf-api-minestom-example-plugin`

`local/` and `surf-api-registry-key-generator` are skipped when `CI=true`.
`surf-api-minestom-luckperms-impl` is only included once `vendor/LuckPerms` has been generated; CI
fails without it.

### Spring contexts

Each JVM has one **root context** with surf-api's own beans. Every plugin gets an **import
context** below it, holding the beans its direct dependencies export, and a **plugin context**
below that, holding the plugin's own beans. On Paper a plugin also gets a **bootstrap context**,
built in Paper's bootstrap phase, for beans that implement `SurfBootstrapLifecycle` or carry
`@BootstrapComponent`. A microservice is a Spring Boot application whose context is the root
context.

A class becomes a bean through a Spring stereotype (`@Service`, `@Component`, `@Repository`) or a
`@Bean` method in a `@Configuration` class. Dependencies are constructor parameters: `T`, `T?`,
`List<T>` for every candidate, `ObjectProvider<T>` for lazy or optional lookup. Component scanning
starts at the plugin's base package and reads only the plugin's own jar.

```kotlin
@Service
class ShopService(private val handlers: List<PriceHandler>) : SurfLifecycle {
    override suspend fun onEnable() {
        // runs in the plugin's enable phase
    }
}
```

The types in `dev.slne.surf.api.spring` (`surf-api-spring-api`):

- `SurfLifecycle`: suspending `onLoad`, `onEnable`, `onDisable`, run in bean initialization order
  (`onDisable` in reverse); `@Order` moves a bean ahead of unordered ones
- `SurfBootstrapLifecycle`: `onBootstrap`, Paper only
- `@BootstrapComponent`: puts a bean into Paper's bootstrap context
- `@PluginApi`: on an interface; every bean implementing it is exported to plugins that declare the
  owner as a direct dependency. All other beans stay private to their plugin.
- `@ConditionalOnPlugin("Name")`: registers an adapter bean only if the plugin is installed; Spring
  evaluates it without loading the class. Several names are alternatives for one plugin (its
  names on different platforms); repeat the annotation to require several plugins. Inject the adapter's interface as `T?`.

The platforms ship Spring Framework, not Spring Boot, so Boot's `@ConditionalOn*` annotations are
available only in microservices.

The Gradle plugin generates the platform main class (`GeneratedSurfPlugin`, and on Paper
`GeneratedSurfBootstrapper`) into the plugin's base package. It extends `SurfPaperPlugin`,
`SurfPaperBootstrapper`, `SurfVelocityPlugin` or `SurfMinestomPlugin`, whose lifecycle methods are
final; plugin code goes into beans. The plugin main instance is a Spring-managed bean: it gets
`@Autowired` members, `@PostConstruct`, every `BeanPostProcessor`, and `@PreDestroy` /
`DisposableBean.destroy()` when its context closes.

Plugins inject surf-api's services (`ConfigHandle`, `SurfEventBus`, `CoroutineScope`,
`PaperDispatchers`, `ViewFrame` or `ViewFrameAccessor`). The companion accessors (`instance`)
and `surfBean<T>()` are `@InternalSurfApi`: they exist for surf-api's own static entry points,
which have no constructor to inject into. `surfBean<T>()` resolves a root-context bean on first
access, which keeps class load order out of the behavior, and resolves it again when the platform
installs another root context. A class that is itself a bean injects the dependency instead.

### NMS Bridge Pattern

NMS code is abstracted behind bridge interfaces in `surf-api-paper-api`
(`dev.slne.surf.api.paper.nms.bridges`). Each version module (`surf-api-paper-nms-v*`) implements
them as beans that exist only when the server runs that version:

```kotlin
// In surf-api-paper-api: the contract, plus the accessor surf-api's static entry points read through
@NmsUseWithCaution
interface SurfPaperNmsCommonBridge {
    fun nextEntityId(): Int

    companion object {
        @InternalSurfApi
        val instance by surfBean<SurfPaperNmsCommonBridge>()
    }
}

// In a version module: found by the module's configuration, not by the root scan
@NmsComponent
class V26_3SurfPaperNmsCommonBridgeImpl : SurfPaperNmsCommonBridge { /* ... */ }

// One per version module; registered only for the selected NmsVersion
@Configuration(proxyBeanMethods = false)
@ConditionalOnMinecraftVersion(NmsVersion.V26_3)
@ComponentScan(
    useDefaultFilters = false,
    includeFilters = [ComponentScan.Filter(NmsComponent::class)],
)
class V26_3NmsConfiguration
```

Plugins inject `SurfPaperNmsCommonBridge`. The Paper runtime selects the version from
`NmsVersion`: the server's own, else the newest older one. The 26.x modules apply
`surf.nms-shared-26`, which compiles the shared sources of `platform/paper/nms/shared-26` into each
of them; a module keeps only the files that differ.

## Key Conventions

### Kotlin Features

- **Context parameters** enabled for surf-api's modules and for projects built with the surf Gradle
  plugin (`-Xcontext-parameters`). Used extensively in inventory framework DSLs:
  ```kotlin
  context(ctx: AbstractSurfViewContext<ViewRef>)
  fun <ViewRef : AbstractSurfViewRef> onOpen(block: context(ViewRef) (@InventoryFrameworkDSL OpenContext).() -> Unit)
  ```
- **`@InternalSurfApi`**: `@RequiresOptIn` annotation for internal APIs. surf-api's own modules opt
  in through the root build; downstream projects do not. Mark internal APIs with it.
- **Coroutines** for all async work. Lifecycle hooks are `suspend`. Every plugin context holds a
  `CoroutineScope` bean (`SupervisorJob`, `Dispatchers.Default`); on Paper, the `PaperDispatchers`
  bean switches to the main, region or entity thread (Folia-aware).
- **Extension functions** are the primary API enrichment pattern, organized in files named
  `*-extension.kt` or `*-extensions.kt`.
- **DSL markers** (`@InventoryFrameworkDSL`, `@ItemDsl`) restrict scope in builder DSLs.

### Logging

Use Google Flogger via the `logger()` helper:

```kotlin
private val log = logger()  // FluentLogger.forEnclosingClass()
log.atWarning().withCause(e).log("Failed to do X for %s", name)
```

### Package Structure

```
dev.slne.surf.api                        # root
dev.slne.surf.api.spring                 # Spring contracts (surf-api-spring-api)
dev.slne.surf.api.spring.context         # root and plugin contexts (surf-api-spring-impl)
dev.slne.surf.api.{common,messages,config,event,nbt,reflection,command,luckperms,inventory,serialization}
                                         # one package per common/ domain
dev.slne.surf.api.core.*                 # core API (surf-api-core-api)
dev.slne.surf.api.core.server.*          # core implementations (surf-api-core-impl)
dev.slne.surf.api.core.generated.*       # registry keys (surf-api-registry-keys)
dev.slne.surf.api.paper.*                # Paper API (extensions, DSLs, sidebars)
dev.slne.surf.api.paper.nms.bridges.*    # Paper NMS bridge interfaces
dev.slne.surf.api.paper.nms.common.*     # The contracts the NMS version modules implement
dev.slne.surf.api.paper.server.*         # Paper implementations
dev.slne.surf.api.minestom.* / .server.* # Minestom
dev.slne.surf.api.velocity.* / .server.* # Velocity
dev.slne.surf.api.standalone.*           # standalone runtime and test support
dev.slne.surf.api.nats / .redis          # NATS and Redis integration for surf-api's contexts
dev.slne.surf.api.shared.api.*           # annotations (surf-api-annotations)
dev.slne.surf.api.shared.internal.*      # internal utilities (surf-api-internal)
dev.slne.surf.api.libs.*                 # relocation target for dependencies shared with plugins
dev.slne.surf.{nats,redis,microservice,database}.*  # infra libraries
```

### Dependency Shading

surf-api's platform jars bundle its dependencies, Spring included and unrelocated: plugins compile
against Spring and use surf-api's copy. Relocated packages:

- Configurate and Adventure NBT, below `dev.slne.surf.api.libs` (`relocationPrefix` in
  `gradle.properties`), by every module's shadow jar (`core-convention.gradle.kts`)
- on Paper, the inventory framework and wyck, below the same prefix
- on Paper and Velocity, jnats and BouncyCastle, below `dev.slne.surf.nats.libs`
  (`relocateNats()` in buildSrc)
- Redisson and its dependencies, below `dev.slne.surf.redis.libs` (Netty below
  `dev.slne.surf.redis.shaded`), already in the shaded surf-redis jars
  (`infra/redis/build.gradle.kts`)

Plugins built with the surf Gradle plugin relocate the shared packages to the same targets:
Configurate on every platform, Adventure NBT on every platform except Velocity, the inventory
framework on Paper and Minestom, and wyck on Paper. The relocations use the infix DSL in
`CommonSurfPlugin` and its platform subclasses, for example in `AbstractPaperSurfPlugin`:

```kotlin
"me.devnatan.inventoryframework" relocatesTo "devnatan.inventoryframework"
```

`checkNoBundledSpring` fails a Paper, Velocity or Minestom plugin whose jar would contain Spring
(`allowBundledSpring = true` turns it off). Modules built on `core-convention` publish no
`shadowRuntimeElements` / `-all` variant; the platform `-all` jars are GitHub release assets only.
The explicit `shadow` publications of `infra/` (for example `surf-api-redis-api`) still publish
their shaded jar.

### Versioning

`version` in `gradle.properties` is the surf-api version (for example `4.0.0`, with `-SNAPSHOT`
appended when `snapshot=true`). `mcVersion` names the Minecraft version the API targets (for example
`26.3`). Publishing triggers on pushes to `version/*` branches.

## When Making Changes

- **API changes** go in `-api` modules; implementations in `-impl` modules. Never expose `-impl`
  types in public API. Aggregates in `aggregate/` only re-export modules; never add code there.
- **New beans**: annotate with a Spring stereotype, take dependencies as constructor parameters,
  implement `SurfLifecycle` only when the bean has work in a platform phase, and put code that uses
  an optional plugin into a `@ConditionalOnPlugin` adapter.
- **New bridges**: define the interface in `surf-api-paper-api` with an `@InternalSurfApi` companion
  `val instance by surfBean<T>()`, and implement it as an `@NmsComponent` class in every version
  module (once in `shared-26` for the 26.x modules).
- **New 26.x NMS version modules**: apply `surf.nms-shared-26` next to `surf.module-impl`.
- **New extension functions**: place in a file named `{topic}-extensions.kt` in the appropriate
  `-api` module.
- **KDoc**: required for all public API members.
- **Version catalog**: all dependency versions live in `gradle/libs.versions.toml`.
