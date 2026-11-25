package dev.slne.surf.api.config.migration

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.NodePath
import org.spongepowered.configurate.transformation.ConfigurationTransformation
import java.util.*

/**
 * The versioned migrations of the configuration class [C].
 *
 * Each migration is registered under the version it upgrades a file **to**. Versions are explicit
 * and may leave gaps, so a removed migration never renumbers the ones after it. Opening a file
 * runs every migration whose version is above the version stored in the file, lowest first, and
 * stores the highest version under [versionKey]. A file without a stored version counts as
 * version `-1`, so all migrations run on it.
 *
 * Pass it to [dev.slne.surf.api.config.ConfigHandles.open], or declare it as a bean of a plugin
 * context to migrate the plugin's `@SurfConfig` class [C]:
 *
 * ```kotlin
 * @Bean
 * fun shopMigrations() = configMigrations<ShopConfig> {
 *     version(1, RenameDiscount)
 *     version(3) { node -> node.node("legacy-prices").raw(null) }
 * }
 * ```
 *
 * Instances are immutable and thread-safe.
 *
 * @param C the configuration class the migrations belong to; it only selects the bean
 * @property versionKey the path of the stored version in the file, or `null` if the migrations
 * leave it to the declaration of the file, which defaults to [DEFAULT_VERSION_KEY]
 */
class ConfigMigrations<C : Any> private constructor(
    private val migrations: SortedMap<Int, ConfigMigration>,
    val versionKey: List<String>?,
) {

    /**
     * Returns these migrations with [versionKey] as their version key unless they set one.
     *
     * @param versionKey the path of the stored version in the file
     * @return the migrations with a version key
     */
    @InternalSurfApi
    fun withVersionKeyFallback(versionKey: List<String>): ConfigMigrations<C> {
        if (this.versionKey != null) return this

        return ConfigMigrations(migrations, versionKey)
    }

    /**
     * Applies the pending migrations to [node] and stores the reached version in it.
     *
     * @param node the root node of the file
     * @return `true` if a migration ran
     * @throws ConfigurateException if a migration fails
     */
    internal fun migrate(node: ConfigurationNode): Boolean {
        if (migrations.isEmpty()) return false

        val transformation = buildTransformation()
        val startVersion = transformation.version(node)
        transformation.apply(node)
        val endVersion = transformation.version(node)
        if (startVersion == endVersion) return false

        log.atInfo()
            .log("Migrated config from version %d to %d", startVersion, endVersion)

        return true
    }

    private fun buildTransformation(): ConfigurationTransformation.Versioned {
        val key = (versionKey ?: listOf(DEFAULT_VERSION_KEY)).toTypedArray<Any>()
        val builder = ConfigurationTransformation.versionedBuilder()
            .versionKey(*key)

        for ((version, migration) in migrations) {
            val step = ConfigurationTransformation.builder()
                .addAction(NodePath.path()) { _, node ->
                    migration.migrate(node)
                    null
                }
                .build()
            builder.addVersion(version, step)
        }

        return builder.build()
    }

    /**
     * Collects the migrations of one configuration class.
     *
     * Not thread-safe.
     */
    class Builder<C : Any> internal constructor() {
        private val migrations = TreeMap<Int, ConfigMigration>()
        private var versionKey: List<String>? = null

        /**
         * Registers [migration] as the step that upgrades a file to [version].
         *
         * @param version the version the migration upgrades to
         * @param migration the migration
         * @return this builder
         * @throws IllegalArgumentException if [version] is negative or already has a migration
         */
        fun version(version: Int, migration: ConfigMigration): Builder<C> {
            require(version >= 0) { "Migration version must not be negative, was $version" }
            require(migrations.putIfAbsent(version, migration) == null) {
                "Migration version $version is registered twice"
            }

            return this
        }

        /**
         * Stores the version under [path].
         *
         * A `@SurfConfig` class whose annotation also names a version key fails to start.
         *
         * @param path the keys from the root of the file to the version
         * @return this builder
         * @throws IllegalArgumentException if [path] is empty or contains a blank key
         */
        fun versionKey(vararg path: String): Builder<C> {
            require(path.isNotEmpty()) { "The version key path must not be empty" }
            require(path.none(String::isBlank)) {
                "The version key path must not contain a blank key, was ${path.toList()}"
            }
            versionKey = path.toList()

            return this
        }

        /**
         * Returns the collected migrations.
         *
         * @return the migrations
         */
        fun build(): ConfigMigrations<C> = ConfigMigrations(TreeMap(migrations), versionKey)
    }

    companion object {
        private val log = logger()

        /**
         * The root-level key that stores the version unless a declaration names another.
         */
        const val DEFAULT_VERSION_KEY = "_config_version"

        /**
         * Returns a builder for the migrations of [C].
         *
         * @return a new builder
         */
        fun <C : Any> builder(): Builder<C> = Builder()

        /**
         * Returns migrations that change nothing.
         *
         * @return migrations without a step
         */
        fun <C : Any> none(): ConfigMigrations<C> = ConfigMigrations(TreeMap(), null)
    }
}

/**
 * Builds the migrations of [C] with [block].
 *
 * @param block registers the migrations on the builder
 * @return the migrations
 */
inline fun <C : Any> configMigrations(
    block: ConfigMigrations.Builder<C>.() -> Unit,
): ConfigMigrations<C> {
    return ConfigMigrations.builder<C>()
        .apply(block)
        .build()
}
