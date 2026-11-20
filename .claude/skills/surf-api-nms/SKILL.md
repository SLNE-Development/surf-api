---
name: surf-api-nms
description: Use when working on surf-api's Paper NMS layer - adding support for a new Minecraft version, running the nms-module-generator, writing or changing a bridge interface under platform/paper/api/.../nms/, implementing a bridge in platform/paper/nms/v26-3 or another version module, wiring an NmsProvider or the @NmsProviderMarker discovery, declaring a remapped field or method with @GenerateReflection, touching the packet channel injector, or fixing "No NmsProvider implementations found" and version-fallback behaviour.
---

# surf-api Paper NMS bridge layer

Three layers, and the whole design rests on keeping them apart.

| Layer | Where | What it holds |
| --- | --- | --- |
| Bridge contracts | `platform/paper/api/src/main/kotlin/dev/slne/surf/api/paper/nms/` | `SurfPaperNmsCommonBridge`, `SurfPaperNmsItemBridge`, the packet bridges, the `NmsPacket` wrappers. Bukkit + Adventure types only. |
| Version-independent plumbing | `platform/paper/nms/api/` | `NmsProvider`, `NmsVersionResolver`, `AbstractChannelInjector`, `NmsPacketBridgeHandler`, `PacketLoreRegistry`, `GlowingLifecycleHandler` |
| Version implementations | `platform/paper/nms/v1-21-11`, `v26-1`, `v26-2`, `v26-3` | one `V26_3…Impl` per bridge, plus extensions, packet registry, glowing, reflection |

`platform/paper/nms/api/build.gradle.kts` depends on `projects.surfApiPaperApi`,
`projects.surfApiInternal`, `libs.paper.api` and, for `AbstractChannelInjector`, `netty-transport`
and `netty-common` (all compile-only; Netty comes from the server at runtime) — and on **no**
`paperDevBundle`.
That is the structural guarantee: the plumbing layer cannot reference `net.minecraft.*` even by
accident, because those classes are not on its compile classpath.

## How a bridge is declared

A bridge is an interface whose implementation is a bean of surf-api's root Spring context. Most
bridges are only injected and are the interface plus its proxy bean. When surf-api's own static
code reads a bridge, the interface also gets a `companion object` whose `INSTANCE` reads a private
file-level `by surfBean` property: public and `@InternalSurfApi` when other surf-api modules read
it, `internal` when only `platform/paper/api` does. From
`platform/paper/api/.../nms/bridges/SurfPaperNmsCommonBridge.kt`:

```kotlin
@file:OptIn(InternalSurfApi::class)

@NmsUseWithCaution
interface SurfPaperNmsCommonBridge {
    fun nextEntityId(): Int
    fun getStateId(material: Material): Int

    companion object {
        @InternalSurfApi
        val INSTANCE get() = bridge
    }
}

@OptIn(NmsUseWithCaution::class)
private val bridge by surfBean<SurfPaperNmsCommonBridge>()
```

Code inside surf-api writes `SurfPaperNmsCommonBridge.INSTANCE.nextEntityId()` — no instance to
pass, no provider, no version. The root build opts every surf-api module into `@InternalSurfApi`;
a plugin does not get that opt-in and injects the bridge interface into a bean instead.
`surfBean` lives in `foundation/spring/api/.../spring/SurfContexts.kt`, is `@InternalSurfApi`
(hence the file-level `@OptIn`), and reads the bean from the installed root context
(`SurfContexts.root`) on first access.

Two things about that shape are load-bearing:

- **`INSTANCE` is a getter, and the companion does not implement the interface.** A
  `companion object : Foo by bridge` would read the bean while the companion's initialiser runs,
  which is during class initialisation — possibly before surf-api installed its root context, or
  while that context is still creating the very bean.
- **`by surfBean<T>()`, never an eager `= ...`.** Eager initialisation resolves when the class is
  loaded, so whichever class happens to load first decides whether the root context was ready.

## `@NmsUseWithCaution`

