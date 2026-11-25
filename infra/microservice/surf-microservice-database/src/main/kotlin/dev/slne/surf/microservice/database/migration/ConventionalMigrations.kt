package dev.slne.surf.microservice.database.migration

import dev.slne.surf.database.config.DatabaseType
import dev.slne.surf.database.migration.MigrationSpec
import dev.slne.surf.database.migration.SurfMigration
import dev.slne.surf.database.migration.internal.MigrationClassFinder

/**
 * Migrations a microservice ships by convention.
 *
 * @property sqlLocation [VENDOR_SQL_LOCATION] if the class loader has a vendor folder below
 * [SQL_LOCATION], otherwise [SQL_LOCATION] if the class loader has it, otherwise `null`
 * @property classNames binary names of the [SurfMigration] classes that [MigrationClassFinder]
 * finds in the application packages, sorted. Exactly these classes are loaded; a class that is no
 * `object`, no companion object and has no public no-argument constructor fails the migration
 */
internal data class ConventionalMigrations(
    val sqlLocation: String?,
    val classNames: List<String>,
) {

    companion object {
        /**
         * Classpath location of the SQL migrations, Flyway's default.
         */
        const val SQL_LOCATION = "db/migration"

        /**
         * Classpath location of vendor-specific SQL migrations, such as `db/migration/postgresql`.
         *
         * [MigrationSpec.sql] resolves the placeholder to the [vendor][DatabaseType.vendor] of the
         * database.
         */
        const val VENDOR_SQL_LOCATION = "$SQL_LOCATION/${MigrationSpec.VENDOR_PLACEHOLDER}"

        /**
         * Returns the migrations to run against a [databaseType] database, or `null` if the
         * application ships none.
         *
         * The SQL files are looked up through [classLoader]. Code migrations are the
         * [SurfMigration] classes in [applicationPackages] that
         * [MigrationClassFinder.findClassNames] finds.
         *
         * Once the class loader has a folder for any vendor below [SQL_LOCATION], only the folder
         * of [databaseType] is used, and SQL files directly in [SQL_LOCATION] are then ignored.
         *
         * @throws IllegalStateException if vendor folders exist but none for [databaseType]
         */
        fun discover(
            classLoader: ClassLoader,
            databaseType: DatabaseType,
            applicationPackages: List<String>,
        ): ConventionalMigrations? {
            val conventions = ConventionalMigrations(
                sqlLocation = findSqlLocation(classLoader, databaseType),
                classNames = MigrationClassFinder.findClassNames(applicationPackages, classLoader),
            )

            return conventions.takeUnless { it.sqlLocation == null && it.classNames.isEmpty() }
        }

        private fun findSqlLocation(classLoader: ClassLoader, databaseType: DatabaseType): String? {
            val vendorsWithFolder = DatabaseType.entries.filter {
                classLoader.hasResource(
                    vendorLocation(it),
                )
            }

            return when {
                vendorsWithFolder.isEmpty() -> SQL_LOCATION.takeIf { classLoader.hasResource(it) }
                databaseType in vendorsWithFolder -> VENDOR_SQL_LOCATION
                else -> error(missingVendorFolderMessage(vendorsWithFolder, databaseType))
            }
        }

        private fun missingVendorFolderMessage(
            vendorsWithFolder: List<DatabaseType>,
            databaseType: DatabaseType,
        ): String {
            return "SQL migrations exist for ${vendorsWithFolder.joinToString { vendorLocation(it) }}, " +
                "but not for the configured ${databaseType.vendor} database; add ${
                    vendorLocation(
                        databaseType,
                    )
                }"
        }

        private fun vendorLocation(databaseType: DatabaseType): String {
            return "$SQL_LOCATION/${databaseType.vendor}"
        }

        private fun ClassLoader.hasResource(name: String) = getResource(name) != null
    }
}
