# buildSrc conventions

Everything here lives in `buildSrc/src/main/kotlin/`. `buildSrc/build.gradle.kts` puts the Kotlin,
Shadow, KSP, serialization, ASM and Gremlin Gradle plugins plus the version catalog accessors on the
convention classpath.

## Plugin graph

```
surf.module-api  ->  core-convention
surf.module-impl ->  core-convention
surf.aggregate   ->  core-convention
api-validation   ->  core-convention          (opt-in, applied next to a role)
vendor-bundle-convention -> core-convention   (opt-in)

core-convention  ->  publish-convention, java-toolchain-convention, test-convention,
                     java, java-library, kotlin("jvm"), kotlin("plugin.serialization"),
                     com.google.devtools.ksp, com.gradleup.shadow
```

## core-convention

- Sets `group` from `gradle.properties` and `version` from `version` plus `-SNAPSHOT` when
  `snapshot=true`.
- Repositories: Maven Central and `https://reposilite.slne.dev/public`. The root `build.gradle.kts`
  adds PaperMC, the Minestom snapshot repository and `repo.wyck.dev` to `allprojects`.
- `compileOnlyApi("org.jetbrains:annotations:...")`.
- `ksp(project(":surf-api-processor"))` for every module **except** `surf-api-annotations`,
  `surf-api-internal` and `surf-api-di-api`: the processor depends on those three for the
  annotations it looks for and the metadata schema it writes, so running it on them would be a
  cycle.
- `-Xjsr305=strict`, sources jar for everyone, javadoc jar for everyone except
  `surf-api-paper-plugin-test`.
- `compileOnly` extends `annotationProcessor`.
- `shadowJar` merges service files and relocates `net.kyori.adventure.nbt` (excluding
  `...nbt.api.**`) and `org.spongepowered.configurate` under `relocationPrefix`
  (`dev.slne.surf.api.libs`). A module adding its own relocations does so in its own
  `tasks { shadowJar { ... } }` block - `common/core/api` relocates the Mojang packages,
  `platform/paper/impl` relocates the inventory framework and `dev.wyck`.

The root `build.gradle.kts` also adds, for every subproject with a Kotlin extension,
`optIn("dev.slne.surf.api.shared.api.util.InternalSurfApi")` and `-Xcontext-parameters`, and strips
`gradleApi()`/`gradleTestKit()` from every configuration except `compileOnly`.

## java-toolchain-convention

Java and Kotlin toolchain from the `javaVersion` property in `gradle.properties` (currently 25).

## test-convention

`testImplementation` extends `compileOnlyApi`, so a dependency a module only exposes at compile time
is still on the test classpath. Adds kotlin-test, JUnit Jupiter, MockK, Lincheck and
kotlinx-coroutines-test; `useJUnitPlatform()` and `-XX:+EnableDynamicAgentLoading` for Lincheck's
dynamically attached agent.

`platform/minestom/impl` additionally applies `java-test-fixtures`; `npc-impl` and `plugins-impl`
consume them with `testImplementation(testFixtures(projects.surfApiMinestomImpl))`.

## publish-convention

`maven-publish`, the `slne-repository-releases` Reposilite repository with credentials from
`SLNE_RELEASES_REPO_USERNAME` / `SLNE_RELEASES_REPO_PASSWORD`, and one `maven` publication from the
`java` component. `relocation.kt` adds extra publications on top of this one.

## api-validation

```kotlin
extensions.configure<KotlinJvmProjectExtension> { abiValidation() }
```

That is the whole plugin - the experimental Kotlin ABI validation, opted in with
`@file:OptIn(ExperimentalAbiValidation::class)`. It gives the project `checkKotlinAbi` and
`updateKotlinAbi`, and a dump at `<moduleDir>/api/<project.name>.api`.

Applied by: `foundation/annotations`, `common/core/api`, `platform/paper/api`,
`platform/minestom/api`, `platform/minestom/impl`, `platform/velocity/api`,
`platform/standalone/impl`. Checked-in dumps:

```
common/core/api/api/surf-api-core-api.api
foundation/annotations/api/surf-api-annotations.api
platform/minestom/api/api/surf-api-minestom-api.api
platform/minestom/impl/api/surf-api-minestom-impl.api
platform/paper/api/api/surf-api-paper-api.api
platform/standalone/impl/api/surf-api-standalone-impl.api
platform/velocity/api/api/surf-api-velocity-api.api
```

`surf.module-api`'s own doc comment states why this is not bundled into the api role: a module whose
surface is generated from the Minecraft version has a dump that changes with every version bump and
tracks nothing worth reviewing, while an implementation module whose surface an aggregate re-exports
does want one.

## vendor-bundle-convention

Used by `platform/minestom/luckperms-impl` and `platform/minestom/spark-impl`. It creates a
`bundled` configuration (resolvable, not consumable) that `compileOnly` and `testImplementation`
extend from, and copies the resolved classes into the module's own `jar`. The vendor builds are not
published anywhere, so they cannot appear as dependencies of a published module.

