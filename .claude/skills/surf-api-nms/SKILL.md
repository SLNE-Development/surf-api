---
name: surf-api-nms
description: Use when working on surf-api's Paper NMS layer - adding support for a new Minecraft version, running the nms-module-generator, writing or changing a bridge interface under platform/paper/api/.../nms/, implementing a bridge in platform/paper/nms/shared-26, v26-3 or another version module, wiring a version module's @ConditionalOnMinecraftVersion configuration or its @NmsComponent beans, declaring a reflected field or method with a @ReflectionProxy interface, touching the packet channel injector, or fixing a missing NMS bean and version-fallback behaviour.
---

# surf-api Paper NMS bridge layer

Three layers, and the whole design rests on keeping them apart.

| Layer | Where | What it holds |
| --- | --- | --- |
| Bridge contracts | `platform/paper/api/src/main/kotlin/dev/slne/surf/api/paper/nms/` | `SurfPaperNmsCommonBridge`, `SurfPaperNmsItemBridge`, the packet bridges, the `NmsPacket` wrappers. Bukkit + Adventure types only. |
| Version-independent plumbing | `platform/paper/nms/api/` | `ConditionalOnMinecraftVersion`, `NmsComponent`, `NmsVersionResolver`, `AbstractChannelInjector`, `NmsPacketBridgeHandler`, `NmsPacketListeners`, `PacketLoreRegistry`, `GlowingLifecycleHandler`, `PlayerNmsScoreboardFactory` |
| Version implementations | `platform/paper/nms/v1-21-11`, `v26-1`, `v26-2`, `v26-3`, with the shared 26.x sources in `shared-26` | one `V26_3…Impl` per bridge, plus extensions, packet registry, glowing, reflection |

`platform/paper/nms/api/build.gradle.kts` depends on `projects.surfApiPaperApi`,
`projects.surfApiInternal`, `libs.paper.api` and, for `AbstractChannelInjector`, `netty-transport`
and `netty-common` (all compile-only; Netty comes from the server at runtime) — and on **no**
`paperDevBundle`.
That is the structural guarantee: the plumbing layer cannot reference `net.minecraft.*` even by
accident, because those classes are not on its compile classpath.

## How a bridge is declared

A bridge is an interface whose implementation is a bean of surf-api's root Spring context,
contributed by the selected version module. When surf-api's own static
code reads a bridge, the interface also gets a `companion object` with `val instance by
surfBean<T>()`: public and `@InternalSurfApi` when other surf-api modules read it, `internal` when
only `platform/paper/api` does. A class that is itself a bean injects the bridge instead. From
`platform/paper/api/.../nms/bridges/SurfPaperNmsCommonBridge.kt`:

```kotlin
@NmsUseWithCaution
interface SurfPaperNmsCommonBridge {
    fun nextEntityId(): Int
    fun getStateId(material: Material): Int

    companion object {
        @InternalSurfApi
        val instance by surfBean<SurfPaperNmsCommonBridge>()
    }
}
```

Code inside surf-api writes `SurfPaperNmsCommonBridge.instance.nextEntityId()` — no instance to
pass, no provider, no version. The root build opts every surf-api module into `@InternalSurfApi`;
a plugin does not get that opt-in and injects the bridge interface into a bean instead.
`surfBean` lives in `foundation/spring/api/.../spring/SurfContexts.kt`, is `@InternalSurfApi`, and
reads the bean from the installed root context (`SurfContexts.root`) on first access and again
whenever the platform installs another root.

Two things about that shape are load-bearing:

- **`instance` is a delegated property, and the companion does not implement the interface.** A
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

1. In surf-api's bootstrap phase `PaperSpringRuntime` reads `ServerBuildInfo.minecraftVersionId()`
   and registers `NmsVersionResolver.resolve(version)` as the singleton `surfNmsVersion`
   (`NmsVersionResolver.BEAN_NAME`) in the root bootstrap context. `NmsVersion.select` takes the
   entry matching the server, else the newest entry older than the server, and the resolver logs a
   warning for that fallback. A server older than every entry fails the bootstrap.
2. The root context scans `dev.slne.surf.api` in the load phase and finds every version module's
   `V<prefix>NmsConfiguration`. Its `@ConditionalOnMinecraftVersion(NmsVersion.V<prefix>)`
   (`OnMinecraftVersionCondition`) compares against `surfNmsVersion`, so exactly one configuration
   is registered; the classes of the others are never loaded.
