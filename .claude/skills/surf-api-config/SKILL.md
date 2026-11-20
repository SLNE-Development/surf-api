---
name: surf-api-config
description: Use when touching surf-api configuration or environment variables - declaring a config file with @SurfConfig and injecting ConfigHandle, or a @ConfigSerializable class with a SpongeYmlConfigClass or SpongeJsonConfigClass companion, loading/saving/reloading, editing or updating a config, choosing a config path or file name, adding a Configurate TypeSerializer through a ConfigSerializerRegistrar, using the constraint annotations (@Range, @NotBlank, @MinSize, @Namespace, ...) or the helper types (ConfigDuration, IntOr, BooleanOrDefault, StringOrDefault), writing a versioned ConfigMigration, reading an environment variable through env/EnvironmentVariables or a property delegate, or diagnosing LoadConfigException, SerializationConfigException, a "@SurfConfig path" failure, "No config found for", MissingEnvironmentVariableException and InvalidEnvironmentVariableException.
---

# surf-api configuration and environment variables

Every claim below comes from `common/config/api/src/main/kotlin/dev/slne/surf/api/config/`,
`common/config/impl/src/main/kotlin/dev/slne/surf/api/config/impl/` and
`common/config/api/src/main/kotlin/dev/slne/surf/api/config/environment/`. Read those files when in
doubt; they are the only source of truth.

Configuration is its own pair of modules, `surf-api-config-api` and `surf-api-config-impl`. You do
not name them: every published coordinate that carried these types re-exports them, so a build file
that depends on `surf-api-core`, `surf-api-paper`, `surf-api-minestom`, `surf-api-velocity` or
`surf-api-standalone` resolves them unchanged.

The environment-variable api lives in the same module, under `config/environment/`. It shares no
type with the config classes - it reads `System.getenv` with typed delegates - but it is the same
job from a user's point of view.

## The library underneath

Configs are Sponge **Configurate**, via the fork pinned in `gradle/libs.versions.toml`
(`configurate = "1.0.0"`, modules `dev.slne.forks.configurate:configurate-yaml`,
`configurate-jackson`, `configurate-extra-kotlin`). Package names are still
`org.spongepowered.configurate.*`, so Configurate's own annotations are what you write:
`@ConfigSerializable`, `@Comment`, `@Setting`, `@Required`, `@Matches`.

surf-api adds four things on top: a loader preset (YAML block style with comments, or Jackson JSON),
a serializer registry (`SpongeConfigSerializers`), versioned migrations, and the `@SurfConfig`
integration that turns a config class into an injectable `ConfigHandle`.

Every property of a config class needs a default - the defaults are what gets written to a fresh
file. Keys in the file are **lower-case-dashed**, not camelCase: `maxPlayers` becomes
`max-players`. That is Configurate's default naming scheme from `ObjectMapper.factoryBuilder()`;
override a single key with `@Setting("my-key")`.

## Declaring a config in a plugin: `@SurfConfig`

A plugin declares a config file by annotating its `@ConfigSerializable` class with `@SurfConfig`,
and its beans inject `ConfigHandle<C>`:

```kotlin
@ConfigSerializable
@SurfConfig("data/shop.yml", migrations = [ShopV1::class, ShopV2::class])
data class ShopConfig(val discount: Double = 0.0)

@Service
class ShopPricing(private val config: ConfigHandle<ShopConfig>) {
    fun price(base: Double) = base * (1 - config.value.discount)
}
```

What `@SurfConfig(path, migrations)` means:

- `path` is relative to the plugin's data folder, may contain subfolders, and must stay inside it.
  Its extension picks the format, case-insensitively: `.yml` or `.yaml` for YAML, `.json` for JSON.
- `migrations` run in order; a migration's version is its position in the array, starting at 1, so
  only ever append. Each entry is a Kotlin `object` or a class with a constructor without
  parameters. See `references/migrations.md`.
- `@SurfConfig` also works as a meta-annotation: a custom annotation that carries
  `@SurfConfig("path")` can go on the config class instead.

How it is wired (`impl/handle/SurfConfigContributor.kt`, `impl/handle/SpongeConfigHandle.kt`):

- surf-api finds `@SurfConfig` classes in the plugin's own jar, the same way the component scan
  does, and registers one singleton `ConfigHandle<C>` per class, with the class as its exact type
  argument. The config class itself is not a bean and must not be a Spring component.
