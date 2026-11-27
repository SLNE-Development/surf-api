---
name: surf-api-build
description: Use when touching the surf-api Gradle build - adding, renaming, moving or splitting a module, writing or repairing a build.gradle.kts, choosing between surf.module-api, surf.module-impl and surf.aggregate, applying surf.nms-shared-26, fixing a checkApiBoundary, checkContextContributors or "applies surf.aggregate but carries sources" failure, merging Spring registries or service files into a shadow jar, turning ABI validation on or off, working in the imported infra/ builds, deciding whether a module belongs in surf-api-bom, or working out a module's published coordinate and Gradle path.
---

# surf-api build and module layout

Every structural fact below comes from `settings.gradle.kts`, the root `build.gradle.kts`,
`gradle.properties` and `buildSrc/src/main/kotlin/`. Read those files when in doubt; they are the
only source of truth.

## Where modules live

The first path segment groups modules and carries no meaning beyond grouping:

| Directory | What it holds |
| --- | --- |
| `foundation/` | `annotations`, `internal`, `registry-keys`, `spring/api`, `spring/impl` |
| `common/` | the platform-independent domains: `common`, `messages`, `event`, `nbt`, `reflection`, `command`, `luckperms`, `inventory`, `serialization` (api only), and the `core` and `config` api/impl pairs |
| `platform/` | `paper/`, `minestom/`, `velocity/`, `standalone/`, plus the `paper/nms/` bridges |
| `aggregate/` | the six coordinates downstream projects depend on; no code |
| `infra/` | the imported NATS, Redis and microservice builds and `database-r2dbc`, each with its own root script |
| `tooling/` | `bom` and the local-only `registry-key-generator`; `tooling/gradle-plugin` and `tooling/nms-module-generator` are included builds |
| `local/` | samples and boot harnesses, never published, excluded in CI |

## The published name comes from the directory

`artifactName(path)` in `settings.gradle.kts` drops the first segment, joins the rest with `-` and
prefixes `surf-api-`:

```
common/core/api           ->  surf-api-core-api
foundation/spring/impl    ->  surf-api-spring-impl
platform/paper/nms/v26-3  ->  surf-api-paper-nms-v26-3
aggregate/paper           ->  surf-api-paper
```

`includeModule` calls `include(name)` and points `projectDir` at the directory, so **the Gradle
path is flat** and equals the published name: `:surf-api-paper-impl`, accessor
`projects.surfApiPaperImpl`. It fails fast on a missing `build.gradle.kts` and on a duplicate
published name. `includePair(group, domain)` includes the `api` and `impl` pair of one domain.

`infra/` is the exception. `includeInfra(repository, modules...)` keeps each imported repository's
tree and renames every segment from `surf-` to `surf-api-`, so its paths nest:
`:surf-api-redis:surf-api-redis-spring`, accessor `projects.surfApiRedis.surfApiRedisSpring`. The
published name is the last segment. `infra/database-r2dbc` is a single module included as
`:surf-api-database-r2dbc`.

## The three roles

| Plugin | Apply it to | What it does |
| --- | --- | --- |
| `surf.module-api` | a module whose types consumers compile against | `core-convention` + registers `checkApiBoundary` and wires it into `check` |
| `surf.module-impl` | a module wired in at runtime and shaded into a platform jar | `core-convention` + fails configuration unless the project name marks it as an implementation |
| `surf.aggregate` | a coordinate that only groups other modules | `core-convention` + fails if the project carries sources |

`surfModuleRole(projectName)` in `buildSrc/src/main/kotlin/SurfModuleRole.kt` classifies by name:
an `-impl` suffix, an `-internal` suffix or a `-nms-v<digit>` match mean `IMPL`; everything else is
`API`. Aggregates are declared by applying `surf.aggregate`, never derived from the name. Name a new
implementation module `...-impl`.