3. That configuration's `@ComponentScan(useDefaultFilters = false, includeFilters =
   [Filter(NmsComponent::class)])` registers the module's `@NmsComponent` classes: the bridge
   implementations, `TickThreadGuard`, `PacketLoreRegistry`, `GlowingLifecycleHandler` and the
   command packet blocker. `@NmsComponent` is no Spring stereotype, so the root scan skips these
   classes, also the packet bridge handler, the glowing bridge and API, the channel injector and
   the lore and glowing packet listeners, which get their collaborators through constructor
   injection. Only `NmsPacketListeners` and `PlayerNmsScoreboardFactory` come from `@Bean` methods
   of the configuration.
4. Consumers inject the bridge interface; surf-api's static code reads `X.instance` (`surfBean`).
   `PacketApiLoader.onEnable()` registers `NmsPacketListeners.instance.listeners` and calls
   `AbstractChannelInjector.instance.register(plugin)`. The item bridge implements `SurfLifecycle`
   and builds the creative-tab order in `onEnable`.

Adding a bridge means an `@NmsComponent` implementation in `shared-26` (or in a 26.x module that
differs) and in `v1-21-11`; nothing else registers it.

## Build side

The 26.x modules' `build.gradle.kts` apply `id("surf.module-impl")`, `id("surf.nms-shared-26")` and
`id("io.papermc.paperweight.userdev") apply true`, and declare `api(projects.surfApiPaperNmsApi)`,
`api(projects.surfApiCoreImpl)` and `paperweight.paperDevBundle("26.3.build.+")`. `v1-21-11` is a
complete standalone module without `surf.nms-shared-26`.

`surf.nms-shared-26` (`buildSrc/src/main/kotlin/surf.nms-shared-26.gradle.kts`) compiles the
shared sources of `platform/paper/nms/shared-26/src/main/kotlin` into every 26.x module. They are
written as the version `shared-26/template.properties` names (`version=v26_3`): that module, the
template module, adds the directory as a source directory as it is, so IntelliJ analyses the shared
files there. Every older module registers `copyNmsTemplate`, a `Sync` to
`build/generated/nms-template/kotlin` that replaces `v26_3`/`V26_3` in paths and content with the
module's own (derived from the project name). A file in an older module's own `src/main/kotlin`
with the same path replaces the shared one, so a module holds only its deltas; the template module
cannot override a shared file. `shared-26` is a plain directory, not a Gradle module.

`paperweight.userdev` is declared `apply false` in the root `build.gradle.kts` at version
`2.0.0-beta.24` and applied per version module. `paperDevBundle(…)` pulls the Mojang-mapped Paper
server for that version — that is what puts `net.minecraft.*` and `org.bukkit.craftbukkit.*` on the
module's compile classpath. The bundle strings differ in shape per version
(`"1.21.11-R0.1-SNAPSHOT"`, `"26.1+"`, `"26.2.build.+"`, `"26.3.build.+"`);
`NmsVersion.paperDevBundle` records the string a newly generated module gets.

`platform/paper/impl/build.gradle.kts` pulls the version modules in with `runtimeOnly`, never
`api`/`implementation` — one line per version, e.g. `runtimeOnly(projects.surfApiPaperNmsV263)`.
Project paths are flat and equal the published artifact name: `:surf-api-paper-nms-v26-3`, not
`:platform:paper:nms:v26-3`. See the `surf-api-build` skill for the naming rule.

## Add support for a new Minecraft version

Worked example: 26.4, generated from 26.3. Steps 1–3 are the generator; 4–7 are yours.

1. **Add the enum entry** in `foundation/internal/src/main/kotlin/.../nms/NmsVersion.kt`:
   `V26_4("26.4", "26.4.build.+")`. It is the only list of versions: the runtime selects from it,
   and the generator's included build compiles the same file (`syncNmsVersion`). The package
   segment (`v26_4`), class prefix (`V26_4`), `modulePath` and `gradlePath` derive from the entry
   name.
2. **Point the root `build.gradle.kts` at the pair**:
   `nmsGenerator { referenceVersion = NmsVersion.V26_3; targetVersion = NmsVersion.V26_4 }`. Add a
   `transformations { … }` block for renames you already know about (`renameClass`, `removeImport`,
   `replaceCode`, `replacePattern`, `transformFile`, `excludeFile`).
3. **Run `./gradlew generateNmsModule`** (`-PforceOverwrite` to skip the overwrite prompt, which
   otherwise fails outright when there is no console). It **deletes** the target's
   `src/main/kotlin`, copies the reference's own `.kt` files with package segment and class prefix
   replaced in path and content, applies the transformations in declaration order, and writes
   `build.gradle.kts` (with `surf.nms-shared-26` for 26.x) only when absent. Between two 26.x
   modules only the reference's deltas are copied; the shared sources reach the target through its
   build file.
4. **Include the module** — add `"v26-4"` to the literal list in `settings.gradle.kts`.
5. **Ship it at runtime** — add `runtimeOnly(projects.surfApiPaperNmsV264)` to
   `platform/paper/impl/build.gradle.kts`.
6. **If the new version is now the server target**, bump `paper { apiVersion = "26.4" }` in the same
   file.
7. **Compile and fix the drift**: `./gradlew :surf-api-paper-nms-v26-4:build`. A shared file that
   breaks only on the new version needs an override: copy it from `build/generated/nms-template`
   into the module's `src/main/kotlin` and fix it there. Fold known renames back into step 2's
   `transformations` block.

`references/new-minecraft-version.md` has the transformation DSL and what to check when the wrong
module is selected.

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
  `platform/paper/api` and get the implementation as a bean at runtime. Never
  `implementation(projects.surfApiPaperNmsV263)`.
- **Version beans carry `@NmsComponent`, never a Spring stereotype.** The root scan covers every
  version module's package, so a `@Service` or `@Component` there registers on every server and
  fails on the wrong version (`NoUniqueBeanDefinitionException`, `NoClassDefFoundError`). Make a
  bean an `@NmsComponent` class with constructor injection, never a Kotlin `object`: Spring cannot
  scan an `object`, and other code would reach it statically instead of injecting it.
- **Versions compare as numbers, never as strings.** `NmsVersion.matches` compares the server
  version component by component, so `26.10` never matches `V26_1`, and `NmsVersion.matching` tries
  the entries newest first. `NmsVersion.select` falls back to the newest entry older than the
  server, never to a newer one. Keep comparisons on `components`; `maxByOrNull { it.versionPrefix }`
  ranks `"26.3"` above `"26.10"`.
- **A missing version bean fails surf-api's load phase.** When no configuration matches, the bridge
  beans are missing and the root context's refresh fails with `NoSuchBeanDefinitionException`.
  Usual causes: the version module is missing from `runtimeOnly` (step 5 above), or its
  configuration names another `NmsVersion`.
- **Final fields cannot be written through a proxy.** A `@ReflectedField(access = SET)` on a
  `final` field fails when the proxy is created, and with it the whole facade object. The one
  final write in the tree, `Item.components` in `v1-21-11`'s item bridge, goes through
  `unsafe.putObject` with a cached field offset.
- **Mixins are not part of this layer.** `sponge-mixin` and `mixinextras` appear only in the
  Minestom modules (`platform/minestom/impl` and `platform/minestom/npc-impl`); the Paper NMS layer
  uses the channel injector and reflection proxies instead.

## Declaring a reflected field or method

Each version module has one `reflection/V<prefix>NmsReflections.kt`: one `@ReflectionProxy`
interface per target class (from `common/reflection/api`), and an `object` that delegates to all of
them, so callers keep one static entry point:

```kotlin
object V26_3NmsReflections :
    V26_3ServerLevelReflection by reflectionProxy<V26_3ServerLevelReflection>(),
    /* one delegate per target class */ {
}

