# buildSrc conventions

Everything here lives in `buildSrc/src/main/kotlin/`. `buildSrc/build.gradle.kts` puts the Kotlin,
Shadow, KSP, serialization, all-open, ASM and Gremlin Gradle plugins plus the version catalog
accessors on the convention classpath.

## Plugin graph

```
surf.module-api   ->  core-convention
surf.module-impl  ->  core-convention
surf.aggregate    ->  core-convention
api-validation    ->  kotlin("jvm")            (opt-in, applied next to a role or the surf plugin)
vendor-bundle-convention -> core-convention    (opt-in)
surf.nms-shared-26 -> kotlin("jvm")           (opt-in, the 26.x NMS modules)

core-convention   ->  publish-convention, java-toolchain-convention, test-convention,
                      java, java-library, kotlin("jvm"), kotlin("plugin.serialization"),
                      kotlin("plugin.spring"), com.gradleup.shadow
```

## core-convention

- Sets `group` from `gradle.properties` and `version` from `version` plus `-SNAPSHOT` when
  `snapshot=true`.
- Repositories: Maven Central and `https://reposilite.slne.dev/public`. The root `build.gradle.kts`
  adds PaperMC, the Minestom snapshot repository and `repo.wyck.dev` to `allprojects`.
- `compileOnlyApi("org.jetbrains:annotations:...")`.
- `kotlin("plugin.spring")` opens classes carrying a Spring stereotype or `@Configuration`, which
  Spring subclasses for proxies and configuration enhancement.
- Compiler options `-Xjsr305=strict` and `javaParameters = true`; Spring matches constructor and
  method parameters by name.
- Sources jar for everyone, javadoc jar for everyone except `surf-api-paper-plugin-test`.
- `compileOnly` extends `annotationProcessor`.
- `shadow { addShadowVariantIntoJavaComponent = false }`: the shaded jar bundles Spring unrelocated,
  so it is not published under the module's coordinate. `shadowJar` and the
  `shadowRuntimeElements` configuration remain for in-build consumers and release assets.
- Every plain `Jar` task uses `DuplicatesStrategy.EXCLUDE`.
- `shadowJar` merges service files and includes every copy of `META-INF/services/**`
  (`DuplicatesStrategy.INCLUDE`). Shadow's default `EXCLUDE` would drop later copies before the
  transformer runs, losing providers such as the `ContextContributor` registrations of bundled
  modules. It also relocates `net.kyori.adventure.nbt` (excluding `...nbt.api.**`) and
  `org.spongepowered.configurate` under `relocationPrefix` (`dev.slne.surf.api.libs`).

A module adds its own relocations in its own `tasks { shadowJar { ... } }` block;
`platform/paper/impl` relocates the inventory framework and `dev.wyck`.

The root `build.gradle.kts` also adds, for every subproject with a Kotlin extension,
`optIn("dev.slne.surf.api.shared.api.util.InternalSurfApi")` and `-Xcontext-parameters`, and strips
`gradleApi()`/`gradleTestKit()` from every configuration except `compileOnly`.

## surf.nms-shared-26

Compiles `platform/paper/nms/shared-26/src/main/kotlin` into the module and fails configuration
for a project name without `-nms-v26-<minor>`. The shared sources are written as the version that
`shared-26/template.properties` names (`v26_3`). That module adds the directory as a Kotlin source
directory of `main` as it is and fails configuration if it holds a file with the path of a shared
one. Every other module registers `copyNmsTemplate`, a `Sync` to
`build/generated/nms-template/kotlin` that replaces the template's package segment and class
prefix in paths and content with the module's own, derived from the project name
(`surf-api-paper-nms-v26-1` gives `v26_1` and `V26_1`), and adds that directory instead. A file in
such a module's own `src/main/kotlin` with the same path replaces the shared file. The nms-module-generator template applies it to every 26.x
module it creates.

The infra modules and local test plugins that apply the surf Gradle plugin get the KSP Gradle
plugin from that plugin.

## java-toolchain-convention

Java and Kotlin toolchain from the `javaVersion` property in `gradle.properties`.

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
`java` component.

