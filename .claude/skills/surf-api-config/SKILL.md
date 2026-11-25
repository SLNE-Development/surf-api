---
name: surf-api-config
description: Use when touching surf-api configuration or environment variables - declaring a config file with @SurfConfig and injecting ConfigHandle, reading one statically with configHandle or by surfConfig, opening any file with ConfigHandles.open, loading/saving/reloading or validating a reload, editing or updating a config, choosing a config path or file name, adding a Configurate TypeSerializer through a ConfigSerializerRegistrar, using the constraint annotations (@Range, @NotBlank, @MinSize, @Namespace, ...) or the helper types (ConfigDuration, IntOr, BooleanOrDefault, StringOrDefault), writing a versioned ConfigMigration or a ConfigMigrations bean, reading an environment variable through env/EnvironmentVariables or a property delegate, or diagnosing LoadConfigException, SerializationConfigException, a "@SurfConfig path" failure, "No running context holds the config handle", MissingEnvironmentVariableException and InvalidEnvironmentVariableException.
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
  parameters. A `ConfigMigrations<C>` bean replaces the array with explicit versions and lambdas;
  both together fail the start. See `references/migrations.md`.
- `versionKey` names the root-level key of the stored version. Empty, the default, leaves it to
  a `ConfigMigrations<C>` bean or to `_config_version`; a key in both places fails the start.
- `@SurfConfig` also works as a meta-annotation: a custom annotation that carries
  `@SurfConfig("path")` can go on the config class instead.

How it is wired (`impl/handle/SurfConfigContributor.kt`, `impl/handle/SurfConfigFile.kt`,
`manager/SpongeConfigHandle.kt`):

- surf-api finds `@SurfConfig` classes in the plugin's own jar, the same way the component scan
  does, and registers one singleton `ConfigHandle<C>` per class, with the class as its exact type
  argument. The config class itself is not a bean and must not be a Spring component.
- `SurfConfigFile.kt` resolves the path, the format and the migrations; `ConfigHandles.open` opens
  the file.
- The file is read, migrated and written back with its defaults while the plugin's context starts.
  A broken file, a bad path or an unconstructible migration fails the plugin's start, not its first
  use.
- Every plugin context gets this integration, and so do the standalone runtime and microservices.

`ConfigHandle<C>` (`config/ConfigHandle.kt`):

| Member | Effect |
| --- | --- |
| `value` | the current configuration |
| `reload(validate = {})` | read the file into a candidate, migrate it in memory, install it once `validate` returns; writes the file only when a migration ran; a throwing `validate` leaves value and file untouched |
| `edit { }` | change the current value in place as receiver, then save; for classes with `var` properties |
| `update { it.copy(...) }` | replace the value with the result, save, return it; for immutable classes |
| `save()` | write the current value to the file |
| `close()` | reject every further call except `value`; `@SurfConfig` handles close with their context |

`reload`, `edit`, `update`, `save` and `close` run one at a time. `value` is not locked: it returns whatever
the last load, `reload` or `update` installed, and an `edit` in progress is visible to readers.
Read `config.value` where you use it instead of caching the data class; that is why beans inject the
handle and not `C`.

## Reading a config outside a bean

Code that is not a bean reads a `@SurfConfig` class statically (`config/ConfigAccess.kt`):

```kotlin
object ShopPrices {
    private val config by surfConfig<ShopConfig>()

    fun price(base: Double) = base * (1 - config.discount)
}

val handle = configHandle<ShopConfig>()
```

- `by surfConfig<C>()` returns the handle's current value on every read; `configHandle<C>()`
  returns the handle.
- The owning context is found through `C`'s class loader: `SurfConfigContributor` registers each
  plugin context in `SurfConfigContexts` (`@InternalSurfApi`) before its refresh and removes it on
  close. Among the active contexts whose loader sees `C`, the one holding the handle bean the
  contributor registered for `C` wins; other `ConfigHandle<C>` beans are ignored. Two such
  contexts fail the lookup with "has a config handle in more than one context".
- It works from the plugin's bean constructors on: during the refresh, the handle is created on
  demand.
- Before the context exists and after it closed: `IllegalStateException` "No running context holds
  the config handle of …; its plugin's context has not started yet or is closed". A class without
  `@SurfConfig`: "… is not annotated with @SurfConfig".
- The delegate caches the handle with its context and resolves it again once that context is
  inactive, so a restarted plugin reads its new handle.

## Opening any file: `ConfigHandles`

For files whose path is known only at runtime, files read before a plugin's context exists (a
Spring `Condition` evaluated during the component scan) and libraries that are not beans (`infra/nats/.../NatsConfig.kt`,
`infra/redis/.../RedisConfig.kt`, `DatabaseConfig.load`):

