package dev.slne.database.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
internal data class ConnectionDetails(
    val host: String = "localhost",
    val port: Int = 3306,
    val database: String = "database",
    val username: String = "root",
    val password: String = "1234"
)