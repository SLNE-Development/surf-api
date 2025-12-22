package de.slne.redis.config

import dev.slne.surf.surfapi.core.api.config.createSpongeYmlConfig
import dev.slne.surf.surfapi.core.api.config.surfConfigApi
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.nio.file.Path

@ConfigSerializable
internal data class LocalConfig(
    val useGlobalConfig: Boolean = true,
    val local: Local = Local()
) {

    @ConfigSerializable
    data class Local(
        val host: String = "localhost",
        val port: Int = 6379
    )

    fun toInternal() = InternalConfig(
        host = local.host,
        port = local.port
    )

    companion object {
        fun create(pluginDataPath: Path): LocalConfig {
            return surfConfigApi.createSpongeYmlConfig<LocalConfig>(pluginDataPath, "redis.yml")
        }
    }
}