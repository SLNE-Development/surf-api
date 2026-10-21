package dev.slne.surf.database.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
internal enum class DatabaseType(val defaultPort: Int) {
    MARIADB(3306),
    POSTGRESQL(5432)
}