`platform/paper/api/.../nms/nms-opt-in.kt` — a `@RequiresOptIn` at `Level.WARNING`:
*"This is delicate API and you should expect it to break at any time or require changes"*. Put it
on every bridge interface and every implementation; consumers either annotate their own class or
`@OptIn(NmsUseWithCaution::class)`. It is a warning, not an error, so nothing breaks if a caller
ignores it — it exists to mark the blast radius of a version bump. `PacketOperation` is
deliberately **not** annotated, and
`PacketOperation.start()` carries `@OptIn(NmsUseWithCaution::class) // not really nms here`.

## Runtime selection, end to end

1. A consumer injects `SurfPaperNmsCommonBridge`, or surf-api's own code reads
   `SurfPaperNmsCommonBridge.INSTANCE` → `surfBean` → `SurfContexts.root`.
2. The root context answers with `SurfPaperNmsCommonBridgeProxy` from
   `platform/paper/impl/.../proxy/`, an ordinary Spring `@Service` bean
   (`org.springframework.stereotype.Service`, found by the root context's component scan of
   `dev.slne.surf.api`) declared as
   `class SurfPaperNmsCommonBridgeProxy : SurfPaperNmsCommonBridge by NmsProvider.current.createCommonBridge()`.
   There is one such proxy per bridge, all in that one package. The proxies are singletons of the
   root context, so they are created when surf-api's load phase builds it.
3. `NmsProvider.current` (in `platform/paper/nms/api/.../common/NmsProvider.kt`) resolves the
   server version through `NmsVersionResolver` (`NmsVersion.matching(Bukkit.getMinecraftVersion())`),
   then scans the classpath for
   `META-INF/surf-api/nms-providers/*.json`, decodes `List<NmsProviderMeta>`, picks the entry whose
   `version` matches, and reflectively instantiates it through a `(JavaPlugin)` constructor.
4. Those JSON files are generated by KSP. `V26_3NmsProvider` carries
   `@NmsProviderMarker(NmsVersion.V26_3)`; `NmsProvidersCollector` in
   `tooling/gradle-plugin/processor/.../nms/` collects every annotated class and writes
   `META-INF/surf-api/nms-providers/<module>.json`. The processor runs only in modules that apply
   the `surf.ksp-processor` convention.
5. `PacketApiLoader.onEnable()` in `platform/paper/impl` calls `provider.initialize()`, registers
   `provider.createPacketListeners()` and `AbstractChannelInjector.instance.register()`;
   `onDisable()` unwinds it and calls `provider.shutdown()`.

`NmsProvider` declares a `create…`/`get…` factory per bridge plus `version`, `plugin`,
`initialize()` and `shutdown()`. Adding a bridge means adding a factory there and implementing it in
**every** version module — the interface has no defaults.

## Build side

Every version module's `build.gradle.kts` is the same file with one string changed:
`id("surf.module-impl")` + `id("surf.ksp-processor")` +
`id("io.papermc.paperweight.userdev") apply true`, `api(projects.surfApiPaperNmsApi)`,
`api(projects.surfApiCoreImpl)`, and `paperweight.paperDevBundle("26.3.build.+")`.

`surf.ksp-processor` (`buildSrc/src/main/kotlin/surf.ksp-processor.gradle.kts`) applies KSP and adds
`ksp(project(":surf-api-processor"))`. Every module that declares `@NmsProviderMarker` or
`@GenerateReflection` must apply it, and only the version modules do. Without it the module still
compiles, but `NmsProvider` and `generatedReflectionAccessor` find nothing at runtime.

`paperweight.userdev` is declared `apply false` in the root `build.gradle.kts` at version
`2.0.0-beta.24` and applied per version module. `paperDevBundle(…)` pulls the Mojang-mapped Paper
server for that version — that is what puts `net.minecraft.*` and `org.bukkit.craftbukkit.*` on the
module's compile classpath. The bundle strings differ in shape per version
(`"1.21.11-R0.1-SNAPSHOT"`, `"26.1+"`, `"26.2.build.+"`, `"26.3.build.+"`); `NmsVersion.paperDevBundle`
in the generator records the string a newly generated module gets.

