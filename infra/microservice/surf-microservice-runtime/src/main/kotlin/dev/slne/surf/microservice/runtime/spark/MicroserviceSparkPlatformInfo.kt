package dev.slne.surf.microservice.runtime.spark

import dev.slne.surf.microservice.runtime.MicroserviceRuntimeBuildConfig
import me.lucko.spark.common.platform.PlatformInfo

/**
 * Describes the microservice runtime to spark.
 */
internal object MicroserviceSparkPlatformInfo : PlatformInfo {
    override fun getType(): PlatformInfo.Type = PlatformInfo.Type.APPLICATION

    override fun getName(): String = "Surf Microservice"

    override fun getBrand(): String = "Surf Microservice"

    override fun getVersion(): String = MicroserviceRuntimeBuildConfig.VERSION

    /**
     * Returns `null`: a microservice runs no Minecraft, and spark omits the field then.
     */
    override fun getMinecraftVersion(): String? = null
}
