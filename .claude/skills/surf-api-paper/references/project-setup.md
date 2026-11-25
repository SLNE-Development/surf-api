# Setting up a Paper project against surf-api

Source: `tooling/gradle-plugin/src/main/kotlin/dev/slne/surf/api/gradle/platform/paper/`,
`.../platform/common/CommonSurfPlugin.kt`, `.../platform/core/CoreSurfPlugin.kt`,
`.../platform/core/CoreSurfExtension.kt`, `.../pluginclass/`,
`.../generators/LibrariesLoaderGenerator.kt` and `tooling/gradle-plugin/build.gradle.kts`.

## The plugin ids

| Id | Implementation | Extension |
| --- | --- | --- |
| `dev.slne.surf.api.gradle.paper-plugin` | `PaperPluginSurfPlugin` | `surfPaperPluginApi { }` |
| `dev.slne.surf.api.gradle.paper-raw` | `RawPaperSurfPlugin` | `surfRawPaperApi { }` |
| `dev.slne.surf.api.gradle.settings` | `SurfSettingsPlugin` | - (settings plugin; only enables `TYPESAFE_PROJECT_ACCESSORS`) |

The Gradle plugin is versioned with surf-api: `tooling/gradle-plugin/build.gradle.kts` reads
`version` from the root `gradle.properties` (`4.0.0`) and appends `-SNAPSHOT` when the `snapshot`
property is true.

The class hierarchy is `CommonSurfPlugin` → `AbstractCoreSurfPlugin` → `AbstractPaperSurfPlugin` →
`PaperPluginSurfPlugin` / `RawPaperSurfPlugin`, and the extensions mirror it:
`CommonSurfExtension` → `CoreSurfExtension` → `AbstractPaperSurfExtension` →
`PaperPluginSurfExtension` / `RawPaperSurfExtension`. A setting you cannot find on the Paper
extension is probably on one of the parents.

## What applying the plugin does

`CommonSurfPlugin` applies, for every surf platform:

- `org.gradle.java-gradle-plugin`, `org.gradle.java-library`, `org.gradle.maven-publish`
- `org.jetbrains.kotlin.jvm` plus the `spring`, `jpa` and `serialization` Kotlin plugins
- `com.gradleup.shadow` and `com.google.devtools.ksp`

`PaperPluginSurfPlugin` adds `de.eldoria.plugin-yml.paper`.

Repositories: `mavenCentral()`, `gradlePluginPortal()`, `https://reposilite.slne.dev/public/`
(named `slne-repository-public`) and `https://repo.papermc.io/repository/maven-public/`.
`useCanvasMc()` adds `https://maven.canvasmc.io/snapshots`.

Kotlin and Java configuration:

- `jvmToolchain(Constants.JAVA_VERSION)` - 25 in this repository (`gradle.properties`)
- free compiler args `-Xjsr305=strict`, `-Xcontext-parameters`, `-Xreturn-value-checker=check`,
  and `javaParameters = true`, which Spring needs to match constructor parameters by name
- `JavaCompile` gets UTF-8 and `-parameters`
- `withSourcesJar()` and `withJavadocJar()`
- no surf-api KSP processor and no generated bean index: Spring finds a plugin's beans by scanning
  its classes. The KSP Gradle plugin is still applied for a plugin's own processors.
- `gradleApi()` and `gradleTestKit()` are removed from every configuration except `compileOnly`

Dependencies added in `afterEvaluate`:

- `dev.slne.surf.api:surf-api-paper:+` at `compileOnly` (`SurfApiPlatform.PAPER`). The version is
  the literal `+`: `Constants.SURF_API_VERSION` is `"+"`, so the newest published api is resolved.
  `addSurfApiToClasspath(false)` suppresses it, `surfApiScope("api")` changes the scope.
- `io.papermc.paper:paper-api` (`Constants.PAPER_API`), or `io.canvasmc.canvas:canvas-api` with
  `useCanvasMc()`, at `compileOnly`.
- When `withCorePaper()` was called: `dev.slne.surf.core:surf-core-api-paper:+` at `compileOnly`.
  The sibling calls are `withCoreCommon()`, `withCoreVelocity()`, `withCoreMinestom()`.
