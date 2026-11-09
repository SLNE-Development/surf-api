# Config migrations

Source: `common/core/api/src/main/kotlin/dev/slne/surf/api/core/config/migration/` and
`.../config/manager/SpongeConfigManager.kt`.

## The pieces

- `ConfigMigration` - a `fun interface` with `fun migrate(node: ConfigurationNode)`. The node is the
  **root** of the file; do whatever you like to the tree.
- `ConfigMigrationBuilder` - collects migrations in a `TreeMap<Int, MutableList<ConfigMigration>>`
  and turns them into a Configurate `ConfigurationTransformation.Versioned`.
- `MigrationResult(fromVersion, toVersion, migrated)` - what `migrate(node)` returns.

The version a migration is registered under is the version it upgrades **to**, and it runs when the
file's current version is **less than** that number. Several migrations may share a version; they
run in registration order. A negative version is rejected with `IllegalArgumentException`.

## Registering

The normal place is the config companion's `init` block, because `SpongeConfigClass` hands its
`migrationBuilder` to the manager when the manager is created:

```kotlin
@ConfigSerializable
data class MyPluginConfig(
    var release: String = "1.0.0",
    var maxPlayers: Int = 100,
) {
    companion object : SpongeYmlConfigClass<MyPluginConfig>(
        MyPluginConfig::class.java,
        plugin.dataPath,
        "migration-example-config.yml"
    ) {
        init {
            migration(1, RenameServerVersionMigration)
            migration(2, RemoveDeprecatedFieldMigration)
            migration(3) { node ->
                val mp = node.node("max-players")
                if (!mp.virtual() && mp.getInt(0) == 0) {
                    mp.set(100)
                }
            }
        }
    }
}

object RenameServerVersionMigration : ConfigMigration {
    override fun migrate(node: ConfigurationNode) {
        val old = node.node("server", "version")
        if (!old.virtual()) {
            node.node("server", "release").set(old.raw())
            old.raw(null)
        }
    }
}

object RemoveDeprecatedFieldMigration : ConfigMigration {
    override fun migrate(node: ConfigurationNode) {
        node.node("deprecated-field").raw(null)
    }
}
```

Two other entry points:

- Build a `ConfigMigrationBuilder` yourself and pass it to
  `surfConfigApi.createSpongeYmlConfigManager<C>(folder, fileName, migrations)` (the `migrations`
  parameter defaults to an empty `ConfigMigrationBuilder()`).
- `manager.addMigration(version, migration)` / `manager.addMigration(version) { node -> … }` after
  the fact. Both return the manager for chaining, and both take effect **only on the next
  `reloadFromFile()`** - the initial load has already happened. `manager.migrations()` returns the
  builder for inspection.

## When they run

- On the initial load, inside `SpongeConfigManager.buildConfigManager`, **before** the node is
  deserialized into the config class. If anything changed, the node is saved straight away so the
  version field is persisted even if deserialization later fails.
- On every `reloadFromFile()`, again before deserialization, via the private `applyMigrations`.

Nothing applies migrations on `save()`.

## The version field

Stored at `ConfigMigrationBuilder.DEFAULT_VERSION_KEY` = **`_config_version`**, a single key at the
root. Change it with `versionKey(vararg path: Any)` - from the companion's `init`, before
registering migrations:

```kotlin
init {
    versionKey("meta", "version")
    migration(1, …)
}
```

Beware: the KDoc on `versionKey` says the default is `"config-version"`. It is wrong; the constant
is `_config_version`.

A file with no version field is version `-1` (Configurate's `VERSION_UNKNOWN`), so **every**
registered migration runs the first time an existing, pre-versioning file is loaded. Write
migrations that tolerate the data already being in its new shape - the examples above all check
`virtual()` first, which is why.

## Failure behaviour

`migrate` declares `@Throws(ConfigurateException::class)`. On the initial load a thrown
`ConfigurateException` is caught by `buildConfigManager` and rethrown as `LoadConfigException`; on a
reload `applyMigrations` logs it and rethrows, so `reloadFromFile` turns it into
`UncheckedIOException`. Either way the config does not come up - a broken migration is a hard
startup failure, not a warning.

A successful run logs `Migrated config from version %d to %d`.

## Checklist for writing one

1. Use the **dashed** key names that are actually in the file (`max-players`, not `maxPlayers`),
   unless the field carries `@Setting`.
2. Guard with `!node.node(...).virtual()` so a rerun on already-migrated data is a no-op.
3. Remove a key with `.raw(null)`, do not set it to an empty string.
4. Register under the next unused integer, and never renumber an existing migration - deployed files
   already store that number.
5. Keep the migration stateless; it may be invoked on load and on every reload.