- The file is read, migrated and written back with its defaults while the plugin's context starts.
  A broken file, a bad path or an unconstructible migration fails the plugin's start, not its first
  use.
- Every plugin context gets this integration, and so do the standalone runtime and microservices.

`ConfigHandle<C>` (`config/ConfigHandle.kt`):

| Member | Effect |
| --- | --- |
| `value` | the current configuration |
| `reload()` | re-read the file, apply pending migrations, install and return the new value; keeps the current value if the file holds no configuration |
| `edit { }` | change the current value in place as receiver, then save; for classes with `var` properties |
| `update { it.copy(...) }` | replace the value with the result, save, return it; for immutable classes |
| `save()` | write the current value to the file |

`reload`, `edit`, `update` and `save` run one at a time. `value` is not locked: it returns whatever
the last load, `reload` or `update` installed, and an `edit` in progress is visible to readers.
Read `config.value` where you use it instead of caching the data class; that is why beans inject the
handle and not `C`.

## Configs outside a plugin context: companion classes

Code that is not a bean - a library such as `infra/redis/.../RedisConfig.kt` or
`infra/nats/.../NatsConfig.kt` - declares the config through a **companion object** that extends
`SpongeYmlConfigClass<C>` or `SpongeJsonConfigClass<C>`. Properties must be `var` to `edit` them.

```kotlin
@ConfigSerializable
data class LibraryConfig(
    @Comment("Greeting sent on join")
    var message: String = "Hello",
    var number: Int = 42,
) {
    companion object : SpongeYmlConfigClass<LibraryConfig>(
        LibraryConfig::class.java,
        Path("config", "my-library"),
        "library.yml"
    )
}
```

Constructor parameters are `(configClass: Class<C>, configFolder: Path, fileName: String)`, in that
order. The file name is checked against `^[a-zA-Z0-9_-]+\.(yml|yaml)$` (YAML) or
`^[a-zA-Z0-9_-]+\.(json)$` (JSON) - see `config/common.kt`. Those are IntelliJ `@Pattern`
annotations: the IDE warns, nothing throws at runtime.

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
`LibraryConfig.init()` during startup if you want load failures early and on the right thread.

The companion creates its manager through surf-api's `SurfConfigApi` bean, which also tracks it per
`Class` in `SpongeConfigTracker` (impl). A bean can inject `SurfConfigApi` and use the same
operations directly; the static accessors `surfConfigApi` and `SurfConfigApi.instance` are
`@InternalSurfApi`:

```kotlin
@Service
class ReportFiles(private val configApi: SurfConfigApi) {
    fun open(folder: Path) = configApi.createSpongeYmlConfigManager<ReportConfig>(folder, "reports.yml")
}
```

Reified extensions exist for `createSpongeYmlConfig`, `createSpongeJsonConfig`, the two
`…ConfigManager` functions and `getSpongeConfig`. `reloadSpongeConfig` and
`getSpongeConfigManagerForConfig` have **no reified form** - pass `MyConfig::class.java`. Asking for
a config that was never created throws `IllegalStateException` - "No config found for …" from
`getSpongeConfig`, "No config manager found for …" from `getSpongeConfigManagerForConfig` and
`reloadSpongeConfig`. Registering the same class twice replaces the earlier manager. `@SurfConfig`
handles are not tracked there, so `getSpongeConfig` fails for them too.

## Serializers

`SpongeConfigSerializers` is a bean of surf-api's root context, one platform subclass each. It is
asked for its module once per config manager, at construction. Out of the box it covers Adventure
`Component` (MiniMessage), `Key`, `TextColor`, enums, `UUID`, `Regex`, `Pattern`, `URI`, `URL`,
`Path`, `File`, `LinkedList`, maps (fault-tolerant) and the surf helper types. Each platform adds
its own on top - Paper contributes `NamespacedKey`, `Material`, `ItemStack`, every Bukkit registry
type and a `ConfigurationSerializable` catch-all.

A plugin registers serializers for its own types with a `ConfigSerializerRegistrar` bean:

```kotlin
@Component
class ShopSerializers : ConfigSerializerRegistrar {
    override fun register(serializers: SpongeConfigSerializers) {
        serializers.registerClassSerializer<Price>(PriceSerializer)
    }
}
```