Not bundled: everything a Minestom server already provides (`org.slf4j`, `org.jspecify`,
`org.jetbrains*`, `org.checkerframework`, Gson, Guava, errorprone), most Adventure modules, and
`org.ow2.asm` - a bundled ASM would land on the system class path ahead of the ASM the server's
library download relocates with.

## SurfModuleRole

```kotlin
enum class SurfModuleRole { API, IMPL, AGGREGATE }

private val NMS_VERSION_MODULE = Regex("-nms-v[0-9]")

fun surfModuleRole(projectName: String): SurfModuleRole = when {
    projectName.endsWith("-impl") -> SurfModuleRole.IMPL
    projectName.contains("-server") -> SurfModuleRole.IMPL
    projectName.endsWith("-internal") -> SurfModuleRole.IMPL
    NMS_VERSION_MODULE.containsMatchIn(projectName) -> SurfModuleRole.IMPL
    else -> SurfModuleRole.API
}
```

`-impl` is the target spelling. The other three are legacy shapes the file documents as such; of
them, `-internal` currently matches only `surf-api-internal`, `-nms-v<digit>` matches the four NMS
bridges, and `-server` matches nothing but the `surf-api-minestom-server` **aggregate** - which
therefore classifies as `IMPL`. Nothing depends on that aggregate from an API module today, but a
dependency on it would be reported as an api-boundary violation.

`AGGREGATE` is never returned by this function; it exists for the role declared by applying
`surf.aggregate`.

## surf.module-impl

`core-convention` plus a settings-time assertion:

```kotlin
require(surfModuleRole(project.name) == SurfModuleRole.IMPL) { ... }
```

Without it, a misnamed implementation module would classify as `API` and `checkApiBoundary` in every
module depending on it would pass for the wrong reason.

## surf.module-api and checkApiBoundary

`surf.module-api` registers a `CheckApiBoundary` task in the `verification` group, wires it into
`check`, and fills it in `afterEvaluate` because the consuming build file's `dependencies` block runs
after the plugin is applied.

Checked configurations - the ones whose declarations reach a consumer's compile or runtime classpath:

```kotlin
val checkedConfigurations = listOf(
    "api", "implementation", "compileOnly", "compileOnlyApi", "runtimeOnly",
)
```

For each of those that exists, it takes the `ProjectDependency` entries, keeps the ones whose name
`surfModuleRole` calls `IMPL`, and records `"${configuration.name} -> ${it.name}"`. Only project
dependencies are inspected; external modules are irrelevant, and `testImplementation` and friends are
not checked.

`CheckApiBoundary.kt` takes the violations as plain strings so the task action touches no project
state and stays configuration-cache compatible. Clean output:

```
:surf-api-core-api: api boundary clean
```

Failure output:

```
:surf-api-foo-api is an API module but depends on implementation modules:
  - api -> surf-api-bar-impl
An API module may only depend on other API modules. Implementations are wired in by the aggregator
through the surfRuntime configuration.
```

`strict` comes from `providers.gradleProperty("surf.apiBoundary.strict").map { it.toBooleanStrict() }
.orElse(true)`: it throws by default, and `-Psurf.apiBoundary.strict=false` logs a warning instead.
The flag is for a transitional local build, not a fix.

## surf.aggregate and surfRuntime

```kotlin
const val SURF_RUNTIME = "surfRuntime"
```

`surf.aggregate` registers it as resolvable-but-not-consumable, makes `runtimeOnly` extend from it,
and in `afterEvaluate` requires that no source set has a non-empty source directory:

```
:surf-api-paper applies surf.aggregate but carries sources: src/main/kotlin.
An aggregate only declares dependencies.
```

The intent is that API modules go on `api` (consumer compile classpath) and the matching
implementations on `surfRuntime` (shipped, never visible at compile time). No aggregate declares a
`surfRuntime` dependency yet.

## relocation.kt

```kotlin
fun Project.publishRelocationFrom(vararg oldArtifactIds: String)
```

For each old artifact id it creates an extra `MavenPublication` named `relocation` + the id in
PascalCase, with `artifactId` set to the old id and a POM carrying
`<distributionManagement><relocation>` pointing at `project.group:project.name:project.version` and
the message "The module was renamed to <name>." A consumer resolving the old coordinate gets a POM
with no artifact, so the build keeps resolving and the warning names the new coordinate.

It requires that the old id differs from the module's own name. Current users, all from the Minestom
restructure:

```
surf-api-minestom-bootstrap-impl          <- surf-api-minestom-server-bootstrap
surf-api-minestom-console-impl            <- surf-api-minestom-server-console
surf-api-minestom-luckperms-impl          <- surf-api-minestom-server-luckperms
surf-api-minestom-npc-impl                <- surf-api-minestom-server-npc
surf-api-minestom-player-visibility-impl  <- surf-api-minestom-server-player-visibility
surf-api-minestom-plugins-impl            <- surf-api-minestom-server-plugins
surf-api-minestom-signed-chat-impl        <- surf-api-minestom-server-signed-chat
surf-api-minestom-spark-impl              <- surf-api-minestom-server-spark
```

Those coordinates are resolved directly by consumers of the surf-api Gradle plugin, which is why
they need a marker. The renamed `api`/`impl` pairs behind an aggregate do not: a consumer only ever
sees the aggregate coordinate.
