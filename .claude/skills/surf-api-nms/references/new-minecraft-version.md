# Adding support for a new Minecraft version

The scaffolding lives in `surf-api-generator/nms-module-generator/`, an **included build**:
`settings.gradle.kts` has `pluginManagement { includeBuild("surf-api-generator/nms-module-generator") }`.
Its own `settings.gradle.kts` names it `gradle-nms-module-generator`, and its `build.gradle.kts`
registers the plugin id `dev.slne.surf.api.generator.nms-module-generator` with
`implementationClass = "dev.slne.surf.api.generator.nms.GenerateNmsModulePlugin"`.

The repository root applies it:

```kotlin
import dev.slne.surf.api.generator.nms.NmsVersion

plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.24" apply false
    id("dev.slne.surf.api.generator.nms-module-generator")
}

nmsGenerator {
    referenceVersion = NmsVersion.V26_2
    targetVersion = NmsVersion.V26_3
}
```

Those two lines are also the record of the last generation that was run. `v26-3` was generated from
`v26-2`, which is why `glow/entity/V26_3EntityGlowingData.kt` carries a prefix that `v26-1`'s
equivalent file does not.

## The two `NmsVersion` enums

They are different types with different jobs, and both need the new entry.

| | `surf-api-generator/nms-module-generator/.../NmsVersion.kt` | `foundation/internal/.../nms/NmsVersion.kt` |
| --- | --- | --- |
| Package | `dev.slne.surf.api.generator.nms` | `dev.slne.surf.api.shared.internal.nms` |
| Used by | the Gradle task, at generation time | the running server, for matching and discovery |
| Members | `versionId`, `classPrefix`, `paperDevBundle`, derived `slug` / `modulePath` / `gradlePath` | `versionPrefix` only |
| Entry | `V26_3(versionId = "v26_3", classPrefix = "V26_3", paperDevBundle = "26.3.build.+")` | `V26_3("26.3")` |

The generator's enum derives everything else:

```kotlin
val slug get() = versionId.replace('_', '-')            // "v26-3"
val modulePath get() = "platform/paper/nms/$slug"       // the directory
val gradlePath get() = ":surf-api-paper-nms-$slug"      // the flat project path
```

The comment in `settings.gradle.kts` explains why the version list there is literal: *"NmsVersion
lives in the included build and is not on the settings classpath, so the versions stay literal
here."*

## The DSL

`NmsGeneratorExtension` (`@NmsGeneratorDsl`) exposes `referenceVersion`, `targetVersion` (both
`lateinit`, both checked by `isConfigured`) and `transformations { … }`. `TransformationScope` offers
six builders, each appending an `NmsTransformation` applied **in declaration order, after** the
automatic `versionId`/`classPrefix` replacement:

| Builder | Effect |
| --- | --- |
| `renameClass(oldFqn, newFqn)` | rewrites `import <oldFqn>` → `import <newFqn>`, and when the simple names differ, every `\b<OldSimple>\b` occurrence |
| `removeImport(fqn)` | deletes the `import <fqn>` line (both `\n` and `\r\n`) |
| `replaceCode(old, new)` | literal `String.replace` over every file |
| `replacePattern(pattern, replacement)` | `Regex(pattern)` replacement; `$1` group references work |
| `transformFile(filePattern) { content -> … }` | arbitrary transform, applied only when the file's relative path equals or ends with `filePattern` (`/` separators) |
| `excludeFile(filePattern)` | skips the file entirely; collected before the copy loop, so the file is never written |

The plugin's own KDoc gives the canonical example:

```kotlin
nmsGenerator {
    referenceVersion = NmsVersion.V26_1
    targetVersion = NmsVersion.V1_21_11

    transformations {
        renameClass("net.minecraft.resources.Identifier", "net.minecraft.resources.ResourceLocation")
        removeImport("net.minecraft.world.item.component.TypedEntityData")
        replaceCode("TypedEntityData.decodeEntity(nbt)", "CustomData.of(nbt)")
    }
}
```

## Running it

```bash
./gradlew generateNmsModule                   # interactive; prompts before overwriting
./gradlew generateNmsModule -PforceOverwrite  # non-interactive
```

`GenerateNmsModuleTask` is registered on the root project (group `generation`). Before anything
else it:

1. `check(ext.isConfigured)` — fails with *"nmsGenerator.referenceVersion and
   nmsGenerator.targetVersion must be set"*.
2. `confirmOverwriteIfNeeded` — if `<target.modulePath>/src/main/kotlin` exists and `-PforceOverwrite`
   is absent, it needs `System.console()`. **There is no console under the Gradle daemon or in CI**,
   so without the flag it throws *"Target module … already exists. Pass -PforceOverwrite to
   overwrite."* A `y`/`yes` answer proceeds; anything else aborts with *"Aborted by user."*

## Exactly what the generator writes

`NmsModuleGenerator.generate()`, in order:

1. `require(referenceSourceRoot.exists())` — the reference module's `src/main/kotlin`.
2. **`prepareTargetDirectory()` deletes the target's `src/main/kotlin` recursively** and recreates
   it. Any hand-written file that lives only in the target module is gone. Everything outside
   `src/main/kotlin` (resources, `build.gradle.kts`, `api/` dumps) is untouched.