- `withSurfRedis()` adds `dev.slne.surf.api:surf-api-redis-api:+` at `compileOnly`. Redis ships
  inside surf-api's own jar, so nothing is shaded or relocated into the plugin. There is no database
  call: the database library serves microservices only.
- `withSurfNpc()` adds `dev.slne.surf.npc:surf-npc-api:+` at `compileOnly` and registers
  `surf-npc-paper` as a required server dependency.

What the compile classpath does **not** get from surf-api: Reactor Netty and `io.netty` (surf-api's
core api exports neither) and DataFixerUpper (`com.mojang:datafixerupper`). A plugin that uses Netty
declares it itself, as `compileOnly` because the server provides it; one that uses DataFixerUpper
declares it itself and binds to the server's `com.mojang.serialization` at runtime, since nothing
relocates it. Guava, Gson and SLF4J come from the Paper api and the server.

Shadow relocations, all below the prefix `Constants.RELOCATION_PREFIX`
(`dev.slne.surf.api.libs`, from the `relocationPrefix` property):

| From | To |
| --- | --- |
| `org.spongepowered.configurate` | `<prefix>.configurate` |
| `net.kyori.adventure.nbt` (except `net.kyori.adventure.nbt.api.**`) | `<prefix>.kyori.nbt` |
| `me.devnatan.inventoryframework` | `<prefix>.devnatan.inventoryframework` |
| `dev.wyck` | `<prefix>.dev.wyck` |

These match the relocations of surf-api's own jar, so a plugin's bytecode names the packages the
server really contains. Further relocations go directly on the plugin's `shadowJar` task.

`withApiValidation(vararg excludedMarkers)` turns on Kotlin's `abiValidation()`; the dump leaves
out declarations annotated with one of `excludedMarkers`. It excludes nothing by default.

`checkInternalApiUsage` runs with `check` and fails when the plugin's main classes reference a
dependency's internal API: a declaration, or a class enclosing it, annotated with an annotation that
carries `@InternalAPIMarker`, such as `@InternalSurfApi`, `@InternalRedisAPI` or another plugin's
own marker. An inherited member is checked where it is declared, and a default-argument call where
its function or constructor is declared. `@PublishedSurfApi` declarations pass, because inlined
surf-api code references them. References between the plugin's own classes pass too.
`@InternalNatsApi` carries no `@InternalAPIMarker`, so the check does not cover it. A surf library built against that internal API allows
the marker with `allowInternalApi("dev.slne.surf.api.shared.api.util.InternalSurfApi")`; an
`@OptIn` in the source does not exempt a reference.

## The generated plugin classes

`SurfPluginClassTasks` (`pluginclass/`) gives every Paper plugin its entry points. Every setting
below lives on `CoreSurfExtension`, so Velocity and Minestom plugins have them too.

| Setting | Default | Effect |
| --- | --- | --- |
| `basePackage` | the longest package shared by the plugin's own Kotlin and Java sources | where the generated classes and the library loader go, and where the component scan starts |
| `generatePluginClass` | `true` | `false` generates no class; the descriptor then takes `main` and `bootstrapper` from the build script alone |
| `verifyPluginClasses` | `true` | `false` skips `checkSurfPluginClasses` |
| `allowBundledSpring` | `false` | `true` skips `checkNoBundledSpring` |

`basePackage` is a `Property<String>`, set as `basePackage.set("dev.slne.example.lobby")` or
`basePackage = "dev.slne.example.lobby"`. The build fails when the sources share no package, when
the package is not a valid name, or when it equals or contains `dev.slne.surf.api` (for example
`dev.slne.surf`), because the scan would then include surf-api itself. It warns when an explicit
package misses some source packages, because their components are then not scanned.

The tasks, all in the `surf-api` or `verification` group:

| Task | What it does |
| --- | --- |
| `computeSurfBasePackage` | resolves the base package and writes it to `build/generated/surf-api/base-package/base-package.txt` |
| `generateSurfPluginClasses` | writes `GeneratedSurfBootstrapper` (extends `SurfPaperBootstrapper`) and `GeneratedSurfPlugin` (extends `SurfPaperPlugin`) as Java sources into `build/generated/surf-api/paper-plugin`, skipping the one the build script names |
| `checkSurfPluginClasses` | checks from the compiled classes that a class named with `mainClass(...)` extends `SurfPaperPlugin` and one named with `bootstrapper(...)` extends `SurfPaperBootstrapper`; runs with `check` and before `jar` and every shadow jar |
| `checkNoBundledSpring` | fails when the shadow jar would bundle Spring: an `org.springframework*` module, or an unrelocated `org/springframework/` entry at the root or under `META-INF/versions/<n>/` of any bundled archive. Runs with `check` and before the shadow jar tasks, and only while surf-api is on `compileOnly` |