## api-validation

Applies `kotlin("jvm")` and nothing else, so it goes next to a role or, in infra, next to the surf
Gradle plugin. It turns on the experimental Kotlin ABI validation, excludes `@InternalSurfApi`
declarations from the dump and registers `checkInlineInternalApi`, wired into `check`. The project
gets `checkKotlinAbi` and `updateKotlinAbi`, and a dump at `<moduleDir>/api/<project.name>.api`.

Applied by `foundation/annotations`, `foundation/spring/api`, the `common/*/api` modules (`common`,
`messages`, `event`, `nbt`, `reflection`, `command`, `luckperms`, `core`, `inventory`,
`serialization`, `config`), `platform/paper/api`, `platform/minestom/api`, `platform/velocity/api`,
`platform/minestom/impl` and `platform/standalone/impl`, and next to the surf Gradle plugin by the
infra modules `surf-redis-api`, `surf-redis-spring`, `surf-nats-spring`, `infra/database-r2dbc`
and the four `infra/microservice` modules. Infra modules with an internal annotation of their own
exclude it in an additional `kotlin { abiValidation { filters { ... } } }` block.

`surf-nats-api` is Kotlin Multiplatform and keeps its own `abiValidation` block, which also writes a
`.klib.api` for its JS target. Every checked-in dump is a file matching `*/api/*.api`.

`surf.module-api`'s doc comment states why this is not bundled into the api role: a module whose
surface is generated from the Minecraft version has a dump that changes with every version bump,
while an implementation module whose surface an aggregate re-exports does want one.

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
    projectName.endsWith("-internal") -> SurfModuleRole.IMPL
    NMS_VERSION_MODULE.containsMatchIn(projectName) -> SurfModuleRole.IMPL
    else -> SurfModuleRole.API
}
```

`-impl` is the spelling for every platform and domain implementation. `-internal` matches only
`surf-api-internal`, which holds the `NmsVersion` list shared by the Paper runtime and the
nms-module-generator. `-nms-v<digit>` matches the four NMS bridges. `AGGREGATE` is never returned by this
function; it exists for the role declared by applying `surf.aggregate`, so the
`surf-api-minestom-server` aggregate classifies as `API` by name.

## surf.module-impl

`core-convention` plus a configuration-time assertion:

```kotlin
require(surfModuleRole(project.name) == SurfModuleRole.IMPL) { ... }
```

Without it, a misnamed implementation module would classify as `API`, and `checkApiBoundary` in
every module depending on it would pass for the wrong reason.

## surf.module-api and checkApiBoundary

`surf.module-api` registers a `CheckApiBoundary` task in the `verification` group, wires it into
`check`, and fills it in `afterEvaluate`, because the consuming build file's `dependencies` block
runs after the plugin is applied.

Checked configurations - the ones whose declarations reach a consumer's compile or runtime
classpath:

```kotlin
val checkedConfigurations = listOf(
    "api", "implementation", "compileOnly", "compileOnlyApi", "runtimeOnly",
)
```

For each of those that exists, it takes the `ProjectDependency` entries, keeps the ones whose name
`surfModuleRole` calls `IMPL`, and records `"${configuration.name} -> ${it.name}"`. Only project
dependencies are inspected; `testImplementation` and friends are not checked.

`CheckApiBoundary.kt` takes the violations as plain strings, so the task action touches no project
state and stays configuration-cache compatible. Clean output:

```
:surf-api-core-api: api boundary clean
```

Failure output:

```
:surf-api-foo-api is an API module but depends on implementation modules:
  - api -> surf-api-bar-impl