surf-api calls every registrar of a plugin context once, before the first `ConfigHandle` of that
plugin opens its file. The registry is shared by all plugins. First registration wins in
Configurate, and registered serializers are flushed before surf's own, so this is also how you
override a built-in serializer.

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
val retries = config.value.maxRetries or 3
```

Per-annotation and per-type detail: `references/constraints-and-types.md`.

## Migrations

With `@SurfConfig`, list them in `migrations`; the first entry upgrades to version 1. In a companion
class, register them in the companion's `init` block, keyed by the version they upgrade **to**:

```kotlin
companion object : SpongeYmlConfigClass<LibraryConfig>(
    LibraryConfig::class.java, Path("config", "my-library"), "library.yml"
) {
    init {
        migration(1, RenameServerVersionMigration)   // object : ConfigMigration
        migration(2) { node -> node.node("deprecated-field").raw(null) }
    }
}
```

They run on the initial load and on every reload, lowest version first, and the reached version is
stored under `_config_version`. A file that has no version field counts as `-1`, so **every**
migration runs the first time such a file is loaded. Details, `versionKey`, `MigrationResult` and
`addMigration`: `references/migrations.md`.

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
`EnvironmentVariableException`. `env` reads **only** `System.getenv`, never a `.env` file.
Converters, the validation DSL, the `sensitive` flag and the exact missing/blank/unparsable
behaviour: `references/environment-variables.md`.

## Traps

- **Registrars need a `@SurfConfig` class.** `ConfigSerializerRegistrar` beans run when the
  plugin's first handle opens. A plugin without any `@SurfConfig` class never runs its registrars,
  and a companion config loaded before that point does not see their serializers either.
- **Register serializers first.** `buildSerializersModule()` is consumed when a manager is
  constructed. A serializer registered after a config loaded is invisible to that config.
- **Reload swaps the instance.** `ConfigHandle.reload()`, `update { }` and `reloadFromFile()`
  install a new `C`; anything that cached the old one keeps stale data. Read `handle.value` or
  `MyConfig.getConfig()` (backed by the `@Volatile` `manager.config`) at the point of use.
- **`ConfigHandle.edit` always saves.** Only the companion's `edit(save = false)` skips the write.
- **Lazy load in companions.** `MyConfig.getConfig()` is the first disk touch. A missing serializer
  or a bad file surfaces there, unless you call `init()`. `@SurfConfig` files load at context start.
- **Path failures name the class.** `SpongeConfigHandle.open` rejects a path with
  "`@SurfConfig path '…' of … must be relative`", "`… leaves the data folder …`" or
  "`… must end in .yml, .yaml or .json`"; a migration that cannot be created fails with
  "`Migration … of … must be an object or have a constructor without parameters`". A plugin without
  a data folder fails with "`… declares @SurfConfig classes …, but has no data folder`".
- **Dashed keys.** Migrations and raw `node.node(...)` paths use `max-players`, never `maxPlayers`.
- **`_config_version`.** The default version key is `DEFAULT_VERSION_KEY`, `"_config_version"`, a
  single root-level key.
- **Maps swallow bad entries.** The default `MapSerializer` logs and skips an invalid entry instead
  of failing the load. Annotate the type `@ThrowExceptions` when you want the failure.
- **Defaults are rewritten.** After load the manager re-serializes and saves, so unknown keys you
  hand-wrote stay, but every default appears in the file. `shouldCopyDefaults(true)`.
- **JSON has no comments.** `@Comment` is silently lost in a JSON file.
- **Save failures are unchecked.** `save()`, `reload()`, `edit`, `update` and `reloadFromFile()`
  wrap `ConfigurateException` in `UncheckedIOException`; load failures throw `LoadConfigException`
  or `SerializationConfigException`. None of them are declared in Kotlin signatures.
- **No runtime, no config.** A companion config resolves `SurfConfigApi` from surf-api's root
  context on first access. Without a running platform runtime that read fails with "surf-api's
  root context has not been started yet …". `ConfigHandle` exists only in a context surf-api
  builds.

Build setup for a module that uses configs: `surf-api-build`. Injecting the handle and the beans
every plugin context provides: `surf-api-components`.
