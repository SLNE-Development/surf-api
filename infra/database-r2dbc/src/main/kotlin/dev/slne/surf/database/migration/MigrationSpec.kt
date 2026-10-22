package dev.slne.surf.database.migration

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet

/**
 * Describes the migrations of one plugin.
 *
 * Every entry is optional, but at least one of [sql], [classes] or [classPackage] must be given. Each may be called
 * more than once; repeated entries are registered once.
 *
 * All migrations must be idempotent (for example `CREATE TABLE IF NOT EXISTS`): the history of a plugin starts at its
 * [baselineVersion], and the target database may already contain objects created before migrations were introduced.
 */
class MigrationSpec internal constructor(private var loader: ClassLoader) {
    private val sqlPaths = ObjectLinkedOpenHashSet<String>()
    private val migrations = ObjectLinkedOpenHashSet<SurfMigration>()
    private val packages = ObjectLinkedOpenHashSet<String>()
    private var baseline = "0"

    /**
     * Adds classpath locations that contain `V<version>__<description>.sql` files, for example `db/migrations`. A
     * `classpath:` prefix is optional.
     */
    fun sql(vararg locations: String) {
        for (location in locations) {
            require(location.isNotBlank()) { "SQL location must not be blank" }
            sqlPaths += location.removePrefix(CLASSPATH_PREFIX).trim('/')
        }
    }

    /**
     * Adds code migrations.
     */
    fun classes(vararg migrations: SurfMigration) {
        this.migrations += migrations
    }

    /**
     * Adds a package that is scanned, including subpackages and named nested classes, for [SurfMigration] objects and
     * subclasses with a public no-argument constructor. `migrate()` fails if the package contains none.
     */
    fun classPackage(packageName: String) {
        require(packageName.isNotBlank()) { "Package name must not be blank" }
        packages += packageName
    }

    /**
     * Sets the version at which the plugin's history is baselined. Defaults to `"0"`, so that a brand-new plugin on a
     * shared, non-empty database still runs its `V1`.
     *
     * On a non-empty database without history, migrations up to and including the baseline are treated as already
     * applied, so every migration above it must be idempotent. On an empty database no baseline is written and every
     * migration runs, including those at or below the baseline.
     *
     * @param version Dot-separated numbers, for example `"0"`, `"1"` or `"2.1"`.
     * @throws IllegalArgumentException if [version] does not match `\d+(\.\d+)*`
     */
    fun baselineVersion(version: String) {
        require(VERSION_REGEX.matches(version)) {
            "Baseline version '$version' must be dot-separated numbers, for example \"1\" or \"2.1\""
        }
        baseline = version
    }

    /**
     * Sets the class loader used to find SQL files and load scanned classes. Defaults to the class loader of the
     * plugin that called `migrate()`.
     */
    fun classLoader(loader: ClassLoader) {
        this.loader = loader
    }

    internal fun build(name: String): MigrationPlan {
        val normalizedName = name.lowercase()
        require(NAME_REGEX.matches(normalizedName)) { "Migration name '$name' must match [a-z0-9_]{1,40}" }
        require(sqlPaths.isNotEmpty() || migrations.isNotEmpty() || packages.isNotEmpty()) {
            "Migration '$name' needs at least one of sql(), classes() or classPackage()"
        }

        return MigrationPlan(
            name = name,
            historyTable = "flyway_schema_history_$normalizedName",
            sqlPaths = sqlPaths.toList(),
            migrations = migrations.toList(),
            packages = packages.toList(),
            baselineVersion = baseline,
            classLoader = loader
        )
    }

    private companion object {
        const val CLASSPATH_PREFIX = "classpath:"
        val NAME_REGEX = Regex("[a-z0-9_]{1,40}")
        val VERSION_REGEX = Regex("""\d+(\.\d+)*""")
    }
}

/**
 * Validated migration settings of one plugin.
 *
 * @property name Migration name as passed to `migrate()`, used in error messages.
 * @property sqlPaths Classpath paths of the SQL locations, without `classpath:` prefix and surrounding slashes.
 */
internal data class MigrationPlan(
    val name: String,
    val historyTable: String,
    val sqlPaths: List<String>,
    val migrations: List<SurfMigration>,
    val packages: List<String>,
    val baselineVersion: String,
    val classLoader: ClassLoader
)