`platform/paper/impl/build.gradle.kts` pulls the version modules in with `runtimeOnly`, never
`api`/`implementation` — one line per version, e.g. `runtimeOnly(projects.surfApiPaperNmsV263)`.
Project paths are flat and equal the published artifact name: `:surf-api-paper-nms-v26-3`, not
`:platform:paper:nms:v26-3`. See the `surf-api-build` skill for the naming rule.

## Add support for a new Minecraft version

Worked example: 26.4, generated from 26.3. Steps 1–3 are the generator; 4–8 are yours.

1. **Add the enum entry to the generator**, in
   `surf-api-generator/nms-module-generator/src/main/kotlin/.../NmsVersion.kt`:
   `V26_4(versionId = "v26_4", classPrefix = "V26_4", paperDevBundle = "26.4.build.+")`.
   `slug`, `modulePath` (`platform/paper/nms/v26-4`) and `gradlePath`
   (`:surf-api-paper-nms-v26-4`) are derived — do not hand-write them.
2. **Point the root `build.gradle.kts` at the pair**:
   `nmsGenerator { referenceVersion = NmsVersion.V26_3; targetVersion = NmsVersion.V26_4 }`. Add a
   `transformations { … }` block for renames you already know about (`renameClass`, `removeImport`,
   `replaceCode`, `replacePattern`, `transformFile`, `excludeFile`).
3. **Run `./gradlew generateNmsModule`** (`-PforceOverwrite` to skip the overwrite prompt, which
   otherwise fails outright when there is no console). It **deletes** the target's
   `src/main/kotlin`, copies every `.kt` from the reference with `versionId` and `classPrefix`
   replaced in both path and content, applies the transformations in declaration order, and writes
   `build.gradle.kts` only when absent.
4. **Register the runtime enum** — `foundation/internal/src/main/kotlin/.../nms/NmsVersion.kt`:
   `V26_4("26.4")`. This is a *different* enum from the generator's; the generator's lives in an
   included build and is not on the settings classpath. Without this entry the generated
   `@NmsProviderMarker(NmsVersion.V26_4)` does not compile. The new module's `build.gradle.kts`
   from the generator already applies `surf.ksp-processor`.
5. **Include the module** — add `"v26-4"` to the literal list in `settings.gradle.kts`
   (`listOf("v1-21-11", "v26-1", "v26-2", "v26-3").forEach { includeModule("platform/paper/nms/$it") }`).
6. **Ship it at runtime** — add `runtimeOnly(projects.surfApiPaperNmsV264)` to
   `platform/paper/impl/build.gradle.kts`.
7. **If the new version is now the server target**, bump `paper { apiVersion = "26.4" }` in the same
   file.
8. **Compile and fix the drift**: `./gradlew :surf-api-paper-nms-v26-4:build`. Every remaining error
   is a genuine Mojang-mapping change; fold the fix back into step 2's `transformations` block so the
   next generation starts cleaner.

The task's own "Next steps" log only mentions steps 5, 4 and 8 — it says nothing about the generator
enum, `runtimeOnly` or `apiVersion`. `references/new-minecraft-version.md` has the detail, the
transformation DSL and what to check when the new module loads but the wrong one is selected.

## Traps

- **A bridge interface must stay version-independent.** One `net.minecraft.*` or
  `org.bukkit.craftbukkit.*` type in a signature and the contract can only be implemented by one
  version — the pattern collapses. When a bridge has to carry NMS-shaped data, mirror it:
  `bridges/data/chat/PlayerChatMessageMirror.kt` is a plain `@Serializable` data class standing in
  for `PlayerChatMessage`, and `SurfPaperNmsPlayerChatPackets` takes Adventure's
  `SignedMessage.Signature` and `Component` rather than NMS chat types. `platform/paper/api` has no
  `paperDevBundle`, so the compiler enforces this.
- **A version module is an implementation, never a compile dependency.** `surfModuleRole` in
  `buildSrc/src/main/kotlin/SurfModuleRole.kt` classifies `-nms-v<digit>` as `IMPL`, so
  `checkApiBoundary` rejects any API module that depends on it. Consumers depend on
  `platform/paper/api` and get the implementation through the proxy at runtime. Never
  `implementation(projects.surfApiPaperNmsV263)`.
