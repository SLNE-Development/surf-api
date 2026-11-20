package dev.slne.surf.database.config

import org.jetbrains.exposed.v1.core.vendors.MariaDBDialect
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.spongepowered.configurate.objectmapping.ConfigSerializable

/**
 * Database vendor supported by surf-database-r2dbc.
 */
@ConfigSerializable
enum class DatabaseType(val defaultPort: Int) {
    MARIADB(3306),
    POSTGRESQL(5432);

    /**
     * Lowercase vendor name, for example `mariadb`, that replaces
     * [MigrationSpec.VENDOR_PLACEHOLDER][dev.slne.surf.database.migration.MigrationSpec.VENDOR_PLACEHOLDER] in SQL
     * locations.
     */
    val vendor: String = name.lowercase()
}

/**
 * The vendor of this database, derived from its dialect.
 *
 * @throws IllegalStateException if the dialect is neither MariaDB nor PostgreSQL
 */
val R2dbcDatabase.databaseType: DatabaseType
    get() = when (dialect) {
        is MariaDBDialect -> DatabaseType.MARIADB
        is PostgreSQLDialect -> DatabaseType.POSTGRESQL
        else -> error(
            "Dialect ${dialect.javaClass.simpleName} has no DatabaseType, " +
                    "expected one of ${DatabaseType.entries.joinToString()}"
        )
    }