An API module may only depend on other API modules. Move the dependency to the matching
implementation module, or move the type the API needs into an API module of its own.
```

`strict` comes from `providers.gradleProperty("surf.apiBoundary.strict").map { it.toBooleanStrict() }
.orElse(true)`: it throws by default, and `-Psurf.apiBoundary.strict=false` logs a warning instead.

## surf.aggregate

`surf.aggregate` applies `core-convention` and in `afterEvaluate` requires that no source set has a
non-empty source directory:

```
:surf-api-paper applies surf.aggregate but carries sources: src/main/kotlin. An aggregate only declares dependencies.
```

Every aggregate exports its modules with plain `api(...)`. The runtime of `surf-api-standalone`
and `surf-api-minestom-server` comes from the implementation module each exports.

## Shadow-jar helpers

### SpringRegistries.kt

```kotlin
fun ShadowJar.mergeSpringRegistries()
```

Merges the Spring registries that many bundled jars extend:

- `META-INF/spring/*.imports` line by line
- `META-INF/spring.factories` and `META-INF/spring/aot.factories` per key, values joined with `,`
- `META-INF/spring.handlers` and `META-INF/spring.schemas` per key, keeping the first value -
  spring-aop, spring-beans and spring-context each ship a copy, and a joined value would name a
  class that does not exist

Exactly these files are included regardless of the task's duplicates strategy, so every copy
reaches the transformer. Used by the `shadowJar` of `platform/paper/impl`, `platform/velocity/impl`
and `platform/minestom/impl`. Covered by `buildSrc/src/test/kotlin/SpringRegistriesTest.kt`.

```kotlin
fun ShadowJar.excludeSpringBootLogging()
```

Excludes spring-boot's logging integration: `META-INF/services/org.apache.logging.log4j.util.PropertySource`
(turns off log4j's shutdown hook), `META-INF/services/ch.qos.logback.classic.spi.Configurator`
(sets logback's root level) and the log4j plugin cache `Log4j2Plugins.dat`, which no other bundled
jar ships. The same three modules call it next to `mergeSpringRegistries()`.

### CheckShadowServiceFile.kt

```kotlin
abstract class CheckShadowServiceFile : DefaultTask() {
    abstract val jar: RegularFileProperty
    abstract val service: Property<String>
    abstract val expectedProviders: ListProperty<String>
}
```

Fails with `<jar> lists no provider <names> in META-INF/services/<service>.` when the jar's service
file misses one of the expected providers. The three platform impl modules register it as
`checkContextContributors`, wired into `check`:

```kotlin
val checkContextContributors by tasks.registering(CheckShadowServiceFile::class) {
    group = "verification"
    jar.set(tasks.shadowJar.flatMap { it.archiveFile })
    service.set("dev.slne.surf.api.spring.context.ContextContributor")
    expectedProviders.addAll(
        "dev.slne.surf.api.config.impl.handle.SurfConfigContributor",
        "dev.slne.surf.api.nats.NatsContextContributor",
        "dev.slne.surf.api.redis.RedisContextContributor",
    )
}
tasks.check { dependsOn(checkContextContributors) }
```

Covered by `buildSrc/src/test/kotlin/CheckShadowServiceFileTest.kt`.

### NatsRelocation.kt

```kotlin
fun ShadowJar.relocateNats()
```

Relocates `io.nats` and `org.bouncycastle` below `dev.slne.surf.nats.libs` and excludes
`native/**/libbc-*`; JNI binds those libraries to the original class names, and without them
BouncyCastle uses its Java implementation. Used by `platform/paper/impl`,
`platform/velocity/impl` and every shadow jar under `infra/nats`, which also merges
`META-INF/services` so `java.security.Provider` names the relocated providers. Minestom keeps
`io.nats` and BouncyCastle unrelocated.

### shadowed.kt

```kotlin
fun Project.shadowed(dependency: ProjectDependency): ModuleDependency
```

Requests the `Bundling.SHADOWED` variant of a project dependency, for modules that publish only
their shaded jar: code compiled against their published coordinate sees the packages that jar
relocates, and inside this build it has to see the same ones. The platform impl modules take
`shadowed(projects.surfApiRedis.surfApiRedisSpring)`; the microservice modules take
`shadowed(projects.surfApiDatabaseR2dbc)`.

## buildSrc tests

`buildSrc/src/test/kotlin/` holds `CheckShadowServiceFileTest` and `SpringRegistriesTest`. They run
with `./gradlew -p buildSrc test`, never as part of the main build's `check`.
