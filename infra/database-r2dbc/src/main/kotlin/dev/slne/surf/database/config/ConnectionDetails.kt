package dev.slne.surf.database.config

import dev.slne.surf.api.core.config.type.number.IntOr
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
internal data class ConnectionDetails(
    val databaseType: DatabaseType = DatabaseType.MARIADB,
    @param:Comment("The schema to use for the database connection. Only relevant for PostgreSQL.")
    val schema: String = "public",
    val host: String = "localhost",
    @param:Comment("The port of the database server. Defaults to 3306 for MariaDB and 5432 for PostgreSQL.")
    val port: IntOr.Default = IntOr.Default.USE_DEFAULT,
    val database: String = "database",
    val username: String = "root",
    val password: String = "1234"
) {
    val resolvedPort: Int = port or databaseType.defaultPort

    override fun toString(): String {
        return "ConnectionDetails(databaseType=$databaseType, schema=$schema, host=$host, port=$resolvedPort, " +
                "database=$database, username=$username, password=<redacted>)"
    }
}
