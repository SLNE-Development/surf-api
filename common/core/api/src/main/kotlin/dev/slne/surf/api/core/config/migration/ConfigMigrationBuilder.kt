package dev.slne.surf.api.core.config.migration

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.api.core.util.mutableObjectListOf
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap
import it.unimi.dsi.fastutil.objects.ObjectList
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.NodePath
import org.spongepowered.configurate.transformation.ConfigurationTransformation

/**
 * Collects [ConfigMigration] instances keyed by target version and builds
 * a Configurate [ConfigurationTransformation.Versioned] from them.
 *
 * The version key defaults to `"_config_version"` at the root of the config file.
 * This can be customized via [versionKey].
 *
 * ## Backwards Compatibility
 *
 * Existing configs that were created **before** versioning was introduced will
 * have no version field. Configurate treats missing version fields as version `-1`
 * (`VERSION_UNKNOWN`), which means **all** registered migrations will be applied
 * in order the first time such a config is loaded. The version field is then
 * written automatically after migration.
 *
 * ## Usage
 *
 * ```kotlin
 * val builder = ConfigMigrationBuilder()
 * builder.migration(1, MyFirstMigration)
 * builder.migration(2, MySecondMigration)
 *
 * // Apply to a loaded node:
 * builder.migrate(node) // applies pending migrations & updates version field
 * ```
 */
class ConfigMigrationBuilder {

    private val migrations = Int2ObjectRBTreeMap<ObjectList<ConfigMigration>>()
    private var versionKeyPath: Array<out Any> = arrayOf(DEFAULT_VERSION_KEY)

    /**
     * Registers a [migration] for the given target [version].
     *
     * When applied, this migration will run if the config's current version is
     * **less than** [version]. Multiple migrations can be registered for the same
     * version; they will run in registration order.
     *
     * @param version the target version this migration upgrades **to** (must be >= 0)
     * @param migration the migration to apply
     * @return this builder for chaining
     * @throws IllegalArgumentException if [version] is negative
     */
    fun migration(version: Int, migration: ConfigMigration): ConfigMigrationBuilder {
        require(version >= 0) { "Migration version must be >= 0, was $version" }
        migrations.computeIfAbsent(version) { mutableObjectListOf() }.add(migration)
        return this
    }

    /**
     * Registers an inline migration for the given target [version].
     *
     * Convenience overload that accepts a lambda instead of a [ConfigMigration] instance.
     *
     * ```kotlin
     * migration(1) { node ->
     *     node.node("old-key").raw(null)
     * }
     * ```
     *
     * @param version the target version this migration upgrades **to** (must be >= 0)
     * @param migration the migration lambda to apply
     * @return this builder for chaining
     */
    inline fun migration(
        version: Int,
        crossinline migration: (ConfigurationNode) -> Unit
    ): ConfigMigrationBuilder {
        return migration(version, ConfigMigration { node -> migration(node) })
    }

    /**
     * Sets the path in the config file where the version number is stored.
     *
     * Defaults to `"config-version"` (a single root-level key).
     *
     * @param path the path components to the version key
     * @return this builder for chaining
     */
    fun versionKey(vararg path: Any): ConfigMigrationBuilder {
        versionKeyPath = path
        return this
    }

    /**
     * Returns `true` if at least one migration has been registered.
     */
    fun hasMigrations(): Boolean = migrations.isNotEmpty()

    /**
     * Returns the highest registered version, or `-1` if no migrations are registered.
     */
    fun latestVersion(): Int = if (migrations.isEmpty()) -1 else migrations.lastIntKey()

    /**
     * Builds a Configurate [ConfigurationTransformation.Versioned] from the
     * registered migrations.
     *
     * @return the versioned transformation, or `null` if no migrations are registered
     */
    fun buildTransformation(): ConfigurationTransformation.Versioned? {
        if (migrations.isEmpty()) return null

        val builder = ConfigurationTransformation.versionedBuilder()
            .versionKey(*versionKeyPath)

        migrations.int2ObjectEntrySet().iterator().forEachRemaining { entry ->
            val version = entry.intKey
            val migrationList = entry.value

            val transformation = ConfigurationTransformation.builder().apply {
                addAction(NodePath.path()) { _, node ->
                    for (migration in migrationList) {
                        migration.migrate(node)
                    }
                    null
                }
            }.build()

            builder.addVersion(version, transformation)
        }

        return builder.build()
    }

    /**
     * Applies all pending migrations to the given [node].
     *
     * This will:
     * 1. Read the current version from the node (defaulting to `-1` if absent)
     * 2. Apply all migrations with a version greater than the current version, in order
     * 3. Update the version field in the node to the latest version
     *
     * @param node the root configuration node to migrate
     * @return the version the node was migrated from, or `-1` if unversioned
     * @throws ConfigurateException if a migration fails
     */
    @Throws(ConfigurateException::class)
    fun migrate(node: ConfigurationNode): MigrationResult {
        val transformation = buildTransformation()
            ?: return MigrationResult(fromVersion = -1, toVersion = -1, migrated = false)

        val startVersion = transformation.version(node)
        transformation.apply(node)
        val endVersion = transformation.version(node)

        val migrated = startVersion != endVersion

        if (migrated) {
            log.atInfo()
                .log("Migrated config from version %d to %d", startVersion, endVersion)
        }

        return MigrationResult(
            fromVersion = startVersion,
            toVersion = endVersion,
            migrated = migrated
        )
    }

    companion object {
        private val log = logger()

        /**
         * Default key used to store the config schema version.
         */
        const val DEFAULT_VERSION_KEY = "_config_version"
    }
}

/**
 * Result of a migration run.
 *
 * @property fromVersion the version before migration (`-1` if unversioned)
 * @property toVersion the version after migration
 * @property migrated `true` if any migrations were actually applied
 */
data class MigrationResult(
    val fromVersion: Int,
    val toVersion: Int,
    val migrated: Boolean
)