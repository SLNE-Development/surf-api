# buildSrc conventions

Everything here lives in `buildSrc/src/main/kotlin/`. `buildSrc/build.gradle.kts` puts the Kotlin,
Shadow, KSP, serialization, all-open, ASM and Gremlin Gradle plugins plus the version catalog
accessors on the convention classpath.

## Plugin graph

```
surf.module-api   ->  core-convention
surf.module-impl  ->  core-convention
surf.aggregate    ->  core-convention
api-validation    ->  core-convention          (opt-in, applied next to a role)
vendor-bundle-convention -> core-convention    (opt-in)
surf.ksp-processor -> com.google.devtools.ksp  (opt-in, applied next to a role)

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

## surf.ksp-processor

```kotlin
plugins { id("com.google.devtools.ksp") }
dependencies { ksp(project(":surf-api-processor")) }
```

The processor generates the implementations behind `@GenerateReflection` interfaces and the NMS
provider index under `META-INF/surf-api/nms-providers` for `@NmsProviderMarker` classes. Every
module that declares either annotation applies it next to its role; the four
`platform/paper/nms/v*` modules do, and the nms-module-generator template emits it. A module that
forgets it still compiles, but its `@GenerateReflection` interfaces get no generated
implementation and `NmsProvider` fails with "No NmsProvider implementations found" at runtime.

The infra modules and local test plugins that apply the surf Gradle plugin get KSP from that
plugin instead.

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
`java` component. `relocation.kt` adds extra publications on top of this one.

## api-validation

```kotlin
extensions.configure<KotlinJvmProjectExtension> { abiValidation() }
```

That is the whole plugin: the experimental Kotlin ABI validation, opted in with
`@file:OptIn(ExperimentalAbiValidation::class)`. It gives the project `checkKotlinAbi` and
`updateKotlinAbi`, and a dump at `<moduleDir>/api/<project.name>.api`.

Applied by `foundation/annotations`, `foundation/spring/api`, the `common/*/api` modules (`common`,
`messages`, `event`, `nbt`, `reflection`, `command`, `luckperms`, `core`, `inventory`,
`serialization`, `config`), `platform/paper/api`, `platform/minestom/api`, `platform/velocity/api`,
`platform/minestom/impl` and `platform/standalone/impl`.

The infra modules configure the same Kotlin feature without buildSrc: a
`kotlin { abiValidation { filters { exclude { annotatedWith.add(...) } } } }` block in the four
`infra/microservice` modules, `surf-nats-api` and `surf-redis-api`, and
`surfCoreApi { withApiValidation() }` in `surf-redis-api`, `surf-redis-spring`, `surf-nats-spring`
and `infra/database-r2dbc`. Every checked-in dump is a file matching `*/api/*.api`;
`surf-nats-api` also has a `.klib.api` for its JS target.

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
`surf-api-internal`, which holds the NMS provider metadata shared by the KSP processor and the
provider loader. `-nms-v<digit>` matches the four NMS bridges. `AGGREGATE` is never returned by this
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

## surf.aggregate and surfRuntime

```kotlin
const val SURF_RUNTIME = "surfRuntime"
```

`surf.aggregate` registers it as resolvable-but-not-consumable, makes `runtimeOnly` extend from it,
and in `afterEvaluate` requires that no source set has a non-empty source directory:

```
:surf-api-paper applies surf.aggregate but carries sources: src/main/kotlin. An aggregate only declares dependencies.
```

API modules go on `api` (consumer compile classpath) and implementations on `surfRuntime` (shipped,
never visible at compile time). No aggregate declares a `surfRuntime` dependency.

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

## relocation.kt

```kotlin
fun Project.publishRelocationFrom(vararg oldArtifactIds: String)
```

For each old artifact id it creates an extra `MavenPublication` named `relocation` + the id in
PascalCase, with `artifactId` set to the old id and a POM carrying
`<distributionManagement><relocation>` pointing at `project.group:project.name:project.version` and
the message "The module was renamed to <name>." A consumer resolving the old coordinate gets a POM
with no artifact, so the build keeps resolving and the warning names the new coordinate.

It requires that the old id differs from the module's own name. Users, all from the Minestom
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

Consumers of the surf Gradle plugin resolve those coordinates directly, which is why they need a
marker. The renamed `api`/`impl` pairs behind an aggregate do not: a consumer only ever sees the
aggregate coordinate.

## buildSrc tests

`buildSrc/src/test/kotlin/` holds `CheckShadowServiceFileTest` and `SpringRegistriesTest`. They run
with `./gradlew -p buildSrc test`, never as part of the main build's `check`.
