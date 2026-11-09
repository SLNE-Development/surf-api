---
name: surf-api-build
description: Use when touching the surf-api Gradle build - adding, renaming, moving or splitting a module, writing or repairing a build.gradle.kts, choosing between surf.module-api, surf.module-impl and surf.aggregate, fixing a checkApiBoundary or "applies surf.aggregate but carries sources" failure, turning ABI validation on or off, deciding whether a module belongs in surf-api-bom, or working out a module's published coordinate and Gradle path.
---

# surf-api build and module layout

Every structural fact below comes from `settings.gradle.kts`, `gradle.properties` and
`buildSrc/src/main/kotlin/`. Read those files when in doubt; they are the only source of truth.

## Where modules live

The first path segment groups modules and carries no meaning beyond grouping:

| Directory | What it holds |
| --- | --- |
| `foundation/` | `annotations`, `internal`, `registry-keys`, `di/api`, `di/impl` |
| `common/` | `core/api`, `core/impl` - platform-independent contracts and their implementation |
| `platform/` | `paper/`, `minestom/`, `velocity/`, `standalone/`, plus the `paper/nms/` bridges |
| `aggregate/` | the six coordinates downstream projects depend on; no code |
| `tooling/` | `bom`, `gradle-plugin`, `gradle-plugin/processor` |
| `local/` | `paper-plugin-test`, `minestom-example`, `minestom-example-plugin`; never published, excluded in CI |
| `surf-api-generator/` | stays at the repository root: local-only and carries thousands of registry data files |

## The published name comes from the directory

`artifactName(path)` in `settings.gradle.kts` drops the first segment, joins the rest with `-` and
prefixes `surf-api-`:

```
common/core/api           ->  surf-api-core-api
platform/paper/nms/v26-3  ->  surf-api-paper-nms-v26-3
aggregate/paper           ->  surf-api-paper
foundation/di/impl        ->  surf-api-di-impl
```

A single-segment path keeps its directory name verbatim (`surf-api-generator`).

`includeModule` then calls `include(name)` and points `projectDir` at the directory, so **the Gradle
path is flat** and equals the published name. There is no `:platform:paper:impl`; it is
`:surf-api-paper-impl`. This also keeps typesafe accessors as `projects.surfApiCoreApi` instead of
`projects.common.core.api` - an `api` accessor sitting next to the `api()` configuration in the same
dependencies block would be a trap - and it means the published name needs no `archivesName`
override.

`includeModule` fails fast on a missing `build.gradle.kts` and on a duplicate published name.
`includePair(group, domain)` is shorthand for the `api` + `impl` pair of one domain.

## The three roles

| Plugin | Apply it to | What it does |
| --- | --- | --- |
| `surf.module-api` | a module whose types consumers compile against | `core-convention` + registers `checkApiBoundary` and wires it into `check` |
| `surf.module-impl` | a module wired in at runtime and shaded into the final jar | `core-convention` + asserts the project name actually marks it as an implementation |
| `surf.aggregate` | a coordinate that only groups other modules | `core-convention` + a `surfRuntime` configuration + fails if the project carries sources |

`surfModuleRole(projectName)` in `buildSrc/src/main/kotlin/SurfModuleRole.kt` classifies by name: an
`-impl` suffix, a `-server` substring, an `-internal` suffix, or a `-nms-v<digit>` match mean `IMPL`;
everything else is `API`. Aggregates are never derived from the name - they are declared by applying
`surf.aggregate`. Name a new implementation module `...-impl`; the other three shapes are legacy
spellings that `surf.module-impl` still accepts.

A few projects apply bare `core-convention` instead of a role and so get no role check:
`platform/paper/nms/api`, the three `local/` modules and `surf-api-generator`.
`tooling/gradle-plugin` and `tooling/gradle-plugin/processor` apply neither - they carry their own
versions.

## ABI validation is a separate opt-in

It is **not** part of `surf.module-api`. A module that wants a tracked ABI dump adds
`` `api-validation` `` to its `plugins` block on top of its role. Seven projects do, two of them
implementation modules (`platform/minestom/impl`, `platform/standalone/impl`), because an aggregate
re-exports their surface. The deliberate exclusions carry their reason in the build file:
`foundation/registry-keys` (generated surface - a dump would change with every Minecraft version and
record nothing worth reviewing) and `foundation/di/api` (contracts still being shaped).

Dumps live at `<moduleDir>/api/<published-name>.api`, e.g.
`common/core/api/api/surf-api-core-api.api`. CI runs `./gradlew checkKotlinAbi`; refresh with
`./gradlew updateKotlinAbi`.

## checkApiBoundary

