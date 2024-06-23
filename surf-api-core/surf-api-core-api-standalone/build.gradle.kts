import dev.slne.surf.surfapi.PluginType

plugins {
    id("dev.slne.java-common-conventions")
    id("dev.slne.java-shadow-conventions")
    id("dev.slne.surf.surfapi") version "1.0.1"
}

surfApi {
    pluginType.set(PluginType.CORE_STANDALONE)
    internal.set(true)
}

description = "surf-api-core-api-standalone"
