# Adding support for a new Minecraft version

The scaffolding lives in `tooling/nms-module-generator/`, an **included build**:
`settings.gradle.kts` has `pluginManagement { includeBuild("tooling/nms-module-generator") }`.
Its own `settings.gradle.kts` names it `gradle-nms-module-generator`, and its `build.gradle.kts`
registers the plugin id `dev.slne.surf.api.generator.nms-module-generator` with
`implementationClass = "dev.slne.surf.api.generator.nms.GenerateNmsModulePlugin"`.

The repository root applies it:

```kotlin
import dev.slne.surf.api.shared.internal.nms.NmsVersion

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
`v26-2`.

## The one `NmsVersion` enum

`foundation/internal/src/main/kotlin/dev/slne/surf/api/shared/internal/nms/NmsVersion.kt` is the
only list of versions. The runtime selects from it, and the generator's build copies the file into
its own sources (`syncNmsVersion` in `nms-module-generator/build.gradle.kts`), since an included
build cannot depend on `:surf-api-internal`. Keep the file free of dependencies beyond the Kotlin
standard library.

An entry carries `versionPrefix` and `paperDevBundle`: `V26_3("26.3", "26.3.build.+")`. The
generator derives the rest in `NmsVersionModules.kt`:

```kotlin
val NmsVersion.versionId get() = name.lowercase()               // "v26_3"
val NmsVersion.classPrefix get() = name                         // "V26_3"
val NmsVersion.slug get() = versionId.replace('_', '-')         // "v26-3"
val NmsVersion.modulePath get() = "platform/paper/nms/$slug"    // the directory
val NmsVersion.gradlePath get() = ":surf-api-paper-nms-$slug"   // the flat project path
```

The version list in `settings.gradle.kts` stays literal, because the generator build is not on the
settings classpath.

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
3. `copyAndTransformSources()` — walks the reference's own `*.kt` files, skips exclusions, replaces
   the reference's `versionId` and `classPrefix` with the target's in path and content, then applies
   every transformation. For a 26.x reference those files are only its deltas to `shared-26`; a
   26.x target gets the shared sources through `surf.nms-shared-26`. Only when the reference is
   26.x and the target is not are the shared files the reference does not override copied as well,
   with the version in `shared-26/template.properties` replaced. Returns the count.
4. `generateBuildFile()` — writes `build.gradle.kts` from an inline template with the target's
   `paperDevBundle` substituted, **only when the file does not already exist**; otherwise logs
   *"build.gradle.kts already exists, skipping"*. The template applies `surf.module-impl`,
   `surf.nms-shared-26` for a 26.x target, and paperweight, and declares
   `compileOnly(libs.fastutil)`, like the existing modules.

A shared file that needs a change for the target only is not copied; override it in the target
module by copying it from `build/generated/nms-template/kotlin` after the first build.

The shared sources stay written as the template module (`shared-26/template.properties`). A new
26.x module compiles copies of them like the other older modules. To make the newest module the
template module, so that IntelliJ analyses the shared files against the newest dev bundle, rename
the package segment and class prefix in `shared-26` to the new version, set `version` in
`template.properties`, and move any override the new module holds into `shared-26` while the
previous template module gets an override for each file whose shared version no longer fits it.

## What the task tells you afterwards

Its lifecycle log prints three next steps:

1. add the target `slug` to the NMS version list in `settings.gradle.kts`, declared as
   `module("<modulePath>")` with project path `<gradlePath>`,
2. add `runtimeOnly(project("<gradlePath>"))` to `platform/paper/impl/build.gradle.kts`,
3. review the generated sources and fix remaining compilation errors.

## What it does not tell you

- **The `NmsVersion` entry** has to exist *before* you can even name the target in
  `nmsGenerator { }`, so this is step 1 in practice, not a follow-up.
- **Without the `runtimeOnly` line** the module is built but never on the server's classpath, and
  no configuration matches the selected version: surf-api's load phase fails with
  `NoSuchBeanDefinitionException` for the first bridge.
- **`paper { apiVersion = "…" }`** in that same file is the Paper plugin descriptor's target version.
  Bump it when the new version becomes the version the plugin is published against.
- **The literal list in `settings.gradle.kts`** is a `listOf("v1-21-11", "v26-1", "v26-2", "v26-3")`
  — add the slug there, not a new `includeModule` line.
- **Nothing updates `tooling/bom`.** `surf-api-bom` builds its constraints from
  `rootProject.subprojects`, so a newly included module is covered automatically.
- **The registry keys follow `mcVersion`, not the NMS modules.** After bumping `mcVersion` in
  `gradle.properties`, run `./gradlew :surf-api-registry-key-generator:generateRegistryKeys`. It
  downloads the server jar, runs the vanilla data generator and rewrites the key objects in
  `foundation/registry-keys`; commit the result.

## Checklist

```
[ ] 1. foundation/internal NmsVersion entry: V26_4("26.4", "26.4.build.+")
[ ] 2. root build.gradle.kts: referenceVersion / targetVersion (+ transformations)
[ ] 3. ./gradlew generateNmsModule [-PforceOverwrite]
[ ] 4. settings.gradle.kts: add "v26-4" to the version list
[ ] 5. platform/paper/impl/build.gradle.kts: runtimeOnly(projects.surfApiPaperNmsV264)
[ ] 6. platform/paper/impl/build.gradle.kts: paper { apiVersion = "26.4" } if it is now the target
[ ] 7. ./gradlew :surf-api-paper-nms-v26-4:build   and fold fixes back into step 2
```

## When the module builds but the wrong version loads

- **`No NMS bindings for Minecraft …, using the newest older ones`** (from `NmsVersionResolver`) —
  no `NmsVersion` entry matched the server's `ServerBuildInfo.minecraftVersionId()`. Check the new
  entry's `versionPrefix`. `NmsVersion.matches` compares version components as numbers, and the
  fallback takes the newest older version by number (`NmsVersion.NEWEST_FIRST`), so the enum's
  declaration order does not matter.
- **`surf-api has no NMS bindings for Minecraft …`** — the server is older than every entry; the
  bootstrap fails.
- **`NoSuchBeanDefinitionException` for a bridge** — the selected entry has no configuration on the
  classpath: the module is missing from `runtimeOnly`, or its `@ConditionalOnMinecraftVersion` names
  another entry.
