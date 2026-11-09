# Adding, renaming or moving a module

## 1. Pick the directory

The directory decides the published name - see `module-layout.md`. Match the existing grouping:

- a platform-independent contract or its implementation -> `common/<domain>/{api,impl}`
- something below the platforms that both `common/` and `platform/` need -> `foundation/<name>` or
  `foundation/<domain>/{api,impl}`
- platform-specific -> `platform/<platform>/{api,impl}`, or `platform/<platform>/<feature>-impl` for
  an optional feature module (the Minestom pattern)
- a downstream coordinate -> `aggregate/<name>`
- build tooling -> `tooling/<name>`
- a sample or manual test -> `local/<name>` (then also add it to `notPublished`)

Check what the derived name will be before creating the directory. `platform/paper/foo/impl` becomes
`surf-api-paper-foo-impl`, which is `IMPL` by the `-impl` suffix. `platform/paper/foo-api` becomes
`surf-api-paper-foo-api`, which is `API`. Avoid a name containing `-server` unless you mean it to
classify as an implementation.

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
    implementation(libs.guava)
}

description = "surf-api-<name>-impl"
```

Rules that bite:

- **Declare the stdlib if nothing else brings it.** `kotlin.stdlib.default.dependency=false`. A
  module with no project dependency needs `api(libs.kotlin.stdlib)` - see
  `foundation/registry-keys/build.gradle.kts` and its comment.
- Use the version catalog (`libs.*`) and typesafe project accessors (`projects.surfApiCoreApi`).
  Typesafe accessors are on via `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")`.
- An API module exposes a dependency with `api`/`compileOnlyApi`, never `implementation`, when a
  consumer compiles against its types. `compileOnlyApi` also lands on the test classpath
  (`test-convention` makes `testImplementation` extend it).
- `description` is set to the published name in most modules; keep that habit.
- Extra Shadow relocations go in the module's own `tasks { shadowJar { ... } }` block on top of the
  two `core-convention` already does.

## 3. Register it in settings.gradle.kts

Add the call in the right section, keeping the existing order (`foundation`, `common`, `platform`,
aggregates, `tooling`, then the non-CI block):

```kotlin
includeModule("platform/paper/foo-impl")
```

or, for a matching `api`/`impl` pair:

```kotlin
includePair("common", "foo")
```

Only pass the `name` argument when the derived name is wrong - the one precedent is
`includeModule("tooling/gradle-plugin/processor", name = "surf-api-processor")`.

A `local/`-style module goes inside the `if (!ci)` block.

## 4. Decide on ABI validation

Add `` `api-validation` `` next to the role only if the module's surface is worth reviewing in a
diff and is stable enough that every PR will not churn the dump:

- yes, if consumers outside this repository compile against it, or an aggregate re-exports it
- no, if the surface is generated (`foundation/registry-keys`) or still being shaped
  (`foundation/di/api`) - both files say so in a comment, follow that practice

After adding it, run `./gradlew :<module>:updateKotlinAbi` and commit the generated
`<moduleDir>/api/<published-name>.api`.

## 5. Decide on the role

| Situation | Plugin |
| --- | --- |
| consumers compile against these types | `surf.module-api` |
| wired in at runtime, shaded into the final jar | `surf.module-impl` |
| no code, only a coordinate that groups others | `surf.aggregate` |
| build tooling or a sample with no role | bare `` `core-convention` `` |

`surf.module-impl` rejects a name that does not mark the module as an implementation. `surf.aggregate`
rejects any source file. `surf.module-api` adds `checkApiBoundary`.

## 6. Reach it from downstream

If downstream projects should see the module, add it to the matching aggregate rather than telling
them a new coordinate:

```kotlin
// aggregate/paper/build.gradle.kts
dependencies {
    api(projects.surfApiPaperApi)
    api(projects.surfApiPaperFooApi)
}
```

Published modules need no BOM edit - `tooling/bom` derives its constraints from the subproject names.
A local-only or independently versioned module must be added to `notPublished` in
`tooling/bom/build.gradle.kts`.

## 7. Verify

```bash
./gradlew projects                              # the new flat path is listed
./gradlew :surf-api-<name>:build
./gradlew :surf-api-<name>:checkApiBoundary     # api modules only
./gradlew checkKotlinAbi                        # if api-validation is applied
./gradlew check --parallel --no-scan
```

## Renaming or moving an existing module

1. Move the directory. The published name follows the path automatically; no `archivesName` anywhere.
2. Update the `includeModule`/`includePair` call.
3. Rename the ABI dump: `<moduleDir>/api/<old-name>.api` -> `<moduleDir>/api/<new-name>.api`. The
   dump is named after the project, so a rename without this strands the old file and
   `checkKotlinAbi` reports the entire surface as new.
4. Update every `projects.<accessor>` reference. The accessor is the camel-cased published name, so a
   rename changes it: `surf-api-minestom-server-npc` was `projects.surfApiMinestomServerNpc` and is
   now `projects.surfApiMinestomNpcImpl`.
5. If consumers resolve the old coordinate directly, add
   `publishRelocationFrom("surf-api-<old-name>")`. Skip it when the module is only ever pulled in
   transitively through an aggregate.
6. If the new name changes its `surfModuleRole` classification - for instance gaining or losing the
   `-impl` suffix - re-check its role plugin and the `checkApiBoundary` result in every module that
   depends on it.
7. If it was in `notPublished`, rename it there too.

Keep the change LF-only; `.gitattributes` pins `* text=auto eol=lf` so a pattern-based edit behaves
the same for everyone, and `*.patch` is excluded from normalisation on purpose.
