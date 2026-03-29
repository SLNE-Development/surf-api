package dev.slne.surf.microservice.runtime.microservice.spark

import me.lucko.spark.common.platform.PlatformInfo

object MicroserviceSparkPlatform : PlatformInfo {
    override fun getType(): PlatformInfo.Type {
        return PlatformInfo.Type.APPLICATION
    }

    override fun getName(): String {
        return "Surf Microservice"
    }

    override fun getBrand(): String {
        return "Microservice"
    }

    override fun getVersion(): String {
        return "1.0.0"
    }

    override fun getMinecraftVersion(): String {
        return "1.21.11"
    }
}