- **Providers are found through JSON, not `META-INF/services`.** `NmsProvider.current` reads
  `META-INF/surf-api/nms-providers/*.json`, never a services file, and no version module carries one.
  Do not add a provider by hand-writing a services entry — annotate with `@NmsProviderMarker` and let
  KSP write the JSON.
- **Versions compare as numbers, never as strings.** `NmsVersion.matches` compares the server
  version component by component, so `26.10` never matches `V26_1`, and `NmsVersion.matching` tries
  the entries newest first. Without a match, `NmsVersionResolver` falls back to `NmsVersion.NEWEST`
  and logs a warning, and `NmsProvider.current` picks the newest provider it found through
  `NmsVersion.NEWEST_FIRST`. Keep comparisons on `components`; `maxByOrNull { it.versionPrefix }`
  ranks `"26.3"` above `"26.10"`.
- **A missing provider fails surf-api's load phase.** `NmsProvider.current` is a `by lazy`, but the
  bridge proxies are singletons of surf-api's root context, which `PaperMain.onLoad` builds, so
  `No NmsProvider implementations found` / `Failed to load NmsProvider` fails that context's
  refresh. Usual causes: the version module does not apply `surf.ksp-processor`, or it is missing
  from `runtimeOnly` (step 6 above).
- **`Reflection.initialize()` is empty in the 26.x modules.** `V26_1Reflection`,
  `V26_2Reflection` and `V26_3Reflection` declare `fun initialize() = Unit`, which exists so
  `V<prefix>NmsProvider.initialize()` has something to call; only `V1_21_11Reflection` builds a
  `reflection-remapper` proxy factory. Paper ships Mojang-mapped, so a remapper has nothing to
  translate. New reflective access goes through `@GenerateReflection`.
- **Mixins are not part of this layer.** `sponge-mixin` and `mixinextras` appear only in the
  Minestom modules (`platform/minestom/impl` and `platform/minestom/npc-impl`); the Paper NMS layer
  uses the channel injector and generated reflection instead.

## Declaring a remapped field or method

Each version module has one `reflection/V<prefix>NmsReflections.kt` — a `@GenerateReflection`
interface (from `foundation/annotations/.../reflection/`) whose companion delegates to the
KSP-generated implementation:

```kotlin
@GenerateReflection
interface V26_3NmsReflections {
    @ReflectedField(name = "ENTITY_COUNTER", target = ServerLevel::class, isStatic = true)
    fun getEntityCounter(): AtomicInteger

    companion object : V26_3NmsReflections by generatedReflectionAccessor()
}
```

Callers write `V26_3NmsReflections.getEntityCounter()`. For an instance member the first parameter is
the receiver; a static member needs an explicit `target` (or `targetName`) because there is nothing
to infer from. `@ReflectedMethod`, `@ReflectedConstructor` and `@ReflectedVarHandle` cover the rest.
Add new reflective access to that file, never a hand-rolled `Field.setAccessible`.
`references/generated-reflection.md` has the target-resolution order, the invocation modes and the
constant-argument annotations.

## Not in this skill

Spring beans and their lifecycle (`surf-api-components`), inventories, dialogs and commands each
have their own skill — list `.claude/skills/` and read the matching one instead of inferring their
rules from here. Module
layout, published coordinates, `checkApiBoundary` and the Gradle conventions are the
`surf-api-build` skill.

## References

Read on demand:

- `references/bridge-interfaces.md` — every interface in `platform/paper/nms/api/`, the full
  `NmsProvider` factory list, the channel-injector packet path, and how to add a bridge.
- `references/version-modules.md` — the layout inside a version module, what each file does, the
  version-drift examples between `v1-21-11`, `v26-1` and `v26-3`, and the build-file anatomy.
- `references/new-minecraft-version.md` — the generator in full: the `nmsGenerator` DSL, every
  transformation type, exactly what gets written, and the post-generation checklist.
- `references/generated-reflection.md` — `@GenerateReflection` and friends, plus the legacy
  `reflection-remapper` proxy still used by `v1-21-11`.