@ReflectionProxy(ServerLevel::class)
interface V26_3ServerLevelReflection {
    @ReflectedStatic
    @ReflectedField("ENTITY_COUNTER")
    fun getEntityCounter(): AtomicInteger
}
```

Callers write `V26_3NmsReflections.getEntityCounter()`. An instance member takes the receiver as
its first parameter; `@ReflectedStatic` marks a static one. `@ReflectedMethod`,
`@ReflectedField(access = GET|SET)` and `@ReflectedConstructor` cover every member; there are no
atomic access modes, so `getAndIncreaseNextChatIndex` is a get and a set in the facade object, as
non-atomic as the server's own `nextChatIndex++`. A method of another target class goes into that
class's interface, never into one with a different target. Add new reflective access to that
file, never a hand-rolled `Field.setAccessible`. `references/reflection-proxies.md` has the member
matching rules and the failure behaviour.

## Not in this skill

Spring beans and their lifecycle (`surf-api-components`), inventories, dialogs and commands each
have their own skill — list `.claude/skills/` and read the matching one instead of inferring their
rules from here. Module
layout, published coordinates, `checkApiBoundary` and the Gradle conventions are the
`surf-api-build` skill.

## References

Read on demand:

- `references/bridge-interfaces.md` — every interface in `platform/paper/nms/api/`, the beans of a
  version configuration, the channel-injector packet path, and how to add a bridge.
- `references/version-modules.md` — the layout inside a version module, what each file does, the
  version-drift examples between `v1-21-11`, `v26-1` and `v26-3`, and the build-file anatomy.
- `references/new-minecraft-version.md` — the generator in full: the `nmsGenerator` DSL, every
  transformation type, exactly what gets written, and the post-generation checklist.
- `references/reflection-proxies.md` — `@ReflectionProxy` and its member annotations, how
  members are matched and how a proxy fails.