These projects apply bare `core-convention` and get no role check: `platform/paper/nms/api` (it
depends on `surf-api-internal`, which classifies as `IMPL`), `platform/standalone/testing`, every
`local/` module and `tooling/registry-key-generator`. The local Paper server tests apply
`paper-server-test` on top of it, which adds the `paperTest` source set, the pinned downloads and
`registerPaperServerTest`; see `references/conventions.md`. Most `infra/` modules apply the surf
Gradle plugin (`dev.slne.surf.api.gradle.core` and friends) the way a downstream project does;
`surf-nats-api` and `surf-nats-core` are Kotlin Multiplatform modules configured by
`infra/nats`'s own build script. The root build sets the group and version of every `infra/`
module. The surf Gradle plugin's `checkInternalApiUsage` runs in their `check` as in any
downstream project; `surf-redis-api`, `surf-redis-core`, `surf-redis-spring`, `surf-nats-spring`,
`surf-microservice-runtime`, `surf-microservice-database` and `surf-microservice-reliability`
allow the markers they are built against with `allowInternalApi(...)`. `InternalRedisAPI` carries `@InternalAPIMarker` directly;
`InternalNatsApi` lives in common code and gets it through the optional expect annotation
`InternalApiMarker`, whose JVM actual is a typealias to `InternalAPIMarker`.

## What `core-convention` gives every module

- `kotlin("jvm")`, `kotlin("plugin.serialization")`, `kotlin("plugin.spring")` (opens Spring
  stereotype and `@Configuration` classes) and `javaParameters = true` (parameter names for Spring)
- Shadow, with `addShadowVariantIntoJavaComponent = false`: the `-all` jar is built for in-build
  consumers and release assets but never published under the module's coordinate
- a `shadowJar` that merges `META-INF/services` with `DuplicatesStrategy.INCLUDE` for those files,
  so every bundled copy reaches the transformer, and relocates Adventure NBT and Configurate below
  `relocationPrefix`

KSP is not part of it, and surf-api has no KSP processor of its own. The 26.x NMS modules apply
`surf.nms-shared-26` next to `surf.module-impl`; it compiles the shared sources of
`platform/paper/nms/shared-26` into the module with the module's package and class prefix, minus
the files the module overrides. Detail for every convention: `references/conventions.md`.

## Shadow jars of the platform runtimes

`platform/paper/impl`, `platform/velocity/impl` and `platform/minestom/impl` bundle Spring and the
NATS and Redis integrations. Their `shadowJar` blocks call buildSrc helpers:

- `mergeSpringRegistries()` merges `META-INF/spring/*.imports` line by line,
  `spring.factories` and `spring/aot.factories` per key, and keeps the first value of a key in
  `spring.handlers` and `spring.schemas`.
- `excludeSpringBootLogging()` leaves out spring-boot's log4j `PropertySource` and logback
  `Configurator` service files and its log4j plugin cache, which would otherwise reach the
  server's logging. surf-api ships spring-boot for its conditions and failure analyzers only.
- `relocateNats()` relocates `io.nats` and BouncyCastle below `dev.slne.surf.nats.libs` and drops
  BouncyCastle's native libraries. Paper, Velocity and every shadow jar under `infra/nats` call
  it; Minestom does not.
- `shadowed(projects.surfApiRedis.surfApiRedisSpring)` requests the already relocated Redis jar.

Each of the three registers `checkContextContributors`, a `CheckShadowServiceFile` task wired into
`check`. It fails when the shadow jar's `META-INF/services/dev.slne.surf.api.spring.context.ContextContributor`
lacks the config, reflection, NATS or Redis contributor; a lost contributor otherwise shows up only as missing
beans at startup.

## ABI validation is a separate opt-in

It is **not** part of `surf.module-api`. Three mechanisms produce a tracked dump at
`<moduleDir>/api/<published-name>.api`:

- `` `api-validation` `` in the `plugins` block, next to the role: `foundation/annotations`,
  `foundation/spring/api`, the eleven `common/*/api` modules, `platform/{paper,minestom,velocity}/api`,
  and the implementation modules `platform/minestom/impl` and `platform/standalone/impl`, whose
  surface an aggregate re-exports
- `` `api-validation` `` next to the surf Gradle plugin in the infra modules with a JVM dump:
  `surf-redis-api`, `surf-redis-spring`, `surf-nats-spring`, `database-r2dbc` and the four
  `infra/microservice` modules. Those that have an internal annotation of their own exclude it in
  an additional `kotlin { abiValidation { filters { ... } } }` block
