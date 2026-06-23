package dev.slne.surf.database.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
enum class DatabaseType {
    MARIADB,
    POSTGRESQL
}