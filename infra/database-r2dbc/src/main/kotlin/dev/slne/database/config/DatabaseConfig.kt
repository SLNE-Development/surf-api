package dev.slne.database.config

import dev.slne.surf.surfapi.core.api.config.createSpongeYmlConfig
import dev.slne.surf.surfapi.core.api.config.surfConfigApi
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.nio.file.Path

@ConfigSerializable
internal data class DatabaseConfig(
    val credentials: ConnectionDetails = ConnectionDetails(),
    val pool: PoolConfig = PoolConfig()
) {
    companion object {
        fun create(path: Path): DatabaseConfig {
            return surfConfigApi.createSpongeYmlConfig<DatabaseConfig>(path, "database.yml")
        }
    }
}