3. `copyAndTransformSources()` — walks the reference tree for `*.kt`, skips exclusions, and for each
   file rewrites the relative path with `transformPath` (`versionId` then `classPrefix` replaced) and
   the content with `transformContent` (same two replacements, then every transformation). Returns the
   count.
4. `generateServiceFile()` — creates `<target>/src/main/resources/META-INF/services/` and writes
   `dev.slne.surf.api.paper.nms.common.NmsProvider` containing
   `dev.slne.surf.api.paper.server.nms.<versionId>.<classPrefix>NmsProvider`. **This file is not read
   at runtime** (`NmsProvider.current` uses `META-INF/surf-api/nms-providers/*.json` written by KSP
   from `@NmsProviderMarker`). It is harmless, and `v26-1`/`v1-21-11` prove nothing needs it.
5. `generateBuildFile()` — writes `build.gradle.kts` from an inline template with the target's
   `paperDevBundle` substituted, **only when the file does not already exist**; otherwise logs
   *"build.gradle.kts already exists, skipping"*.

The blanket `classPrefix` replacement is what makes `@NmsProviderMarker(NmsVersion.V26_2)` become
`@NmsProviderMarker(NmsVersion.V26_3)` and `override val version: NmsVersion = NmsVersion.V26_3` —
the enum constant names are the class prefixes. That only holds because the two enums use the same
constant names; keep it that way.

## What the task tells you afterwards

Its lifecycle log prints three next steps:

1. add the target `slug` to the NMS version list in `settings.gradle.kts`, declared as
   `module("<modulePath>")` with project path `<gradlePath>`,
2. register the target in the runtime `NmsVersion` enum,
3. review the generated sources and fix remaining compilation errors.

## What it does not tell you

- **The generator's own enum entry** has to exist *before* you can even name the target in
  `nmsGenerator { }`, so this is step 1 in practice, not a follow-up.
- **`platform/paper/impl/build.gradle.kts`** needs `runtimeOnly(projects.surfApiPaperNmsV264)`.
  Without it the module is built but never on the server's classpath, and `NmsProvider.current`
  silently falls back to another version — or fails with *"No NmsProvider implementations found"* if
  it is the only one.
- **`paper { apiVersion = "…" }`** in that same file is the Paper plugin descriptor's target version.
  Bump it when the new version becomes the version the plugin is published against.
- **The literal list in `settings.gradle.kts`** is a `listOf("v1-21-11", "v26-1", "v26-2", "v26-3")`
  — add the slug there, not a new `includeModule` line.
- **Nothing updates `tooling/bom`.** `surf-api-bom` builds its constraints from
  `rootProject.subprojects`, so a newly included module is covered automatically.

## Checklist

```
[ ] 1. generator NmsVersion entry: versionId, classPrefix, paperDevBundle
[ ] 2. root build.gradle.kts: referenceVersion / targetVersion (+ transformations)
[ ] 3. ./gradlew generateNmsModule [-PforceOverwrite]
[ ] 4. foundation/internal NmsVersion entry: V26_4("26.4")   <- declaration order matters
[ ] 5. settings.gradle.kts: add "v26-4" to the version list
[ ] 6. platform/paper/impl/build.gradle.kts: runtimeOnly(projects.surfApiPaperNmsV264)
[ ] 7. platform/paper/impl/build.gradle.kts: paper { apiVersion = "26.4" } if it is now the target
[ ] 8. ./gradlew :surf-api-paper-nms-v26-4:build   and fold fixes back into step 2
```

## When the module builds but the wrong provider loads

`NmsProvider.current` logs `Discovered %s NmsProviders: %s` at info before selecting. Read that line
first.

- **`No NmsProvider implementations found`** — no `META-INF/surf-api/nms-providers/*.json` on the
  classpath. Either KSP did not run (the module must apply `surf.module-impl`/`core-convention`, which
  adds `ksp(project(":surf-api-processor"))`), or the provider class lost its `@NmsProviderMarker`, or
  the module is missing from `runtimeOnly` in `platform/paper/impl`.
- **`No exact match for NmsProvider version …, using fallback`** — the server's
  `Bukkit.getMinecraftVersion()` did not `startsWith` any `versionPrefix`. Check the new
  `NmsVersion` entry's prefix string.
- **A wrong but plausible version is selected** — two string hazards, both in
  `NmsVersionResolver`/`NmsProvider`:
  - matching is `entries.firstOrNull { mcVersion.startsWith(it.versionPrefix) }`, so a `26.11`
    server matches `V26_1` ("26.1") when `V26_1` comes first in the enum;
  - the fallback is `maxByOrNull { it.versionPrefix }` over **strings**, so `"26.10" < "26.3"` and
    `"1.21.11"` sorts below everything `26.*`.

  Declaration order in `foundation/internal/.../NmsVersion.kt` is therefore load-bearing. Put the
  more specific prefix first.
- **`Failed to load NmsProvider implementation. Implementation must have a constructor with a single
  argument of type JavaPlugin`** — the generated provider lost its `(plugin: JavaPlugin)` primary
  constructor.
