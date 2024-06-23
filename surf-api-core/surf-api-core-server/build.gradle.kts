import dev.slne.surf.surfapi.PluginType

plugins {
    id("dev.slne.java-library-conventions")
    id("dev.slne.java-shadow-conventions")
    id("dev.slne.surf.surfapi") version "1.0.1"
}

surfApi {
    pluginType.set(PluginType.CORE_API)
    internal.set(true)
}

dependencies {
    api(project(":surf-api-core:surf-api-core-api"))
}

description = "surf-api-core-server"
