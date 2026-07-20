package dev.slne.surf.database.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
internal data class ConnectionDetails(
    val databaseType: DatabaseType = DatabaseType.MARIADB,
    @param:Comment("The schema to use for the database connection. Only relevant for PostgreSQL.")
    val schema: String = "public",
    val host: String = "localhost",
    val port: Int = 3306,
    val database: String = "database",
    val username: String = "root",
    val password: String = "1234"
) {
    override fun toString(): String {
        return "ConnectionDetails(databaseType=$databaseType, schema=$schema, host=$host, port=$port, " +
                "database=$database, username=$username, password=<redacted>)"
    }
}
