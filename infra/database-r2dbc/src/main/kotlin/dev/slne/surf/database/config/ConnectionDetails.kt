package dev.slne.surf.database.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class ConnectionDetails(
    val databaseType: DatabaseType = DatabaseType.MARIADB,
    val host: String = "localhost",
    val port: Int = 3306,
    val database: String = "database",
    val username: String = "root",
    val password: String = "1234"
)