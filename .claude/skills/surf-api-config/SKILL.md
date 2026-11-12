---
name: surf-api-config
description: Use when touching surf-api configuration or environment variables - declaring a @ConfigSerializable class with a SpongeYmlConfigClass or SpongeJsonConfigClass companion, loading/saving/reloading or editing a config, choosing a config file name, adding a Configurate TypeSerializer for a type the config cannot write yet, using the constraint annotations (@Range, @NotBlank, @MinSize, @Namespace, ...) or the helper types (ConfigDuration, IntOr, BooleanOrDefault, StringOrDefault), writing a versioned ConfigMigration, reading an environment variable through env/EnvironmentVariables or a property delegate, or diagnosing LoadConfigException, SerializationConfigException, "No config found for", MissingEnvironmentVariableException and InvalidEnvironmentVariableException.
---

# surf-api configuration and environment variables

Every claim below comes from `common/config/api/src/main/kotlin/dev/slne/surf/api/config/`,
`common/config/impl/src/main/kotlin/dev/slne/surf/api/config/impl/` and
`common/config/api/src/main/kotlin/dev/slne/surf/api/config/environment/`. Read those files when in
doubt; they are the only source of truth.

Configuration is its own pair of modules, `surf-api-config-api` and `surf-api-config-impl`. You do
not name them: every published coordinate that carried these types before re-exports them, so a
build file that depends on `surf-api-core`, `surf-api-paper`, `surf-api-minestom`,
`surf-api-velocity` or `surf-api-standalone` resolves them unchanged.

The environment-variable api lives in the same module, under `config/environment/`. It shares no
type with the config classes - it reads `System.getenv` with typed delegates - but it is the same
job from a user's point of view, and a five-file module of its own would not have been a domain.
Do not confuse it with `EnvironmentConfig` / `EnvironmentAccessor` / `PropertyService` in
`surf-api-core-impl`: those answer the container's `@ConditionalOnEnvironment` and
`@ConditionalOnProperty` while a container is being built, they are a different mechanism, and they
stay where the `ContainerPlatform` implementations can reach them.

## The library underneath

Configs are Sponge **Configurate**, via the fork pinned in `gradle/libs.versions.toml`
(`configurate = "1.0.0"`, modules `dev.slne.forks.configurate:configurate-yaml`,
`configurate-jackson`, `configurate-extra-kotlin`). Package names are still
`org.spongepowered.configurate.*`, so Configurate's own annotations are what you write:
`@ConfigSerializable`, `@Comment`, `@Setting`, `@Required`, `@Matches`.

surf-api adds three things on top: a loader preset (YAML block style with comments, or Jackson JSON),
a serializer registry (`SpongeConfigSerializers`), and versioned migrations.

## Declaring a config

A config is a `@ConfigSerializable` data class whose **companion object** extends
`SpongeYmlConfigClass<C>` or `SpongeJsonConfigClass<C>`. Every property needs a default - the
defaults are what gets written to a fresh file - and `var` if you want to `edit` it.

```kotlin
@ConfigSerializable
data class ModernTestConfig(
    @Comment("This is a modern config!")
    var message: String = "Hello from Modern Config!",
    var number: Int = 42,
    var booleanOrDefault: BooleanOrDefault = BooleanOrDefault.TRUE,
) {
    companion object : SpongeYmlConfigClass<ModernTestConfig>(
        ModernTestConfig::class.java,
        plugin.dataPath,
        "modern-test-config.yml"
    )
}
```

Constructor parameters are `(configClass: Class<C>, configFolder: Path, fileName: String)`, in that
order. The file name is checked against `^[a-zA-Z0-9_-]+\.(yml|yaml)$` (YAML) or
`^[a-zA-Z0-9_-]+\.(json)$` (JSON) - see `config/common.kt`. Those are IntelliJ `@Pattern`
annotations: the IDE warns, nothing throws at runtime.