A named class wins over the generated one. A named bootstrapper also moves the component scan to
its own package, and `basePackage` then has no effect on the scan.

## The generated `paper-plugin.yml`

Paper plugin descriptions come from `de.eldoria.plugin-yml.paper`, which the surf plugin applies and
then configures in `afterEvaluate` through `configure<PaperPluginDescription>`. The fields surf
sets, and therefore the ones to set through `surfPaperPluginApi { }` rather than `paper { }`:

| Field | Source |
| --- | --- |
| `main` | `mainClass(...)`, else `<basePackage>.GeneratedSurfPlugin` |
| `bootstrapper` | `bootstrapper(...)`, else `<basePackage>.GeneratedSurfBootstrapper` |
| `authors` | `authors` (a `ListProperty<String>`, convention `["SLNE Development"]`) |
| `api-version` | always `Constants.MINECRAFT_VERSION` - `26.3` in this repository |
| `folia-supported` | `foliaSupported(...)`, convention `true`; forced true by `useCanvasMc()` |
| `loader` | `<basePackage>.PaperLibraryLoader`, only with `generateLibraryLoader(true)` |
| `bootstrap-dependencies` | `surf-paper-api` (required), your `bootstrapDependencies { }` block, then the `surfPluginDependency` entries |
| `dependencies.server` | `surf-paper-api` (required), `surf-core-paper` when a core module is set, `surf-npc-paper`, your `serverDependencies { }` block, the `surfPluginDependency` entries, then `CommandAPI` (required) unless the block declared it |

`surfPluginDependency(name)` declares another surf plugin as bootstrap **and** server dependency,
required, with `joinClasspath = true` and `load = BEFORE`; `softSurfPluginDependency(name)` does the
same with `required = false`. Both are needed for a plugin that injects another surf plugin's
`@PluginApi` beans: the bootstrap dependency makes that plugin's context exist when this one
bootstraps. Use them only for surf plugins - a required bootstrap dependency on a plugin without a
bootstrapper makes Paper skip the dependent plugin. CommandAPI is always a server dependency
because Paper does not pass CommandAPI's classes on through surf-api.

Because surf configures these in `afterEvaluate`, a value set earlier in a `paper { }` block is
overwritten. Everything surf does not touch - `name`, `version`, `description`, `website`,
`permissions`, `load`, `prefix` - stays with plugin-yml and belongs in `paper { }`; surf fills
`name`, `version`, `description`, `website` and `author` from the project when they are unset.
`website` falls back to the `url` Gradle property (`providers.gradleProperty`, so an `ext` property
does not count) and then to the `origin` Git remote as an `https` URL without credentials, port or
`.git` suffix. Git runs only when neither is set.

Dependency blocks take the plugin-yml container, so either form works:

```kotlin
import net.minecrell.pluginyml.paper.PaperPluginDescription

surfPaperPluginApi {
    serverDependencies {
        register("LuckPerms") {
            required = true
            joinClasspath = true
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
    }
}
```

`dev.slne.surf.api.gradle.util.registerRequired(name)` and `registerSoft(name)` are the shorthands
the plugin itself uses; both default to `joinClasspath = true` and `load = BEFORE`.

The generating task is plugin-yml's `generatePaperPluginDescription`. Every
`GeneratePluginDescription` task gets `useDefaultCentralProxy()`.

## The library loader

With `generateLibraryLoader(true)`:

1. `generateLibrariesJson = true` makes plugin-yml write a `paper-libraries.json` resource into the
   jar.
2. `generateLibrariesLoader` (a `GenerateLibrariesLoaderTask`, group `surf-api`) generates a Java
   `PaperLibraryLoader` into the base package and adds the output to the `main` source set.
3. That class implements Paper's `PluginLoader`, reads `/paper-libraries.json` with Gson and feeds
   its repositories and dependencies into a `MavenLibraryResolver`. A missing or empty resource
   yields an empty set instead of failing.