Registered only by `surf.module-api`. It forbids an API module from declaring a dependency on a
project that `surfModuleRole` classifies as `IMPL`, in any of `api`, `implementation`, `compileOnly`,
`compileOnlyApi` or `runtimeOnly`. Violations are collected in `afterEvaluate` - the consuming
`dependencies` block runs after the plugin is applied - and reported as `configuration -> module`.
It fails the build; `-Psurf.apiBoundary.strict=false` downgrades it to a warning.

When it fires, do not reach for that flag. Either move the type you need into the matching `-api`
module, or make the consuming module an implementation module. Implementations are wired in at
runtime, never compiled against.

## Aggregates

`aggregate/{core,paper,minestom,minestom-server,velocity,standalone}` are the coordinates downstream
projects know - `dev.slne.surf.api:surf-api-paper` and friends. Each is a `build.gradle.kts` holding
`id("surf.aggregate")`, one `api(projects....)` line and a `description`, nothing else. They carry no
code, and `surf.aggregate` fails the build if any source directory of any source set is non-empty: an
aggregate that silently compiled code would publish classes nobody expects there. Renaming or
splitting an API module therefore never breaks a downstream coordinate, as long as the aggregate
keeps exporting the right projects.

## surf-api-bom and notPublished

`tooling/bom` is a `java-platform` that constrains every surf-api module to one version, so a
transitive path cannot pull a second version of a sibling into the graph. The constraints are built
from `rootProject.subprojects` names rather than from the other projects' state, because those
projects are not configured yet while the BOM is.

A module lands in the `notPublished` set for one of two reasons: `settings.gradle.kts` only includes
it outside CI (`surf-api-generator`, `surf-api-paper-plugin-test`, `surf-api-minestom-example`,
`surf-api-minestom-example-plugin`), or it carries a version of its own (`surf-api-gradle-plugin`,
`surf-api-processor`) - plus `surf-api-bom` itself. A new published module needs no edit here; a
local-only or independently versioned one must be added.

## Renaming a module

`publishRelocationFrom(vararg oldArtifactIds)` in `buildSrc/src/main/kotlin/relocation.kt` publishes
a POM for each old coordinate with a `<distributionManagement><relocation>` block pointing at this
module, so a stale consumer keeps resolving and gets a warning naming the new coordinate. Add it when
consumers resolve the old coordinate directly - the eight renamed `platform/minestom/*-impl` modules
do, e.g. `publishRelocationFrom("surf-api-minestom-server-npc")`. A module only ever pulled in
transitively needs none. It rejects a relocation from the module's own artifact id.

## Traps

- **No implicit Kotlin stdlib.** `gradle.properties` sets `kotlin.stdlib.default.dependency=false`.
  A module with no project dependency, and nothing else pulling the stdlib in, must declare
  `api(libs.kotlin.stdlib)` itself. `foundation/registry-keys` does, with a comment saying why;
  `foundation/annotations` gets away without it only because `api(libs.kotlin.reflect)` brings it
  along.
- **ABI dumps are named after the project.** Renaming a module strands its dump under the old file
  name and `checkKotlinAbi` then reports the whole surface as new. Rename
  `<moduleDir>/api/<old-name>.api` to the new name in the same commit.
- **`.gitattributes` pins LF** for every text file, in the repository and in the working tree on
  every platform, so a pattern-based edit behaves the same whoever runs it. `*.bat` and `*.cmd` are
  CRLF and `*.patch` is left untouched - never normalise a patch.
- **`surf-api-minestom-server` is an aggregate whose name contains `-server`**, so `surfModuleRole`
  classifies it as `IMPL`. An API module that depended on that coordinate would trip
  `checkApiBoundary`. Nothing does today.

## Commands

Gradle paths are flat, and `./gradlew` runs from the repository root:

```bash
./gradlew projects                              # every flat path
./gradlew :surf-api-paper-impl:build
./gradlew :surf-api-core-api:checkApiBoundary
./gradlew checkKotlinAbi                        # the ABI gate, as CI runs it
./gradlew updateKotlinAbi                       # refresh the dumps, then commit the api/ files
./gradlew build shadowJar --parallel --no-scan  # the PR build
./gradlew check --parallel --no-scan
```

`platform/minestom/luckperms-impl` is only included once `vendor/LuckPerms` exists; create it with
`git submodule update --init --recursive` and then `./gradlew applyPatches` from `vendor/`. CI errors
out instead of skipping it. The `local/` modules and `surf-api-generator` are excluded whenever the
`CI` environment variable is true.

## References

Read on demand, not up front:

- `references/module-layout.md` - the full directory-to-coordinate table, every `includeModule`
  call, the `name` override, the included builds, the aggregates and the BOM.
- `references/conventions.md` - what each `buildSrc` plugin applies, the role classification rules,
  `checkApiBoundary` and `surfRuntime` in detail.
- `references/adding-a-module.md` - the end-to-end checklist for a new, renamed or moved module.
