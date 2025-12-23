package dev.slne.redis.config

import dev.slne.surf.surfapi.core.api.config.createSpongeYmlConfig
import dev.slne.surf.surfapi.core.api.config.surfConfigApi
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.nio.file.Path
import kotlin.io.path.div

@ConfigSerializable
internal data class GlobalConfig(
    val host: String = "localhost",
    val port: Int = 6379,
    val password: String? = null
) {

    fun toInternal() = InternalConfig(
        host = host,
        port = port,
        password = password
    )

    companion object {
        fun createOrLoad(pluginsPath: Path): GlobalConfig {
            return surfConfigApi.createSpongeYmlConfig<GlobalConfig>(
                pluginsPath / "surf-redis",
                "global.yml"
            )
        }
    }
}