4. The generated class name is written into the description's `loader` field.

## Folia and CanvasMC

`foliaSupported` defaults to `true`, and the api's own plugin sets it too, so plugin code must
assume region threading. `SurfApiPaper.isFolia` answers it at runtime, and
`dev.slne.surf.api.paper.region.TickThreadGuard` asserts thread ownership.

To get onto the right thread from a coroutine, inject the plugin's `PaperDispatchers`
(`dev.slne.surf.api.paper.coroutine`): `main` is the server thread (on Folia the global region),
`region(location)` and `region(world, chunkX, chunkZ)` the owner of a chunk, `entity(entity)` the
owner of an entity, and `async` is `Dispatchers.Default`. The dispatchers park work until the
plugin is enabled and reject it once the plugin disables; a rejected block cancels its coroutine
instead of being dropped. `entity.withEntity { }` runs a non-suspending block on the entity's thread
and never runs it against a removed entity. `Int.ticks` converts ticks to a `Duration` at 50 ms
each.

```kotlin
@Service
class Healer(private val scope: CoroutineScope, private val dispatchers: PaperDispatchers) {

    fun healLater(player: Player) = scope.launch {
        delay(20.ticks)
        withContext(dispatchers.entity(player)) { player.health = 20.0 }
    }
}
```

Code that is not a bean has no plugin context to inject from; launch the work from a bean instead.

Recurring work is a `@Scheduled` method of a bean. `PaperSchedulers.GLOBAL_REGION`
(`dev.slne.surf.api.paper.coroutine`) runs it on `PaperDispatchers.main`: the server thread, or on
Folia the global region, where it must not touch regions or entities. There are no region or
entity schedulers; switch inside the method with `withContext(dispatchers.region(...))` or
`entity.withEntity { }`. Without `scheduler` the method runs on `Dispatchers.Default`.

```kotlin
@Service
class Announcer(private val server: Server) {

    @Scheduled(
        fixedRate = 5,
        timeUnit = TimeUnit.MINUTES,
        scheduler = PaperSchedulers.GLOBAL_REGION,
    )
    fun announce() {
        server.broadcast(Component.text("Vote for the server!"))
    }
}
```

`useCanvasMc()` swaps the `paper-api` compile dependency for `Constants.CANVAS_API`, adds the Canvas
repository, forces `foliaSupported = true`, and resolves the `org.bukkit:bukkit` capability conflict
in favour of the Canvas api. `SurfApiPaper.isCanvasMc` is the runtime probe.

## Testing

`CommonSurfExtension.testing` (a `SurfTestingExtension`) is **on by default**. It gives the project
JUnit 5, mockk and lincheck with versions baked into the plugin, and `AbstractPaperSurfPlugin`
adds the Paper (or Canvas) api to `testImplementation`. There is no MockBukkit: a test that needs
Bukkit statics mocks them with mockk, `mockkStatic(Bukkit::class)`. When the api is on
`compileOnly`/`compileOnlyApi`, it is mirrored onto `testImplementation` so tests can see it.
`SurfTestingConfigurer.addSurfRuntimeLibraries` puts the libraries the surf-api Paper runtime
ships on `testRuntimeOnly`: `Constants.COMMON_RUNTIME_TEST_LIBRARIES` and
`PAPER_RUNTIME_TEST_LIBRARIES`, generated from the version catalog by the plugin's
`generateConstants`. Velocity and Minestom plugins do the same with their own lists; Velocity also
puts `velocity-api` on `testImplementation`. `PluginTestRuntimeBuildTest` covers all three.
`testing { enabled.set(false) }` switches all of it off.

The api's implementation only exists at runtime through the `surf-paper-api` plugin, so a unit test
can exercise contracts and pure helpers but not the service-backed Paper features.

## Inside this repository

`local/paper-plugin-test` does **not** apply the published Gradle plugin - inside this build it
applies `core-convention` and the plugin-yml plugin directly, names its own `PaperPluginMain` and
`PaperPluginBootstrapper` in `paper { }`, and declares `compileOnlyApi(projects.surfApiPaperApi)`.
Being part of surf-api's build, it opts into `@InternalSurfApi` and reads beans through `surfBean`
in places; a downstream plugin injects them instead. Treat it as a usage sample for the api, not as
a template for a downstream `build.gradle.kts`.
