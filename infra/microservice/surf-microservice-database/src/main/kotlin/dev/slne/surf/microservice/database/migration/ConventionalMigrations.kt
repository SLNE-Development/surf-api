package dev.slne.surf.microservice.database.migration

import dev.slne.surf.database.config.DatabaseType
import dev.slne.surf.database.migration.MigrationSpec
import dev.slne.surf.database.migration.SurfMigration
import dev.slne.surf.microservice.database.DatabaseCustomizer

/**
 * Migrations a microservice ships by convention.
 *
 * @property sqlLocation [VENDOR_SQL_LOCATION] if the class loader has a vendor folder below [SQL_LOCATION],
 * otherwise [SQL_LOCATION] if the class loader has it, otherwise `null`
 * @property classes code migrations listed in [MIGRATIONS_RESOURCE]
 */
internal class ConventionalMigrations(
    val sqlLocation: String?,
    val classes: List<SurfMigration>,
) {
    val isEmpty: Boolean
        get() = sqlLocation == null && classes.isEmpty()

    companion object {
        /**
         * Classpath location of the SQL migrations, Flyway's default.
         */
        const val SQL_LOCATION = "db/migration"

        /**
         * Classpath location of vendor-specific SQL migrations, such as `db/migration/postgresql`.
         *
         * [MigrationSpec.sql] resolves the placeholder to the [vendor][DatabaseType.vendor] of the database.
         */
        const val VENDOR_SQL_LOCATION = "$SQL_LOCATION/${MigrationSpec.VENDOR_PLACEHOLDER}"

        /**
         * Returns the migrations to run at start against a [databaseType] database, or `null` if migrations are
         * skipped.
         *
         * [customizer] decides through [DatabaseCustomizer.runMigrations]. Without one, migrations run only if a
         * convention found something. Nothing is loaded when [DatabaseCustomizer.runMigrations] is `false`.
         *
         * Once the class loader has a folder for any vendor below [SQL_LOCATION], only the folder of [databaseType]
         * is used, and SQL files directly in [SQL_LOCATION] are then ignored.
         *
         * @throws IllegalStateException if vendor folders exist but none for [databaseType], or a listed migration
         * class cannot be instantiated
         */
        fun select(
            customizer: DatabaseCustomizer?,
            classLoader: ClassLoader,
            databaseType: DatabaseType,
        ): ConventionalMigrations? {
            if (customizer?.runMigrations == false) return null

            val conventions = discover(classLoader, databaseType)
            return conventions.takeUnless { customizer == null && conventions.isEmpty }
        }

        private fun discover(classLoader: ClassLoader, databaseType: DatabaseType) = ConventionalMigrations(
            sqlLocation = findSqlLocation(classLoader, databaseType),
            classes = loadListedMigrations(classLoader),
        )

        private fun findSqlLocation(classLoader: ClassLoader, databaseType: DatabaseType): String? {
            val vendorsWithFolder = DatabaseType.entries.filter { classLoader.hasResource(vendorLocation(it)) }

            return when {
                vendorsWithFolder.isEmpty() -> SQL_LOCATION.takeIf { classLoader.hasResource(it) }
                databaseType in vendorsWithFolder -> VENDOR_SQL_LOCATION
                else -> error(missingVendorFolderMessage(vendorsWithFolder, databaseType))
            }
        }

        private fun missingVendorFolderMessage(vendorsWithFolder: List<DatabaseType>, databaseType: DatabaseType) =
            "SQL migrations exist for ${vendorsWithFolder.joinToString { vendorLocation(it) }}, " +
                    "but not for the configured ${databaseType.vendor} database; add ${vendorLocation(databaseType)}"

        private fun vendorLocation(databaseType: DatabaseType) = "$SQL_LOCATION/${databaseType.vendor}"

        private fun ClassLoader.hasResource(name: String) = getResource(name) != null
    }
}
