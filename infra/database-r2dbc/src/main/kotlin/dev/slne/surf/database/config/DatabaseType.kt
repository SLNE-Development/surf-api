package dev.slne.surf.database.config

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