- `surf-nats-api` is Kotlin Multiplatform, which `api-validation` does not support; its own
  `abiValidation` block also writes a `.klib.api` for the JS target

`foundation/registry-keys` stays out on purpose: its surface is generated, so a dump would change
with every Minecraft version. CI runs `./gradlew checkKotlinAbi`; refresh with
`./gradlew updateKotlinAbi`. The dumps omit reified inline functions such as `surfBean<T>` and
`runMicroservice<A>`, so review changes to their signatures by hand.

### `@InternalSurfApi` and `@PublishedSurfApi`

Both live in `foundation/annotations` and are opt-ins at error level, so plugin code cannot call
either; the root build opts every surf-api module in.

- `@InternalSurfApi`: used only between surf-api's modules. The `api-validation` convention
  excludes it from the dumps, so it may change in any release.
- `@PublishedSurfApi`: internal as well, but referenced from a public `inline` function. The
  compiler copies that function's body into the calling plugin, so the declaration is part of the
  plugin's bytecode. The dumps keep it; change it only as you would public API.

`checkInlineInternalApi` (registered by `api-validation`, part of `check`) reads the Kotlin
metadata of every public `inline` function and inline accessor, follows its bytecode, including
`$default` variants and `crossinline` lambda classes, and fails when it references a declaration
marked only `@InternalSurfApi`. Fix such a failure by switching the referenced declaration to
`@PublishedSurfApi`, not by suppressing the check.

The dump filter excludes only the annotated class itself, not the classes nested in it: a
companion object or `Key` object of an internal class needs the marker of its own, or it stays in
the dump. `checkAbiDumpNesting`, registered by the root build for every module with an `api/`
directory and part of `check`, fails when a `.api` dump lists a nested class whose outer class it
leaves out (`$$serializer` classes resolve to the class they serialize). Source: `buildSrc/src/main/kotlin/CheckInlineInternalApi.kt`,
covered by `CheckInlineInternalApiTest`.

## checkApiBoundary

Registered only by `surf.module-api`. It forbids an API module from declaring a dependency on a
project that `surfModuleRole` classifies as `IMPL`, in any of `api`, `implementation`, `compileOnly`,
`compileOnlyApi` or `runtimeOnly`. Violations are collected in `afterEvaluate` and reported as
`configuration -> module`. It fails the build; `-Psurf.apiBoundary.strict=false` downgrades it to
a warning for a transitional local build.

When it fires, move the type you need into the matching `-api` module, or make the consuming
module an implementation module. Implementations are wired in at runtime, never compiled against.

## Where third-party libraries go

The surf-api runtime jar of each platform ships the libraries, so the api modules only compile
against them:

- An `*-api` module declares every third-party library - the Kotlin stdlib, kotlin-reflect,
  coroutines, serialization, Adventure, Configurate, Spring and the utility libraries - as
  `compileOnlyApi`. A consumer still compiles against it; nothing reaches a runtime classpath.
- The runtime dependency sits on the implementation module that ships it: `surf-api-spring-impl`
  (Spring Framework, spring-boot, spring-boot-autoconfigure, stdlib), `surf-api-config-impl`
  (Configurate), `surf-api-core-impl` (Kotlin libraries, reactor, Adventure NBT, glm, aedile),
  and the platform impls for what only their platform needs. `surf-api-standalone` and
  `surf-api-minestom-server` reach these through the impl module they export, so they carry real
  runtime dependencies; the plugin coordinates do not.
- fastutil is not part of this scheme: no api module depends on it. The Paper NMS version
  modules declare it `compileOnly` against the copy the server ships.
- A new `compileOnlyApi` library needs such a runtime home, or the shadow jars lose it. Compare
  `jar tf` of the paper, velocity, minestom and standalone impl shadow jars before and after.
- The catalog bundles `adventure`, `configurate`, `surf-runtime-utils`, `kotlin-coroutines`
  (`kotlinx-coroutines-core` only) and `kotlin-coroutines-reactor` (the reactor bridges and
  reactor-core, pinned for the database and Redis jars) group the libraries that travel together.

`test-convention` makes `testImplementation` extend the module's own `compileOnlyApi`, not the
`compileOnlyApi` of its project dependencies. A test of an api module that needs such a library at
runtime declares it with `testRuntimeOnly`.

