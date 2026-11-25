# Adding, renaming or moving a module

This checklist covers modules included with `includeModule`. A module inside an imported `infra/`
repository is added to that repository's `includeInfra(...)` call and applies the surf Gradle plugin
instead of a buildSrc role; see `module-layout.md`.

## 1. Pick the directory

The directory decides the published name - see `module-layout.md`. Match the existing grouping:

- a platform-independent contract or its implementation -> `common/<domain>/{api,impl}`; a domain
  whose implementation is wired in elsewhere has only `common/<domain>/api`
- something below the platforms that both `common/` and `platform/` need -> `foundation/<name>` or
  `foundation/<domain>/{api,impl}`
- platform-specific -> `platform/<platform>/{api,impl}`, or `platform/<platform>/<feature>-impl` for
  an optional feature module (the Minestom pattern)
- a downstream coordinate -> `aggregate/<name>`
- build tooling -> `tooling/<name>`
- a sample, manual test or boot harness -> `local/<name>`

Check the derived name before creating the directory. `platform/paper/foo/impl` becomes
`surf-api-paper-foo-impl`, which is `IMPL` by the `-impl` suffix. `platform/paper/foo-api` becomes
`surf-api-paper-foo-api`, which is `API`.

## 2. Write the build file

An API module:

```kotlin
plugins {
    id("surf.module-api")
}

dependencies {
    api(projects.surfApiCoreApi)
    compileOnlyApi(libs.adventure.api)
}

description = "surf-api-<name>"
```

An implementation module:

```kotlin
plugins {
    id("surf.module-impl")
}

dependencies {
    api(projects.surfApiFooApi)
    implementation(projects.surfApiSpringImpl)
}

description = "surf-api-<name>-impl"
```

Every module directory has a `README.md`: a heading with the published name, one sentence on what
the module does, a short list of what it contains and, for an api-only domain, where its
implementation lives. Keep purpose and history out of `build.gradle.kts` and `settings.gradle.kts`;
a build-script comment states only a non-obvious build constraint.

Rules that bite:

- **Declare the stdlib if nothing else brings it.** `kotlin.stdlib.default.dependency=false`. A
  module with no project dependency needs `compileOnlyApi(libs.kotlin.stdlib)` - see
  `foundation/registry-keys/build.gradle.kts`.
- **Third-party libraries are `compileOnlyApi` in an API module.** The platform runtime ships them;
  the runtime dependency belongs on the implementation module that bundles the library (see
  "Where third-party libraries go" in `SKILL.md`).
- Use the version catalog (`libs.*`) and typesafe project accessors (`projects.surfApiCoreApi`).
  Typesafe accessors are on via `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")`.
- An API module exposes a project dependency with `api` and a third-party library with
  `compileOnlyApi`, never `implementation`, when a consumer compiles against its types.
  `compileOnlyApi` also lands on the test classpath (`test-convention` makes `testImplementation`
  extend it).
- A module that declares beans or Spring contracts depends on `projects.surfApiSpringApi`; one that
  contributes to every plugin context through a `ContextContributor` depends on
  `projects.surfApiSpringImpl`.
- A 26.x NMS version module adds `id("surf.nms-shared-26")` next to its role.
- Set `description` to the published name, as most modules do.
- Extra Shadow relocations go in the module's own `tasks { shadowJar { ... } }` block on top of the
  two `core-convention` applies.
- A module that a platform impl bundles and that registers a `ContextContributor` in
  `META-INF/services` must reach that jar's merged service file. Add its provider to the
  `checkContextContributors` list of `platform/paper/impl`, `platform/velocity/impl` and
  `platform/minestom/impl`.

## 3. Register it in settings.gradle.kts

Add the call in the right section, keeping the existing order (`foundation`, `common`, `platform`,
aggregates, `tooling`, `infra`, then the non-CI block):

```kotlin
includeModule("platform/paper/foo-impl")
```

or, for a matching `api`/`impl` pair:

```kotlin
includePair("common", "foo")
```

Pass the `name` argument only when the derived name is wrong; see `module-layout.md` for the
precedent.

A `local/`-style module goes inside the `if (!ci)` block.

## 4. Decide on ABI validation

Add `` `api-validation` `` next to the role only if the module's surface is worth reviewing in a
diff:

- yes, if consumers outside this repository compile against it, or an aggregate re-exports it
- no, if the surface is generated, like `foundation/registry-keys`; say so in the module's
  `README.md`, as that module does

After adding it, run `./gradlew :<module>:updateKotlinAbi` and commit the generated
`<moduleDir>/api/<published-name>.api`.

## 5. Decide on the role

| Situation | Plugin |
| --- | --- |
| consumers compile against these types | `surf.module-api` |
| wired in at runtime, shaded into a platform jar | `surf.module-impl` |
| no code, only a coordinate that groups others | `surf.aggregate` |
| a sample, harness or test-support module with no role | bare `` `core-convention` `` |

`surf.module-impl` rejects a name that does not mark the module as an implementation.
`surf.aggregate` rejects any source file. `surf.module-api` adds `checkApiBoundary`.

## 6. Reach it from downstream

If downstream projects should see the module, export it from an API module the matching aggregate
already exports, or add it to the aggregate, rather than telling them a new coordinate:

```kotlin
// aggregate/paper/build.gradle.kts
dependencies {
    api(projects.surfApiPaperApi)
    api(projects.surfApiPaperFooApi)
}
```

`common/core/api` re-exports most api-only common domains this way, so `surf-api-core` keeps
resolving them.

Published modules need no BOM edit - `tooling/bom` derives its constraints from the subproject
names. A module included only outside CI, or one with a version of its own, belongs in
`notPublished` in `tooling/bom/build.gradle.kts`.

## 7. Verify

```bash
./gradlew projects                              # the new path is listed
./gradlew :surf-api-<name>:build
./gradlew :surf-api-<name>:checkApiBoundary     # api modules only
./gradlew checkKotlinAbi                        # if api-validation is applied
./gradlew check --parallel --no-scan
```

## Renaming or moving an existing module

1. Move the directory. The published name follows the path; no `archivesName` anywhere.
2. Update the `includeModule`/`includePair` call.
3. Rename the ABI dump: `<moduleDir>/api/<old-name>.api` -> `<moduleDir>/api/<new-name>.api`. The
   dump is named after the project, so a rename without this strands the old file and
   `checkKotlinAbi` reports the entire surface as new.
4. Update every `projects.<accessor>` reference. The accessor is the camel-cased published name,
   so a rename changes it: `surf-api-minestom-server-npc` was `projects.surfApiMinestomServerNpc`
   and is `projects.surfApiMinestomNpcImpl`.
5. If the new name changes its `surfModuleRole` classification - gaining or losing the `-impl`
   suffix - re-check its role plugin and the `checkApiBoundary` result in every module that depends
   on it.
6. If it was in `notPublished`, rename it there too.
7. If the module's classes are named in a build file, such as a `checkContextContributors`
   provider list, update those names when the package moves.

Keep the change LF-only; `.gitattributes` pins `* text=auto eol=lf`, and `*.patch` is excluded
from normalisation on purpose.
