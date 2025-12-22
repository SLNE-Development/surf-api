package de.slne.redis.config

import java.nio.file.Path

internal data class InternalConfig(
    val host: String = "localhost",
    val port: Int = 6379
) {

    companion object {
        fun load(pluginDataPath: Path, pluginsPath: Path): InternalConfig {
            val localConfig = LocalConfig.create(pluginDataPath)

            if (!localConfig.useGlobalConfig) {
                return localConfig.toInternal()
            }

            val globalConfig = GlobalConfig.createOrLoad(pluginsPath)

            return globalConfig.toInternal()
        }
    }
}