## Aggregates

`aggregate/{core,paper,minestom,minestom-server,velocity,standalone}` are the coordinates
downstream projects know - `dev.slne.surf.api:surf-api-paper` and friends. Each build file holds
`id("surf.aggregate")`, its `api(projects....)` lines and a `description`. `surf-api-core` exports
`surf-api-core-api` and `surf-api-config-api`; the others export one project each. `surf.aggregate`
fails the build if any source directory is non-empty, so renaming or splitting an API module never
breaks a downstream coordinate as long as the aggregate keeps exporting the right projects.

## surf-api-bom and notPublished

`tooling/bom` is a `java-platform` that constrains every leaf subproject to one version. The
constraints come from `rootProject.subprojects` names, so the infra modules are included.
`notPublished` lists what the BOM leaves out: `surf-api-bom`,
`surf-api-registry-key-generator`, `surf-api-paper-plugin-test`, `surf-api-minestom-example`,
`surf-api-minestom-example-plugin`, the six `local/*-shared-bean-*` modules,
`surf-api-paper-server-harness` and the two `local/paper-scenario-*` modules. Add every module
that `settings.gradle.kts` includes only outside CI to that set; otherwise it becomes a constraint
in a locally built BOM but not in a CI-built one.

## Renaming a module

Until 4.0.0 ships, a renamed module publishes no relocation POM, and a consumer that names an old
coordinate fails to resolve it. Record the rename in `MIGRATION-4.0.md`. Checklist:
`references/adding-a-module.md`.

## Traps

- **No implicit Kotlin stdlib.** `gradle.properties` sets `kotlin.stdlib.default.dependency=false`.
  A module with no project dependency, and nothing else pulling the stdlib in, declares
  `compileOnlyApi(libs.kotlin.stdlib)` itself, as `foundation/registry-keys` does.
- **ABI dumps are named after the project.** Renaming a module strands its dump under the old file
  name and `checkKotlinAbi` reports the whole surface as new. Rename
  `<moduleDir>/api/<old-name>.api` in the same change.
- **In-build coordinates resolve to projects.** The root build substitutes every
  `dev.slne.surf.api:<module>` request with the project, and with its `SHADOWED` variant when the
  module publishes a `shadow` publication. Inside a module, request a shaded infra jar with
  `shadowed(projects....)`.
- **`.gitattributes` pins LF** for every text file in the working tree on every platform. `*.bat`
  and `*.cmd` are CRLF and `*.patch` is left untouched - never normalise a patch.

## Commands

Run `./gradlew` from the repository root:

```bash
./gradlew projects                                  # every Gradle path
./gradlew :surf-api-paper-impl:build
./gradlew :surf-api-core-api:checkApiBoundary
./gradlew :surf-api-paper-impl:checkContextContributors
./gradlew checkKotlinAbi                            # the ABI gate, as CI runs it
./gradlew updateKotlinAbi                           # refresh the dumps, then commit the api/ files
./gradlew build shadowJar --parallel --no-scan      # the PR build
./gradlew check --parallel --no-scan                # includes the gradle-plugin build's check
./gradlew -p buildSrc test                          # buildSrc's own tests; `check` skips them
```

`buildSrc` is built for its jar and nothing else, so its tests (`CheckShadowServiceFileTest`,
`SpringRegistriesTest`) are not part of `check`. CI runs them as a step of their own; run them
after touching `buildSrc`.

`platform/minestom/luckperms-impl` is included only once `vendor/LuckPerms` exists; create it with
`git submodule update --init --recursive` and then `./gradlew applyPatches` from `vendor/`. CI
errors out instead of skipping it. The `local/` modules and `tooling/registry-key-generator` are
excluded whenever the `CI` environment variable is true.

## References

Read on demand:

- `references/module-layout.md` - every included module with its Gradle path, the `name`
  overrides, `includeInfra`, the included builds, the aggregates and the BOM.
- `references/conventions.md` - what each `buildSrc` plugin and helper does, the role
  classification, `checkApiBoundary`, the shadow-jar helpers and `surf.aggregate` in detail.
- `references/adding-a-module.md` - the end-to-end checklist for a new, renamed or moved module.
