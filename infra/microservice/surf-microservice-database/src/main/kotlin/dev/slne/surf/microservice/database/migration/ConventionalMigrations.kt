package dev.slne.surf.microservice.database.migration

import dev.slne.surf.database.migration.SurfMigration
import dev.slne.surf.microservice.database.DatabaseCustomizer

/**
 * Migrations a microservice ships by convention.
 *
 * @property sqlLocation [SQL_LOCATION] if the class loader has it, otherwise `null`
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
         * Returns the migrations to run at start, or `null` if migrations are skipped.
         *
         * [customizer] decides through [DatabaseCustomizer.runMigrations]; without one, migrations run only if a
         * convention found something. Nothing is loaded when [DatabaseCustomizer.runMigrations] is `false`.
         *
         * @throws IllegalStateException if a listed migration class cannot be instantiated
         */
        fun select(customizer: DatabaseCustomizer?, classLoader: ClassLoader): ConventionalMigrations? {
            if (customizer?.runMigrations == false) return null

            val conventions = discover(classLoader)
            return conventions.takeUnless { customizer == null && conventions.isEmpty }
        }

        private fun discover(classLoader: ClassLoader) = ConventionalMigrations(
            sqlLocation = SQL_LOCATION.takeIf { classLoader.getResource(it) != null },
            classes = loadListedMigrations(classLoader),
        )
    }
}
