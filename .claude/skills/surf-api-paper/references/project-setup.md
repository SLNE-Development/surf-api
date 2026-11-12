# Setting up a Paper project against surf-api

Source: `tooling/gradle-plugin/src/main/kotlin/dev/slne/surf/api/gradle/platform/paper/`,
`.../platform/common/CommonSurfPlugin.kt`, `.../platform/core/CoreSurfPlugin.kt`,
`.../generators/LibrariesLoaderGenerator.kt` and `tooling/gradle-plugin/build.gradle.kts`.

## The plugin ids

| Id | Implementation | Extension |
| --- | --- | --- |
| `dev.slne.surf.api.gradle.paper-plugin` | `PaperPluginSurfPlugin` | `surfPaperPluginApi { }` |
| `dev.slne.surf.api.gradle.paper-raw` | `RawPaperSurfPlugin` | `surfRawPaperApi { }` |
| `dev.slne.surf.api.gradle.settings` | `SurfSettingsPlugin` | - (settings plugin; only enables `TYPESAFE_PROJECT_ACCESSORS`) |

The published version of the Gradle plugin is `3.0.0` (`tooling/gradle-plugin/build.gradle.kts`),
with `-SNAPSHOT` appended when the `snapshot` property is true.

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
  and `javaParameters = true`
- `JavaCompile` gets UTF-8 and `-parameters`
- `withSourcesJar()` and `withJavadocJar()`
- the `surf-api-processor` KSP processor, added to the `ksp` configuration
  (`configureSurfProcessor`). It is what writes the module's bean metadata, so without it a
  `@Service` in a downstream plugin compiles and is then simply absent at runtime - the container
  builds its graph from that metadata and never scans classes.
- `gradleApi()` and `gradleTestKit()` are removed from every configuration except `compileOnly`

Dependencies added in `afterEvaluate`:

- `dev.slne.surf.api:surf-api-paper:+` at `compileOnly` (`SurfApiPlatform.PAPER`). The version is
  the literal `+`: `Constants.SURF_API_VERSION` is `"+"`, so the newest published api is resolved.
  `addSurfApiToClasspath(false)` suppresses it, `surfApiScope("api")` changes the scope.
- `io.papermc.paper:paper-api` (`Constants.PAPER_API`), or `io.canvasmc.canvas:canvas-api` with
  `useCanvasMc()`, at `compileOnly`.
- When `withCorePaper()` was called: `dev.slne.surf.core:surf-core-api-paper:+` at `compileOnly`.
  The sibling calls are `withCoreCommon()`, `withCoreVelocity()`, `withCoreMinestom()`.
- `withSurfRedis()` adds `dev.slne.surf.redis:surf-redis-api:+` at `compileOnly`;
  `withSurfDatabaseR2dbc(version, relocation)` adds `dev.slne.surf:surf-database-r2dbc:+` at `api`
  and relocates `dev.slne.surf.database`.
- `withSurfNpc()` adds `dev.slne.surf.npc:surf-npc-api:+` at `compileOnly` and registers
  `surf-npc-paper` as a required server dependency.

Shadow relocations, all below the prefix `Constants.RELOCATION_PREFIX`
(`dev.slne.surf.api.libs`, from the `relocationPrefix` property):

| From | To |
| --- | --- |
| `org.spongepowered.configurate` | `<prefix>.configurate` |
| `com.mojang.serialization` | `<prefix>.mojang.serialization` |
| `com.mojang.datafixers` | `<prefix>.mojang.datafixers` |
| `net.kyori.adventure.nbt` (except `net.kyori.adventure.nbt.api.**`) | `<prefix>.kyori.nbt` |
| `me.devnatan.inventoryframework` | `<prefix>.devnatan.inventoryframework` |
| `dev.wyck` | `<prefix>.dev.wyck` |

`withApiValidation()` turns on Kotlin's `abiValidation()`.

## The generated `paper-plugin.yml`

Paper plugin descriptions come from `de.eldoria.plugin-yml.paper`, which the surf plugin applies and
then configures in `afterEvaluate` through `configure<PaperPluginDescription>`. The fields surf
sets, and therefore the ones to set through `surfPaperPluginApi { }` rather than `paper { }`:

| Field | Source |
| --- | --- |
| `main` | `mainClass(...)` - required |
| `bootstrapper` | `bootstrapper(...)` - optional |
| `authors` | `authors` (a `ListProperty<String>`, convention `["SLNE Development"]`) |
| `api-version` | always `Constants.MINECRAFT_VERSION` - `26.3` in this repository |
| `folia-supported` | `foliaSupported(...)`, convention `true`; forced true by `useCanvasMc()` |
| `loader` | only with `generateLibraryLoader(true)` |
| `bootstrap-dependencies` | `surf-paper-api` (required), `surf-redis-paper` when redis is used unrelocated, plus your `bootstrapDependencies { }` block |
| `dependencies.server` | `surf-paper-api` (required), `surf-core-paper` when a core module is set, `surf-redis-paper`, `surf-npc-paper`, plus your `serverDependencies { }` block |

Because surf configures these in `afterEvaluate`, a value set earlier in a `paper { }` block is
overwritten. Everything surf does not touch - `name`, `version`, `description`, `website`,
`permissions`, `load`, `prefix` - stays with plugin-yml and belongs in `paper { }`.

Dependency blocks take the plugin-yml container, so either form works:

```kotlin
import net.minecrell.pluginyml.paper.PaperPluginDescription

surfPaperPluginApi {
    mainClass("dev.slne.example.lobby.LobbyPlugin")

    serverDependencies {
        register("CommandAPI") {
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
   `PaperLibraryLoader` into the *parent package of your main class* - `mainClass` minus its last
   segment - and adds the output to the `main` source set.
3. That class implements Paper's `PluginLoader`, reads `/paper-libraries.json` with Gson and feeds
   its repositories and dependencies into a `MavenLibraryResolver`. A missing or empty resource
   yields an empty set instead of failing.
4. The generated class name is written into the description's `loader` field.

## Folia and CanvasMC

`foliaSupported` defaults to `true`, and the api's own plugin sets it too, so plugin code must
assume region threading. `SurfApiPaper.isFolia` answers it at runtime, and
`dev.slne.surf.api.paper.region.TickThreadGuard` asserts thread ownership. mccoroutine's
folia dispatchers (`plugin.entityDispatcher(entity)`, `plugin.regionDispatcher(location)`) are the
way to get onto the right thread from a coroutine - `local/paper-plugin-test` uses both.

`useCanvasMc()` swaps the `paper-api` compile dependency for `Constants.CANVAS_API`, adds the Canvas
repository, forces `foliaSupported = true`, and resolves the `org.bukkit:bukkit` capability conflict
in favour of the Canvas api. `SurfApiPaper.isCanvasMc` is the runtime probe.

## Testing

`CommonSurfExtension.testing` (a `SurfTestingExtension`) is **on by default**. It gives the project
JUnit 5, mockk and lincheck with versions baked into the plugin, and `AbstractPaperSurfPlugin`
adds the Paper (or Canvas) api to `testImplementation`.

```kotlin
surfPaperPluginApi {
    mainClass("dev.slne.example.lobby.LobbyPlugin")

    testing {
        mockBukkit.set(true)   // off by default
    }
}
```

`mockBukkit` resolves `org.mockbukkit.mockbukkit:mockbukkit-v<major.minor>` from
`Constants.MINECRAFT_VERSION`. When the api is on `compileOnly`/`compileOnlyApi`, it is mirrored
onto `testImplementation` so tests can see it. `testing { enabled.set(false) }` switches all of it
off.

Note that the api's implementation only exists at runtime through the `surf-paper-api` plugin, so a
unit test can exercise contracts and pure helpers but not the service-backed Paper features.

## Inside this repository

`local/paper-plugin-test` does **not** apply the published Gradle plugin - inside this build it
applies `core-convention` and the plugin-yml plugin directly, and declares
`compileOnlyApi(projects.surfApiPaperApi)`. Treat it as a usage sample for the api, not as a
template for a downstream `build.gradle.kts`.
