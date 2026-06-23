package dev.slne.surf.database.config

import dev.slne.surf.api.core.config.createSpongeYmlConfig
import dev.slne.surf.api.core.config.surfConfigApi
import org.slf4j.event.Level
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.nio.file.Path

@ConfigSerializable
data class DatabaseConfig(
    val logLevel: Level = Level.DEBUG,
    val credentials: ConnectionDetails = ConnectionDetails(),
    val pool: PoolConfig = PoolConfig()
) {
    companion object {
        fun create(path: Path): DatabaseConfig {
            return surfConfigApi.createSpongeYmlConfig<DatabaseConfig>(path, "database.yml")
        }
    }
}