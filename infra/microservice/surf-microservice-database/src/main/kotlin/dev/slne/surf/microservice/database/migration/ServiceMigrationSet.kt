package dev.slne.surf.microservice.database.migration

import dev.slne.surf.database.migration.MigrationSet
import dev.slne.surf.database.migration.MigrationSpec
import org.springframework.core.Ordered

/**
 * The migrations of the microservice itself, described by [migrations].
 *
 * SQL files and migration classes are resolved through [applicationClassLoader] instead of the class loader of this
 * library, because they belong to the application. Only the classes in [ConventionalMigrations.classNames] are loaded,
 * never the rest of the application's packages. The history lives in `flyway_schema_history_<name>`.
 *
 * Runs at [order], before the sets of libraries.
 */
internal class ServiceMigrationSet(
    override val name: String,
    val migrations: ConventionalMigrations,
    private val applicationClassLoader: ClassLoader,
    private val order: Int,
) : MigrationSet, Ordered {

    override fun getOrder(): Int = order

    override fun MigrationSpec.describe() {
        classLoader(applicationClassLoader)
        migrations.sqlLocation?.let { location -> sql(location) }
        if (migrations.classNames.isNotEmpty()) classNames(*migrations.classNames.toTypedArray())
    }
}