```kotlin
ConfigHandles.open<ReportConfig>(folder.resolve("reports.yml")).use { handle ->
    render(handle.value)
}

ConfigHandles.open(
    path,
    format = ConfigFormat.JSON,
    migrations = configMigrations<ReportConfig> { version(1, RenameTitle) },
)
```

- Without a `format`, `.json` opens as JSON and any other extension as YAML; an explicit
  `ConfigFormat` overrides the extension.
- The folder is created; the file is read, migrated and written back with its defaults right away.
- The caller owns the handle and closes it. Nothing tracks it by class; there is no global registry.
- The serializers come from `SpongeConfigSerializers.instance`, so surf-api's root context must be
  running; on Paper that rules out the bootstrap phase. A plugin's `ConfigSerializerRegistrar`s have run only once its context started.

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

With `@SurfConfig`, list them in `migrations`; the first entry upgrades to version 1. For explicit,
sparse versions and lambdas, declare a `ConfigMigrations<C>` bean instead, or pass one to
`ConfigHandles.open`:

```kotlin
@Bean
fun libraryMigrations() = configMigrations<LibraryConfig> {
    version(1, RenameServerVersionMigration)
    version(4) { node -> node.node("deprecated-field").raw(null) }
}
```

They run when the file opens and on every reload, lowest version first, and the reached version is
stored under `_config_version` unless `@SurfConfig(versionKey = …)` or the builder's
`versionKey(…)` names another. A file that has no version field counts as `-1`, so **every**
migration runs the first time such a file is loaded. Details: `references/migrations.md`.

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

- **Registrars run with the context.** `ConfigSerializerRegistrar` beans run once while the plugin's
  context starts, before its first handle opens. A file opened with `ConfigHandles.open` before
  that point does not see their serializers.
- **Register serializers first.** `buildSerializersModule()` is consumed when a file is opened. A
  serializer registered afterwards is invisible to that handle.
- **Reload swaps the instance.** `ConfigHandle.reload()` and `update { }` install a new `C`;
  anything that cached the old one keeps stale data. Read `handle.value` or a `surfConfig`
  delegate at the point of use.
- **`ConfigHandle.edit` always saves.**
- **Static access needs a running context.** `configHandle<C>()` and `surfConfig<C>()` fail before
  the plugin's context starts and after it closes. A Spring `Condition` runs during the component
  scan, before that: open the file with `ConfigHandles.open` there.
- **Migrations in one place.** `@SurfConfig(migrations = …)` and a `ConfigMigrations<C>` bean
  together fail the start. Name the `@Configuration` class differently from its `@Bean` method.
- **Path failures name the class.** `SurfConfigFile` rejects a path with
  "`@SurfConfig path '…' of … must be relative`", "`… leaves the data folder …`" or
  "`… must end in .yml, .yaml or .json`"; a migration that cannot be created fails with
  "`Migration … of … must be an object or have a constructor without parameters`". A plugin without
  a data folder fails with "`… declares @SurfConfig classes …, but has no data folder`".
- **Dashed keys.** Migrations and raw `node.node(...)` paths use `max-players`, never `maxPlayers`.
- **`_config_version`.** The default version key is `ConfigMigrations.DEFAULT_VERSION_KEY`,
  `"_config_version"`, a single root-level key.
- **Maps swallow bad entries.** The default `MapSerializer` logs and skips an invalid entry instead
  of failing the load. Annotate the type `@ThrowExceptions` when you want the failure.
- **Defaults are written on open, not on reload.** Opening re-serializes and saves, so unknown keys
  you hand-wrote stay, but every default appears in the file (`shouldCopyDefaults(true)`). A
  reload without a migration leaves the file untouched. A reload of an emptied file keeps the
  current value and logs a warning; it never falls back to the defaults.
- **Configurate is relocated.** surf-api's jars load Configurate from
  `dev.slne.surf.api.libs.configurate`, and the surf-api Gradle plugins relocate a plugin's
  references the same way. A config class whose annotations still name
  `org.spongepowered.configurate` fails to open with a `LoadConfigException` listing them; build
  the plugin with the Gradle plugin.
- **JSON has no comments.** `@Comment` is silently lost in a JSON file.
- **Save failures are unchecked.** `save()`, `reload()`, `edit` and `update` wrap
  `ConfigurateException` in `UncheckedIOException`; opening throws `LoadConfigException` or
  `SerializationConfigException`. None of them are declared in Kotlin signatures.
- **No runtime, no config.** Opening a file reads `SpongeConfigSerializers` from surf-api's root
  context. Without a running platform runtime that fails with "surf-api's root context has not been
  started yet …".

Build setup for a module that uses configs: `surf-api-build`. Injecting the handle and the beans
every plugin context provides: `surf-api-components`.