Keys in the file are **lower-case-dashed**, not camelCase: `maxPlayers` becomes `max-players`.
That is Configurate's default naming scheme from `ObjectMapper.factoryBuilder()`; override a single
key with `@Setting("my-key")`.

What the companion gives you (`SpongeConfigClass`):

| Member | Effect |
| --- | --- |
| `getConfig(): C` | the current in-memory instance |
| `edit(save = true) { ... }` | mutate the current instance as receiver, then save unless `save = false` |
| `save()` | write the current instance back to disk |
| `reloadFromFile()` | re-read, apply migrations, **replace** the instance, return it |
| `init()` | no-op that forces the lazy manager, i.e. loads the file now |
| `isInitialized()` | whether the manager (and so the file) has been touched yet |
| `manager` | the `SpongeConfigManager<C>` |
| `migration(version, …)`, `versionKey(vararg path)` | protected; call from the companion's `init` block |

The manager is `by lazy`, so **nothing is read from disk until the first access**. Call
`MyConfig.init()` in your plugin's load/enable step if you want load failures early and on the
right thread.

### Without a companion

`SurfConfigApi` (`config/SurfConfigApi.kt`) is the same thing one level down, reached through
`surfConfigApi` or the `SurfConfigApi` companion:

```kotlin
val config = surfConfigApi.createSpongeYmlConfig<MyConfig>(configFolder, "my-config.yml")
val manager = surfConfigApi.createSpongeYmlConfigManager<MyConfig>(configFolder, "my-config.yml")
val current = surfConfigApi.getSpongeConfig<MyConfig>()
```

Reified extensions exist for `createSpongeYmlConfig`, `createSpongeJsonConfig`, the two
`…ConfigManager` functions and `getSpongeConfig`. `reloadSpongeConfig` and
`getSpongeConfigManagerForConfig` have **no reified form** - pass `MyConfig::class.java`.

Instances are tracked per `Class` in `SpongeConfigTracker` (impl). Asking for a config that was
never created throws `IllegalStateException` - "No config found for …" from `getSpongeConfig`, "No
config manager found for …" from `getSpongeConfigManagerForConfig` and `reloadSpongeConfig`.
Registering the same class twice replaces the earlier manager.

## Serializers

`surfSpongeConfigSerializers` (a `SpongeConfigSerializers` bean) is asked for
its module once per config manager, at construction. Out of the box it covers Adventure
`Component` (MiniMessage), `Key`, `TextColor`, enums, `UUID`, `Regex`, `Pattern`, `URI`, `URL`,
`Path`, `File`, `LinkedList`, maps (fault-tolerant) and the surf helper types. Each platform adds
its own on top - Paper contributes `NamespacedKey`, `Material`, `ItemStack`, every Bukkit registry
type and a `ConfigurationSerializable` catch-all.

Register your own **before the first config manager is created**:

```kotlin
surfSpongeConfigSerializers.registerClassSerializer(MyType::class.java, MyTypeSerializer)
```

First registration wins in Configurate, and your registrations are flushed before surf's own, so
this is also how you override a built-in serializer.

Full list, the `TypeToken` variant, the map annotations and how to add a platform-wide serializer:
`references/serializers.md`.

## Constraints and helper types

Validation is declarative, on the field: `@field:Range(min = 0.0, max = 1.0)`,
`@field:NotBlank`, `@field:MinSize(1)`, `@field:Namespace("minecraft")` and ~20 more in
`config/constraints/`. A violated constraint throws `SerializationException` during load, which
surfaces as `SerializationConfigException`.

`config/type/` holds the three-state scalars: `ConfigDuration` (`"30s"`, `"5m"`, `"2h"`, `"1d"`),
`DurationOrDisabled`, `BooleanOrDefault`, `StringOrDefault`, `IntOr.Default` / `IntOr.Disabled`,
`DoubleOr.Default` / `DoubleOr.Disabled`. They all read as a literal value or the words `default` /
`disabled`, and resolve with `or`:

