# Config migrations

Source: `common/config/api/src/main/kotlin/dev/slne/surf/api/config/migration/` and
`.../config/manager/SpongeConfigManager.kt`.

## The pieces

- `ConfigMigration` - a `fun interface` with `fun migrate(node: ConfigurationNode)`. The node is the
  **root** of the file; do whatever you like to the tree. A lambda works too:
  `ConfigMigration { node -> … }`.
- `ConfigMigrations<C>` - the immutable set of migrations for config class `C`, one per explicit
  version, plus an optional version key. Built with `configMigrations<C> { }` or
  `ConfigMigrations.builder<C>()`; `ConfigMigrations.none()` is the empty set. It turns the
  migrations into a Configurate `ConfigurationTransformation.Versioned` internally.

The version a migration is registered under is the version it upgrades **to**, and it runs when the
file's stored version is **less than** that number. Versions may leave gaps. A negative version or
a version registered twice is rejected with `IllegalArgumentException`.

## Registering

### In `@SurfConfig`

List the migrations in the annotation. Their versions are their positions, starting at 1:

```kotlin
@ConfigSerializable
@SurfConfig(
    "config.yml",
    migrations = [RenameServerVersionMigration::class, RemoveDeprecatedFieldMigration::class],
)
data class MyPluginConfig(
    val release: String = "1.0.0",
    val maxPlayers: Int = 100,
)
```

`RenameServerVersionMigration` upgrades to version 1, `RemoveDeprecatedFieldMigration` to version 2.
Each entry must be a Kotlin `object` or a class with a constructor without parameters; anything else
fails the plugin's start with "Migration … of … must be an object or have a constructor without
parameters". Append new migrations at the end: removing or reordering one changes the version of
every migration after it, and deployed files already store those numbers.

### As a `ConfigMigrations<C>` bean

For explicit versions and lambdas, declare the migrations as a bean of the plugin. surf-api picks
the bean whose type argument is the config class:

```kotlin
@Configuration
class MyPluginConfigBeans {

    @Bean
    fun myPluginConfigMigrations(): ConfigMigrations<MyPluginConfig> {
        return configMigrations {
            version(1, RenameServerVersionMigration)
            version(2, RemoveDeprecatedFieldMigration)
            version(5) { node ->
                val maxPlayers = node.node("max-players")
                if (!maxPlayers.virtual() && maxPlayers.getInt(0) == 0) {
                    maxPlayers.set(100)
                }
            }
        }
    }
}
```

- Declaring migrations in the annotation **and** in a bean fails the start ("declares migrations in
  @SurfConfig and in a ConfigMigrations bean"); two beans for one class fail as well.
- Do not give the `@Configuration` class the same bean name as the `@Bean` method
  (`class MyPluginConfigMigrations` with `fun myPluginConfigMigrations()`): Spring rejects that
  with "factory-bean reference points back to the same bean definition".

### With `ConfigHandles.open`

Pass the same object: `ConfigHandles.open(path, migrations = configMigrations<C> { … })`.

## When they run

- When the file is opened, **before** the node is deserialized into the config class. The file is
  then written back with the reached version and the class's defaults.
- On every `ConfigHandle.reload()`, on the candidate in memory. The file is written only when a
  migration ran and `validate` accepted the candidate; otherwise it stays byte for byte as it was.

Nothing applies migrations on `save()`.

## The version field

Stored at `ConfigMigrations.DEFAULT_VERSION_KEY` = **`_config_version`**, a single key at the root,
unless one of these names another:

- `@SurfConfig(versionKey = "schema")` - a root-level key; applies to the annotation's migrations
  and to a bean that sets no key of its own.
- `versionKey("meta", "version")` in the builder - any path.

Declaring the key both in `@SurfConfig` and in the bean's builder fails the plugin's start with
"declares a version key in @SurfConfig and in a ConfigMigrations bean".

A file with no version field is version `-1` (Configurate's `VERSION_UNKNOWN`), so **every**
registered migration runs the first time an existing, pre-versioning file is loaded. Write
migrations that tolerate the data already being in its new shape - the examples above all check
`virtual()` first, which is why.

## Failure behaviour

When the file is opened, a `ConfigurateException` from a migration becomes `LoadConfigException`;
on a reload it becomes `UncheckedIOException` and the current value stays. Either way a broken
migration is a hard failure, not a warning; for a `@SurfConfig` class it fails the plugin's start.

A successful run logs `Migrated config from version %d to %d`.

## Checklist for writing one

1. Use the **dashed** key names that are actually in the file (`max-players`, not `maxPlayers`),
   unless the field carries `@Setting`.
2. Guard with `!node.node(...).virtual()` so a rerun on already-migrated data is a no-op.
3. Remove a key with `.raw(null)`, do not set it to an empty string.
4. Register under a new, higher version, and never renumber an existing migration - deployed files
   already store that number.
5. Keep the migration stateless; it may be invoked on open and on every reload.