```kotlin
val retries = config.maxRetries or 3
```

Per-annotation and per-type detail: `references/constraints-and-types.md`.

## Migrations

Register them in the companion's `init` block, keyed by the version they upgrade **to**:

```kotlin
companion object : SpongeYmlConfigClass<MyPluginConfig>(
    MyPluginConfig::class.java, plugin.dataPath, "migration-example-config.yml"
) {
    init {
        migration(1, RenameServerVersionMigration)   // object : ConfigMigration
        migration(2) { node -> node.node("deprecated-field").raw(null) }
    }
}
```

They run on the initial load and on every `reloadFromFile()`, lowest version first, and the reached
version is stored under `_config_version`. A file that has no version field counts as `-1`, so
**every** registered migration runs the first time such a file is loaded. Details, `versionKey`,
`MigrationResult` and `addMigration`: `references/migrations.md`.

## Environment variables

Separate API, no schema: `env` (a process-wide `EnvironmentVariables`) in
`config/environment/`. Direct reads resolve immediately, delegates lazily on first access and take
the variable name from the property name as written:

```kotlin
object ServiceEnvironment {
    val DATABASE_HOST by env.requiredNonBlank()
    val DATABASE_PORT by env.int(default = 5432) { requireIn(1..65535) }
    val DATABASE_PASSWORD by env.requiredSecret()
    val API_BASE by env.required().named("API_BASE_URL")
}
```

Missing required value, unparsable value and failed validation each throw their own subclass of
`EnvironmentVariableException`. Converters, the validation DSL, the `sensitive` flag and the exact
missing/blank/unparsable behaviour: `references/environment-variables.md`.

`env` reads **only** `System.getenv`. The `.env` file support in
`core/server/environment/EnvironmentAccessor.kt` is used solely for `SURF_ENVIRONMENT` behind
`@ConditionalOnEnvironment` - see the `surf-api-components` skill.

## Traps

- **Lazy load.** `MyConfig.getConfig()` is the first disk touch. A missing serializer or a bad file
  surfaces there, not at plugin start, unless you call `init()`.
- **Register serializers first.** `buildSerializersModule()` is consumed when the manager is
  constructed. A serializer registered after the first config loaded is invisible to it.
- **`reloadFromFile()` swaps the instance.** Anything that cached the old `C` keeps stale data.
  Always go through `MyConfig.getConfig()` (or `manager.config`, which is `@Volatile`).
- **Dashed keys.** Migrations and raw `node.node(...)` paths use `max-players`, never `maxPlayers`.
- **`_config_version`, despite the KDoc.** `ConfigMigrationBuilder.versionKey`'s doc comment claims
  the default is `"config-version"`; `DEFAULT_VERSION_KEY` is `"_config_version"`. The constant
  wins.
- **Maps swallow bad entries.** The default `MapSerializer` logs and skips an invalid entry instead
  of failing the load. Annotate the type `@ThrowExceptions` when you want the failure.
- **Defaults are rewritten.** After load the manager re-serializes and saves, so unknown keys you
  hand-wrote stay, but every default appears in the file. `shouldCopyDefaults(true)`.
- **JSON has no comments.** `@Comment` is silently lost on `SpongeJsonConfigClass`.
- **Save failures are unchecked.** `save()` and `reloadFromFile()` wrap `ConfigurateException` in
  `UncheckedIOException`; load failures throw `LoadConfigException` or
  `SerializationConfigException`. None of them are declared in Kotlin signatures.
- **No impl on the classpath, no config.** `surfConfigApi` and `surfSpongeConfigSerializers` are
  `by surfBean<T>()` properties, so without the platform's `-impl` module the first read fails with
  `No bean provides dev.slne.surf.api.config.SurfConfigApi in the container for ...`.

Build setup for a module that uses configs: `surf-api-build`. Injecting a config into a bean:
`surf-api-components`.
